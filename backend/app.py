from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
from emotion_model import analyze_emotion 
from music_service import get_music_file, get_music_list, get_next_music, get_prev_music

app = Flask(__name__)
CORS(app)  

@app.route('/api/analyze', methods=['POST', 'OPTIONS'])
def analyze_text():
    if request.method == 'OPTIONS':
        return '', 200

    data = request.get_json()
    text = data.get("text", "")
    if not text:
        return jsonify({"error": "No text provided"}), 400

    try:
        emotion = analyze_emotion(text)  
    except Exception as e:
        return jsonify({"error": str(e)}), 500

    return jsonify({ "emotion": emotion })

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

if __name__ == '__main__':
    print("🚀 Flask running...")
    app.run(host='0.0.0.0', port=5000, debug=True)
