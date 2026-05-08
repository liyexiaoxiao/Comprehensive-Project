import json
import requests
import tempfile
import os

from flask import Flask, request, jsonify
from flask_cors import CORS
from qwen_service import query_qwen_companion

app = Flask(__name__)
CORS(app)

# 情绪识别服务的地址
EMOTION_SERVICE_URL = "http://localhost:5003/predict"

@app.route('/api/companion/chat', methods=['POST', 'OPTIONS'])
def companion_chat():
    """接收音频文件，调用情绪识别服务，然后生成对话回复"""
    if request.method == 'OPTIONS':
        return '', 200

    # 检查是否有音频文件
    if 'audio' in request.files:
        audio_file = request.files['audio']
        if audio_file.filename == '':
            return jsonify({'error': 'No audio file selected'}), 400

        temp_path = None
        try:
            # 保存音频文件到临时位置
            with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as temp_file:
                temp_path = temp_file.name
                audio_file.save(temp_path)

            # 读取文件内容并发送到情绪识别服务
            with open(temp_path, 'rb') as f:
                audio_content = f.read()

            # 获取前端传来的 transcript（如果有）
            frontend_transcript = request.form.get('transcript', '').strip()

            files = {'audio': ('recording.wav', audio_content, 'audio/wav')}
            data = {'transcript': frontend_transcript} if frontend_transcript else {}
            emotion_response = requests.post(EMOTION_SERVICE_URL, files=files, data=data, timeout=30)

            if emotion_response.status_code != 200:
                error_detail = emotion_response.text
                return jsonify({'error': f'Emotion recognition service failed: {error_detail}'}), 500

            emotion_data = emotion_response.json()

            # 打印情绪识别服务返回的完整结果
            print("=" * 60)
            print("情绪识别服务返回结果:")
            print(json.dumps(emotion_data, ensure_ascii=False, indent=2))
            print("=" * 60)

            # 提取识别结果
            detected_emotion = emotion_data.get('complex_emotion', emotion_data.get('basic_emotion', 'neutral'))
            transcript = emotion_data.get('transcript', '')

            if not transcript:
                return jsonify({'error': 'No transcript from audio'}), 400

            # 调用 LLM 生成回复
            model_raw = query_qwen_companion(transcript, detected_emotion)

            # 解析 LLM 返回的 JSON
            emotion = '平静'
            reply = model_raw
            try:
                parsed = json.loads(model_raw)
                emotion = parsed.get('emotion', emotion)
                reply = parsed.get('reply', reply)
            except Exception:
                pass

            return jsonify({
                'emotion': emotion,
                'reply': reply,
                'raw': model_raw,
                'detected_emotion': detected_emotion,
                'transcript': transcript,
                'emotion_details': emotion_data,  # 返回完整的情绪识别结果
            })

        except requests.exceptions.RequestException as e:
            return jsonify({'error': f'Failed to call emotion service: {str(e)}'}), 500
        except Exception as e:
            return jsonify({'error': str(e)}), 500
        finally:
            # 清理临时文件
            if temp_path and os.path.exists(temp_path):
                os.unlink(temp_path)

    # 兼容旧的文本接口（如果前端直接发送文本）
    else:
        data = request.get_json() or {}
        user_text = (data.get('text') or '').strip()
        detected_emotion = (data.get('detected_emotion') or '').strip()

        if not user_text:
            return jsonify({'error': 'No text or audio provided'}), 400

        try:
            # 将情绪参数传递给 LLM
            model_raw = query_qwen_companion(user_text, detected_emotion if detected_emotion else None)

            # 优先按 JSON 解析；如果模型偶发不按 JSON 返回，则回退为兜底结构
            emotion = '平静'
            reply = model_raw
            try:
                parsed = json.loads(model_raw)
                emotion = parsed.get('emotion', emotion)
                reply = parsed.get('reply', reply)
            except Exception:
                pass

            return jsonify({
                'emotion': emotion,
                'reply': reply,
                'raw': model_raw,
                'detected_emotion': detected_emotion,
            })
        except Exception as e:
            return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    print("🚀 Flask running...")
    app.run(host='0.0.0.0', port=5000, debug=True)
