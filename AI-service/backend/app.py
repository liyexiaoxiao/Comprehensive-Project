import json
import os
import tempfile
from datetime import datetime, timezone

import requests
from flask import Flask, Response, jsonify, request, send_from_directory
from flask_cors import CORS
from pymongo import MongoClient
from qwen_service import query_qwen_companion, stream_qwen_companion
from tts_service import synthesize_speech

app = Flask(__name__)
CORS(app)

# Emotion recognition service
EMOTION_SERVICE_URL = os.getenv("EMOTION_SERVICE_URL", "http://localhost:5003/predict")

# Generated TTS audio
AUDIO_OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "generated_audio")
os.makedirs(AUDIO_OUTPUT_DIR, exist_ok=True)

# MongoDB config
MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017")
MONGO_DB = os.getenv("MONGO_DB", "comprehensive_project")
MONGO_COLLECTION = os.getenv("MONGO_COLLECTION", "ai_chat_log")

mongo_client = MongoClient(MONGO_URI)
mongo_db = mongo_client[MONGO_DB]
ai_chat_log_collection = mongo_db[MONGO_COLLECTION]


def now_utc():
    return datetime.now(timezone.utc)


def build_tts_payload(reply: str, voice: str = None):
    try:
        audio_filename = synthesize_speech(reply, AUDIO_OUTPUT_DIR, voice)
        return {
            "audio_url": f"/api/audio/{audio_filename}",
            "tts_error": None,
        }
    except Exception as e:
        print(f"TTS generation failed: {e}")
        return {
            "audio_url": None,
            "tts_error": str(e),
        }


def parse_history_items(logs):
    """Convert stored DB messages into the format expected by the LLM."""
    history = []
    for log in logs:
        role = log.get("role")
        content = log.get("content")
        if role in ("user", "assistant") and content:
            history.append({"role": role, "content": content})
    return history


def fetch_history(session_id: str, limit: int = 20):
    cursor = (
        ai_chat_log_collection
        .find({"sessionId": session_id})
        .sort("createdAt", 1)
    )
    logs = list(cursor)
    if limit > 0 and len(logs) > limit:
        logs = logs[-limit:]
    return logs


def save_chat_log(user_id: int, session_id: str, role: str, content: str, emotion_label: str = None):
    doc = {
        "userId": user_id,
        "sessionId": session_id,
        "role": role,
        "content": content,
        "emotionLabel": emotion_label,
        "createdAt": now_utc(),
    }
    ai_chat_log_collection.insert_one(doc)


@app.route('/api/audio/<path:filename>', methods=['GET'])
def generated_audio(filename):
    return send_from_directory(AUDIO_OUTPUT_DIR, filename, mimetype='audio/mpeg')


