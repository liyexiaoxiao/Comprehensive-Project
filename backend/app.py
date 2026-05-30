import json

from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
from auth import current_user_id, install_jwt_auth
from music_service import (
    delete_uploaded_music,
    get_music_file,
    get_music_file_by_name,
    get_music_list,
    get_next_music,
    get_prev_music,
    list_uploaded_music,
    save_uploaded_music,
)

app = Flask(__name__)
CORS(app)  
install_jwt_auth(
    app,
    public_paths=[
        ("GET", "/api/music/list"),
        ("GET", "/api/music/file"),
    ],
)


def load_emotion_analyzer():
    from emotion_model import analyze_emotion
    return analyze_emotion


def parse_user_id(raw_value):
    if raw_value in (None, ''):
        return None
    try:
        return int(raw_value)
    except (TypeError, ValueError):
        return None

@app.route('/api/analyze', methods=['POST', 'OPTIONS'])
def analyze_text():
    if request.method == 'OPTIONS':
        return '', 200

    data = request.get_json()
    text = data.get("text", "")
    if not text:
        return jsonify({"error": "No text provided"}), 400

    try:
        analyze_emotion = load_emotion_analyzer()
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


@app.route('/api/music/file/<path:filename>', methods=['GET'])
def get_music_by_filename(filename):
    try:
        file_path, error = get_music_file_by_name(filename)
        if error:
            return jsonify({"error": error}), 404

        return send_file(file_path, mimetype='audio/mpeg')
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/api/music/uploads', methods=['GET'])
def list_uploaded_tracks():
    try:
        user_id = parse_user_id(current_user_id())
        tracks = list_uploaded_music(user_id)
        return jsonify({"tracks": tracks})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/api/music/uploads', methods=['POST', 'OPTIONS'])
def upload_music_file():
    if request.method == 'OPTIONS':
        return '', 200

    try:
        file = request.files.get('file')
        title = request.form.get('title', '')
        artist = request.form.get('artist', '')
        duration = request.form.get('duration', 0)
        tags_raw = request.form.get('tags', '[]')
        try:
            tags = json.loads(tags_raw) if tags_raw else []
        except json.JSONDecodeError:
            tags = []

        saved_track, error = save_uploaded_music(
            file_storage=file,
            user_id=parse_user_id(current_user_id()),
            title=title,
            artist=artist,
            duration=duration,
            tags=tags,
        )
        if error:
            return jsonify({"error": error}), 400
        return jsonify(saved_track), 201
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/api/music/uploads/<track_id>', methods=['DELETE'])
def delete_uploaded_track(track_id):
    try:
        deleted, error, status_code = delete_uploaded_music(
            track_id,
            parse_user_id(current_user_id())
        )
        if not deleted:
            return jsonify({"error": error}), status_code
        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    print("Flask running...")
    app.run(host='0.0.0.0', port=5000, debug=True, use_reloader=False)
