from flask import Flask, request, jsonify
from flask_cors import CORS
from emotion_model import analyze_emotion 

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

if __name__ == '__main__':
    print("🚀 Flask running...")
    app.run(host='0.0.0.0', port=5000, debug=True)
