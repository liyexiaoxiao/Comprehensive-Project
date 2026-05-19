import os
import uuid

import requests
from dotenv import load_dotenv


BASE_DIR = os.path.dirname(__file__)
DOTENV_PATH = os.path.join(BASE_DIR, ".env")
load_dotenv(DOTENV_PATH)

SILICONFLOW_TTS_URL = "https://api.siliconflow.cn/v1/audio/speech"
DEFAULT_TTS_MODEL = os.getenv("SILICONFLOW_TTS_MODEL", "FunAudioLLM/CosyVoice2-0.5B")
DEFAULT_TTS_VOICE = os.getenv("SILICONFLOW_TTS_VOICE", "FunAudioLLM/CosyVoice2-0.5B:alex")
AVAILABLE_TTS_VOICES = {
    "alex": "FunAudioLLM/CosyVoice2-0.5B:alex",
    "anna": "FunAudioLLM/CosyVoice2-0.5B:anna",
    "bella": "FunAudioLLM/CosyVoice2-0.5B:bella",
    "benjamin": "FunAudioLLM/CosyVoice2-0.5B:benjamin",
    "charles": "FunAudioLLM/CosyVoice2-0.5B:charles",
    "claire": "FunAudioLLM/CosyVoice2-0.5B:claire",
    "david": "FunAudioLLM/CosyVoice2-0.5B:david",
    "diana": "FunAudioLLM/CosyVoice2-0.5B:diana",
}


def normalize_tts_voice(voice: str = None) -> str:
    selected_voice = (voice or DEFAULT_TTS_VOICE).strip()
    if selected_voice in AVAILABLE_TTS_VOICES:
        return AVAILABLE_TTS_VOICES[selected_voice]
    if selected_voice in AVAILABLE_TTS_VOICES.values():
        return selected_voice
    return DEFAULT_TTS_VOICE


def synthesize_speech(text: str, output_dir: str, voice: str = None) -> str:
    """Generate speech with SiliconFlow CosyVoice and return the saved filename."""
    clean_text = (text or "").strip()
    if not clean_text:
        raise ValueError("TTS input text is empty")
    siliconflow_api_key = (os.getenv("SILICONFLOW_API_KEY") or "").strip()
    if not siliconflow_api_key:
        raise RuntimeError("SILICONFLOW_API_KEY is not configured")

    os.makedirs(output_dir, exist_ok=True)
    filename = f"{uuid.uuid4().hex}.mp3"
    output_path = os.path.join(output_dir, filename)

    payload = {
        "model": DEFAULT_TTS_MODEL,
        "voice": normalize_tts_voice(voice),
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
            "Authorization": f"Bearer {siliconflow_api_key}",
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
