import os
import uuid

import requests
from dotenv import load_dotenv


load_dotenv()

SILICONFLOW_API_KEY = os.getenv("SILICONFLOW_API_KEY")
SILICONFLOW_TTS_URL = "https://api.siliconflow.cn/v1/audio/speech"
DEFAULT_TTS_MODEL = os.getenv("SILICONFLOW_TTS_MODEL", "FunAudioLLM/CosyVoice2-0.5B")
DEFAULT_TTS_VOICE = os.getenv("SILICONFLOW_TTS_VOICE", "FunAudioLLM/CosyVoice2-0.5B:alex")


def synthesize_speech(text: str, output_dir: str) -> str:
    """Generate speech with SiliconFlow CosyVoice and return the saved filename."""
    clean_text = (text or "").strip()
    if not clean_text:
        raise ValueError("TTS input text is empty")
    if not SILICONFLOW_API_KEY:
        raise RuntimeError("SILICONFLOW_API_KEY is not configured")

    os.makedirs(output_dir, exist_ok=True)
    filename = f"{uuid.uuid4().hex}.mp3"
    output_path = os.path.join(output_dir, filename)

    payload = {
        "model": DEFAULT_TTS_MODEL,
        "voice": DEFAULT_TTS_VOICE,
        "input": clean_text,
        "response_format": "mp3",
        "sample_rate": 44100,
        "speed": 1,
        "gain": 0,
        "stream": True,
    }

    response = requests.post(
        SILICONFLOW_TTS_URL,
        headers={
            "Authorization": f"Bearer {SILICONFLOW_API_KEY}",
            "Content-Type": "application/json",
        },
        json=payload,
        timeout=60,
    )
    if response.status_code != 200:
        raise RuntimeError(f"SiliconFlow TTS failed: {response.text}")

    with open(output_path, "wb") as audio_file:
        audio_file.write(response.content)

    return filename
