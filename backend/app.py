import json

from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
from music_service import get_music_file, get_music_list, get_next_music, get_prev_music
from qwen_service import query_qwen_companion, analyze_emotion_by_qwen
from kimi_service import generate_meditation_guide

app = Flask(__name__)
CORS(app)  

@app.route('/api/analyze', methods=['POST', 'OPTIONS'])
def analyze_text():
    if request.method == 'OPTIONS':
        return '', 200

    data = request.get_json() or {}
    text = (data.get("text") or "").strip()
    if not text:
        return jsonify({"error": "No text provided"}), 400

    try:
        emotion = analyze_emotion_by_qwen(text)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

    return jsonify({"emotion": emotion})

@app.route('/api/music/<emotion>', methods=['GET'])
def get_music(emotion):
    try:
        file_path, error = get_music_file(emotion)
        if error:
            return jsonify({"error": error}), 404
            
        return send_file(file_path, mimetype='audio/mpeg')
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/music/<emotion>/next', methods=['GET'])
def get_next(emotion):
    try:
        file_path, error = get_next_music(emotion)
        if error:
            return jsonify({"error": error}), 404
            
        return send_file(file_path, mimetype='audio/mpeg')
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/music/<emotion>/prev', methods=['GET'])
def get_prev(emotion):
    try:
        file_path, error = get_prev_music(emotion)
        if error:
            return jsonify({"error": error}), 404
            
        return send_file(file_path, mimetype='audio/mpeg')
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/music/list', methods=['GET'])
def list_music():
    try:
        music_list = get_music_list()
        return jsonify({"music_files": music_list})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/api/companion/chat', methods=['POST', 'OPTIONS'])
def companion_chat():
    if request.method == 'OPTIONS':
        return '', 200

    data = request.get_json() or {}
    user_text = (data.get('text') or '').strip()

    if not user_text:
        return jsonify({'error': 'No text provided'}), 400

    try:
        model_raw = query_qwen_companion(user_text)

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
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/meditation/guide', methods=['POST', 'OPTIONS'])
def meditation_guide():
    if request.method == 'OPTIONS':
        return '', 200

    data = request.get_json() or {}
    emotion = (data.get('emotion') or '').strip()

    if not emotion:
        return jsonify({'error': 'No emotion provided'}), 400

    try:
        guide = generate_meditation_guide(emotion)
        return jsonify({'emotion': emotion, 'guide': guide})
    except Exception as e:
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    print("🚀 Flask running...")
    app.run(host='0.0.0.0', port=5000, debug=True)
