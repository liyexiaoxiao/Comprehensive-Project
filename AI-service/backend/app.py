import json
import os
import tempfile
from datetime import datetime, timezone

import requests
from flask import Flask, request, jsonify
from flask_cors import CORS
from pymongo import MongoClient
from qwen_service import query_qwen_companion

app = Flask(__name__)
CORS(app)

# 情绪识别服务的地址
EMOTION_SERVICE_URL = os.getenv("EMOTION_SERVICE_URL", "http://localhost:5003/predict")

# MongoDB 配置（可通过环境变量覆盖）
MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017")
MONGO_DB = os.getenv("MONGO_DB", "comprehensive_project")
MONGO_COLLECTION = os.getenv("MONGO_COLLECTION", "ai_chat_log")

mongo_client = MongoClient(MONGO_URI)
mongo_db = mongo_client[MONGO_DB]
ai_chat_log_collection = mongo_db[MONGO_COLLECTION]


def now_utc():
    return datetime.now(timezone.utc)


def parse_history_items(logs):
    """将 DB 历史记录转换成 LLM 所需消息格式。"""
    history = []
    for log in logs:
        role = log.get("role")
        content = log.get("content")
        if role in ("user", "assistant") and content:
            history.append({"role": role, "content": content})
    return history


def fetch_history(session_id: str, limit: int = 20):
    """按 createdAt 顺序获取会话历史。"""
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
    """保存单条聊天日志，字段与 AiChatLog.java 对齐。"""
    doc = {
        "userId": user_id,
        "sessionId": session_id,
        "role": role,
        "content": content,
        "emotionLabel": emotion_label,
        "createdAt": now_utc(),
    }
    ai_chat_log_collection.insert_one(doc)


@app.route('/api/companion/chat', methods=['POST', 'OPTIONS'])
def companion_chat():
    """支持文本/语音输入的多轮对话，并将用户与AI消息入库 MongoDB。"""
    if request.method == 'OPTIONS':
        return '', 200

    try:
        # 统一读取会话参数
        user_id_raw = request.form.get('userId') if request.form else None
        session_id = request.form.get('sessionId') if request.form else None

        if request.is_json:
            payload = request.get_json() or {}
            user_id_raw = payload.get('userId', user_id_raw)
            session_id = payload.get('sessionId', session_id)

        if user_id_raw is None:
            return jsonify({'error': 'userId is required'}), 400
        if not session_id:
            return jsonify({'error': 'sessionId is required'}), 400

        try:
            user_id = int(user_id_raw)
        except (ValueError, TypeError):
            return jsonify({'error': 'userId must be integer'}), 400

        # 1) 处理输入（语音优先）
        detected_emotion = None
        transcript = ''

        if 'audio' in request.files:
            audio_file = request.files['audio']
            if audio_file.filename == '':
                return jsonify({'error': 'No audio file selected'}), 400

            temp_path = None
            try:
                with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_file:
                    temp_path = temp_file.name
                    audio_file.save(temp_path)

                with open(temp_path, 'rb') as f:
                    audio_content = f.read()

                frontend_transcript = request.form.get('transcript', '').strip()
                files = {'audio': ('recording.wav', audio_content, 'audio/wav')}
                data = {'transcript': frontend_transcript} if frontend_transcript else {}

                emotion_response = requests.post(EMOTION_SERVICE_URL, files=files, data=data, timeout=30)
                if emotion_response.status_code != 200:
                    return jsonify({'error': f'Emotion recognition service failed: {emotion_response.text}'}), 500

                emotion_data = emotion_response.json()
                detected_emotion = emotion_data.get('complex_emotion', emotion_data.get('basic_emotion', 'neutral'))
                transcript = (emotion_data.get('transcript') or '').strip()

                if not transcript:
                    return jsonify({'error': 'No transcript from audio'}), 400

            finally:
                if temp_path and os.path.exists(temp_path):
                    os.unlink(temp_path)

        else:
            data = request.get_json() or {}
            transcript = (data.get('text') or '').strip()
            detected_emotion = (data.get('detected_emotion') or '').strip() or None
            if not transcript:
                return jsonify({'error': 'No text or audio provided'}), 400

        # 2) 拉取历史 -> 组装多轮上下文
        logs = fetch_history(session_id, limit=20)
        history = parse_history_items(logs)

        # 3) 先保存用户消息
        save_chat_log(
            user_id=user_id,
            session_id=session_id,
            role='user',
            content=transcript,
            emotion_label=detected_emotion,
        )

        # 4) 调用模型（带历史）
        model_raw = query_qwen_companion(transcript, detected_emotion, history)

        emotion = '平静'
        reply = model_raw
        try:
            parsed = json.loads(model_raw)
            emotion = parsed.get('emotion', emotion)
            reply = parsed.get('reply', reply)
        except Exception:
            pass

        # 5) 保存 AI 回复
        save_chat_log(
            user_id=user_id,
            session_id=session_id,
            role='assistant',
            content=reply,
            emotion_label=emotion,
        )

        return jsonify({
            'userId': user_id,
            'sessionId': session_id,
            'emotion': emotion,
            'reply': reply,
            'raw': model_raw,
            'detected_emotion': detected_emotion,
            'transcript': transcript,
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/companion/history', methods=['GET'])
def companion_history():
    """按会话查询历史消息。"""
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