@app.route('/api/companion/chat', methods=['POST', 'OPTIONS'])
def companion_chat():
    """Support text/audio chat, emotion recognition, TTS, and MongoDB history."""
    if request.method == 'OPTIONS':
        return '', 200

    try:
        payload = request.get_json(silent=True) if request.is_json else {}
        payload = payload or {}

        user_id_raw = payload.get('userId') or request.form.get('userId')
        session_id = (payload.get('sessionId') or request.form.get('sessionId') or '').strip()
        tts_voice = (payload.get('tts_voice') or request.form.get('tts_voice') or '').strip()

        if user_id_raw is None:
            return jsonify({'error': 'userId is required'}), 400
        if not session_id:
            return jsonify({'error': 'sessionId is required'}), 400

        try:
            user_id = int(user_id_raw)
        except (TypeError, ValueError):
            return jsonify({'error': 'userId must be integer'}), 400

        detected_emotion = None
        emotion_data = {}
        transcript = ''

        if 'audio' in request.files:
            audio_file = request.files['audio']
            if audio_file.filename == '':
                return jsonify({'error': 'No audio file selected'}), 400

            temp_path = None
            try:
                file_suffix = os.path.splitext(audio_file.filename)[1] or '.wav'
                with tempfile.NamedTemporaryFile(suffix=file_suffix, delete=False) as temp_file:
                    temp_path = temp_file.name
                    audio_file.save(temp_path)

                with open(temp_path, 'rb') as f:
                    audio_content = f.read()

                frontend_transcript = (request.form.get('transcript') or '').strip()
                upload_name = audio_file.filename or f"recording{file_suffix}"
                upload_mime = audio_file.mimetype or 'application/octet-stream'
                files = {'audio': (upload_name, audio_content, upload_mime)}
                data = {'transcript': frontend_transcript} if frontend_transcript else {}

                emotion_response = requests.post(EMOTION_SERVICE_URL, files=files, data=data, timeout=60)
                if emotion_response.status_code != 200:
                    return jsonify({'error': f'Emotion recognition service failed: {emotion_response.text}'}), 500

                emotion_data = emotion_response.json()
                detected_emotion = emotion_data.get('complex_emotion', emotion_data.get('basic_emotion', 'neutral'))
                transcript = (emotion_data.get('transcript') or frontend_transcript).strip()

                if not transcript:
                    return jsonify({'error': 'No transcript from audio'}), 400

            finally:
                if temp_path and os.path.exists(temp_path):
                    os.unlink(temp_path)
        else:
            transcript = (payload.get('text') or '').strip()
            detected_emotion = (payload.get('detected_emotion') or '').strip() or None
            if not transcript:
                return jsonify({'error': 'No text or audio provided'}), 400

        logs = fetch_history(session_id, limit=20)
        history = parse_history_items(logs)

        save_chat_log(
            user_id=user_id,
            session_id=session_id,
            role='user',
            content=transcript,
            emotion_label=detected_emotion,
        )

        model_raw = query_qwen_companion(transcript, detected_emotion, history)

        emotion = '平静'
        reply = model_raw
        try:
            parsed = json.loads(model_raw)
            emotion = parsed.get('emotion', emotion)
            reply = parsed.get('reply', reply)
        except Exception:
            pass

        save_chat_log(
            user_id=user_id,
            session_id=session_id,
            role='assistant',
            content=reply,
            emotion_label=emotion,
        )

        tts_payload = build_tts_payload(reply, tts_voice)

        return jsonify({
            'userId': user_id,
            'sessionId': session_id,
            'emotion': emotion,
            'reply': reply,
            'raw': model_raw,
            'detected_emotion': detected_emotion,
            'transcript': transcript,
            'emotion_details': emotion_data,
            **tts_payload,
        })

    except requests.exceptions.RequestException as e:
        return jsonify({'error': f'Failed to call emotion service: {str(e)}'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/companion/chat/stream', methods=['POST', 'OPTIONS'])
def companion_chat_stream():
    """SSE: 流式返回 AI 回复文本，并在结束后落库与可选 TTS。"""
    if request.method == 'OPTIONS':
        return '', 200

    try:
        payload = request.get_json(silent=True) or {}
        user_id_raw = payload.get('userId')
        session_id = (payload.get('sessionId') or '').strip()
        transcript = (payload.get('text') or '').strip()
        detected_emotion = (payload.get('detected_emotion') or '').strip() or None
        tts_voice = (payload.get('tts_voice') or '').strip()

        if user_id_raw is None:
            return jsonify({'error': 'userId is required'}), 400
        if not session_id:
            return jsonify({'error': 'sessionId is required'}), 400
        if not transcript:
            return jsonify({'error': 'text is required for stream mode'}), 400

        try:
            user_id = int(user_id_raw)
        except (TypeError, ValueError):
            return jsonify({'error': 'userId must be integer'}), 400

        logs = fetch_history(session_id, limit=20)
        history = parse_history_items(logs)

        save_chat_log(
            user_id=user_id,
            session_id=session_id,
            role='user',
            content=transcript,
            emotion_label=detected_emotion,
        )

        def event_stream():
            try:
                raw_text = ''
                for token in stream_qwen_companion(transcript, detected_emotion, history):
                    raw_text += token
                    yield f"data: {json.dumps({'type': 'delta', 'text': token}, ensure_ascii=False)}\n\n"

                emotion = '平静'
                reply = raw_text
                try:
                    parsed = json.loads(raw_text)
                    emotion = parsed.get('emotion', emotion)
                    reply = parsed.get('reply', reply)
                except Exception:
                    pass

                save_chat_log(
                    user_id=user_id,
                    session_id=session_id,
                    role='assistant',
                    content=reply,
                    emotion_label=emotion,
                )

                done_payload = {
                    'type': 'done',
                    'emotion': emotion,
                    'reply': reply,
                    'raw': raw_text,
                }
                yield f"data: {json.dumps(done_payload, ensure_ascii=False)}\n\n"
            except Exception as e:
                yield f"data: {json.dumps({'type': 'error', 'error': str(e)}, ensure_ascii=False)}\n\n"

        return Response(
            event_stream(),
            mimetype='text/event-stream',
            headers={
                'Cache-Control': 'no-cache',
                'Connection': 'keep-alive',
                'X-Accel-Buffering': 'no',
            },
        )

    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/companion/audio/analyze', methods=['POST', 'OPTIONS'])
def companion_audio_analyze():
    """阶段1：仅做语音识别+情绪识别，返回 transcript 与 detected_emotion。"""
    if request.method == 'OPTIONS':
        return '', 200

    if 'audio' not in request.files:
        return jsonify({'error': 'audio file is required'}), 400

    audio_file = request.files['audio']
    if audio_file.filename == '':
        return jsonify({'error': 'No audio file selected'}), 400

    temp_path = None
    try:
        file_suffix = os.path.splitext(audio_file.filename)[1] or '.wav'
        with tempfile.NamedTemporaryFile(suffix=file_suffix, delete=False) as temp_file:
            temp_path = temp_file.name
            audio_file.save(temp_path)

        with open(temp_path, 'rb') as f:
            audio_content = f.read()

        frontend_transcript = (request.form.get('transcript') or '').strip()
        upload_name = audio_file.filename or f"recording{file_suffix}"
        upload_mime = audio_file.mimetype or 'application/octet-stream'
        files = {'audio': (upload_name, audio_content, upload_mime)}
        data = {'transcript': frontend_transcript} if frontend_transcript else {}

        emotion_response = requests.post(EMOTION_SERVICE_URL, files=files, data=data, timeout=60)
        if emotion_response.status_code != 200:
            return jsonify({'error': f'Emotion recognition service failed: {emotion_response.text}'}), 500

        emotion_data = emotion_response.json()
        detected_emotion = emotion_data.get('complex_emotion', emotion_data.get('basic_emotion', 'neutral'))
        transcript = (emotion_data.get('transcript') or frontend_transcript).strip()

        if not transcript:
            return jsonify({'error': 'No transcript from audio'}), 400

        return jsonify({
            'transcript': transcript,
            'detected_emotion': detected_emotion,
            'emotion_details': emotion_data,
        })
    except requests.exceptions.RequestException as e:
        return jsonify({'error': f'Failed to call emotion service: {str(e)}'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500
    finally:
        if temp_path and os.path.exists(temp_path):
            os.unlink(temp_path)


@app.route('/api/companion/tts', methods=['POST', 'OPTIONS'])
def companion_tts():
    """阶段3：异步 TTS 生成，不阻塞流式文字输出。"""
    if request.method == 'OPTIONS':
        return '', 200

    payload = request.get_json(silent=True) or {}
    text = (payload.get('text') or '').strip()
    tts_voice = (payload.get('tts_voice') or '').strip() or None

    if not text:
        return jsonify({'error': 'text is required'}), 400

    tts_payload = build_tts_payload(text, tts_voice)
    return jsonify(tts_payload)


@app.route('/api/companion/history', methods=['GET'])
def companion_history():
    session_id = (request.args.get('sessionId') or '').strip()
    if not session_id:
        return jsonify({'error': 'sessionId is required'}), 400

    logs = fetch_history(session_id, limit=200)
    result = []
    for log in logs:
        result.append({
            'id': str(log.get('_id')),
            'userId': log.get('userId'),
            'sessionId': log.get('sessionId'),
            'role': log.get('role'),
            'content': log.get('content'),
            'emotionLabel': log.get('emotionLabel'),
            'createdAt': log.get('createdAt').isoformat() if log.get('createdAt') else None,
        })

    return jsonify({'sessionId': session_id, 'messages': result})


if __name__ == '__main__':
    print('Flask running...')
    app.run(host='0.0.0.0', port=5000, debug=True)
