import json

from flask import Flask, request, jsonify
from flask_cors import CORS
from qwen_service import query_qwen_companion

app = Flask(__name__)
CORS(app)

@app.route('/api/companion/chat', methods=['POST', 'OPTIONS'])
def companion_chat():
    if request.method == 'OPTIONS':
        return '', 200

    data = request.get_json() or {}
    user_text = (data.get('text') or '').strip()
    detected_emotion = (data.get('detected_emotion') or '').strip()  # 从前端接收情绪参数

    if not user_text:
        return jsonify({'error': 'No text provided'}), 400

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
            'detected_emotion': detected_emotion,  # 返回识别出的情绪供前端参考
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    print("🚀 Flask running...")
    app.run(host='0.0.0.0', port=5000, debug=True)
