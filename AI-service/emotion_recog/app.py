# -*- coding: utf-8 -*-
"""
Speech Emotion Recognition Microservice
Flask backend - dual-channel fusion with VA mapping and complex emotion inference
"""

import os
os.environ.setdefault("HF_HOME", "D:/hf_cache")
os.environ.setdefault("HF_HUB_OFFLINE", "1")
os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")

import math
import tempfile
import numpy as np
from flask import Flask, request, jsonify
import torch
import torchaudio
from langdetect import detect

app = Flask(__name__)

# ---------------------------------------------------------------------------
# Russell VA coordinates for each unified emotion label
# Source: Russell (1980) & Warriner et al. (2013)
# ---------------------------------------------------------------------------
VA_MAP = {
    "joy":          ( 0.80,  0.60),
    "anger":        (-0.60,  0.80),
    "fear":         (-0.60,  0.60),
    "surprise":     ( 0.10,  0.70),
    "disgust":      (-0.60,  0.20),
    "sadness":      (-0.70, -0.50),
    "neutral":      ( 0.00,  0.00),
    "calm":         ( 0.30, -0.60),
}

# Unified label space (8 labels)
UNIFIED_LABELS = ["anger", "calm", "disgust", "fear", "joy", "neutral", "sadness", "surprise"]

# Mapping from speech model raw labels → unified labels
# Model outputs uppercase short codes: ANG, CAL, DIS, FEA, HAP, NEU, SAD, SUR
SPEECH_LABEL_MAP = {
    # uppercase short codes (actual model output)
    "ANG": "anger", "CAL": "calm", "DIS": "disgust", "FEA": "fear",
    "HAP": "joy",   "NEU": "neutral", "SAD": "sadness", "SUR": "surprise",
    # mixed-case variants (fallback)
    "Angry": "anger", "Calm": "calm", "Disgust": "disgust", "Fear": "fear",
    "Happy": "joy", "Neutral": "neutral", "Sad": "sadness", "Surprise": "surprise",
    "ang": "anger", "cal": "calm", "dis": "disgust", "fea": "fear",
    "hap": "joy", "neu": "neutral", "sad": "sadness", "sur": "surprise",
}

# Mapping from text model raw labels → unified labels
TEXT_LABEL_MAP = {
    "anger": "anger", "disgust": "disgust", "fear": "fear",
    "joy": "joy", "neutral": "neutral", "sadness": "sadness", "surprise": "surprise",
}

# Global model variables
speech_classifier = None
speech_to_text = None
text_classifier = None
zh_en_translator = None
models_ready = False
models_error = None


def load_model():
    global speech_classifier, speech_to_text, text_classifier, zh_en_translator, models_ready, models_error
    if models_ready:
        return
    device = 0 if torch.cuda.is_available() else -1
    print(f"Loading models on {'cuda' if device == 0 else 'cpu'}...")

    try:
        from transformers import pipeline
        speech_classifier = pipeline(
            "audio-classification",
            model="prithivMLmods/Speech-Emotion-Classification",
            device=device,
            model_kwargs={"local_files_only": True},
        )
        speech_to_text = pipeline(
            "automatic-speech-recognition",
            model="openai/whisper-tiny",
            device=device,
            model_kwargs={"local_files_only": True},
        )
        text_classifier = pipeline(
            "text-classification",
            model="j-hartmann/emotion-english-distilroberta-base",
            return_all_scores=True,
            framework="pt",
            device=device,
            model_kwargs={"local_files_only": True},
        )
        zh_en_translator = pipeline(
            "translation",
            model="Helsinki-NLP/opus-mt-zh-en",
            device=device,
            model_kwargs={"local_files_only": True},
        )
    except Exception as exc:
        models_ready = False
        models_error = str(exc)
        raise RuntimeError(
            "本地模型加载失败，请确认 Hugging Face 缓存已存在于 D:/hf_cache/hub"
        ) from exc
    print("Models loaded successfully!")
    models_ready = True
    models_error = None


# ---------------------------------------------------------------------------
# Helper: build a zero-initialised probability dict over UNIFIED_LABELS
# ---------------------------------------------------------------------------
def _zero_probs():
    return {label: 0.0 for label in UNIFIED_LABELS}


def _normalise(probs: dict) -> dict:
    total = sum(probs.values())
    if total <= 0:
        return probs
    return {k: v / total for k, v in probs.items()}


# ---------------------------------------------------------------------------
# Acoustic feature extraction (librosa optional — graceful fallback)
# ---------------------------------------------------------------------------
def extract_acoustic_features(audio_array: np.ndarray, sr: int = 16000) -> dict:
    """Return speech_rate, rms, pause_rate, rms_std from raw audio."""
    try:
        import librosa
        rms = float(np.sqrt(np.mean(audio_array ** 2)))
        rms_std = float(np.std(librosa.feature.rms(y=audio_array)[0]))
        # Rough speech-rate proxy: zero-crossing rate correlates with voiced activity
        zcr = librosa.feature.zero_crossing_rate(audio_array)[0]
        speech_rate = float(np.mean(zcr) * sr)  # pseudo syllable rate
        # Pause rate: fraction of frames with very low energy
        frame_rms = librosa.feature.rms(y=audio_array)[0]
        pause_rate = float(np.mean(frame_rms < 0.01))
        return {"speech_rate": speech_rate, "rms": rms, "rms_std": rms_std, "pause_rate": pause_rate}
    except ImportError:
        # librosa not installed — use simple numpy fallback
        rms = float(np.sqrt(np.mean(audio_array ** 2)))
        rms_std = float(np.std(audio_array))
        return {"speech_rate": 0.0, "rms": rms, "rms_std": rms_std, "pause_rate": 0.0}


def estimate_audio_quality(features: dict) -> float:
    """Estimate speech quality in [0, 1] from rms_std (stability proxy)."""
    rms_std = features.get("rms_std", 0.0)
    # Stable speech → low rms_std relative to rms; noisy → high
    rms = features.get("rms", 1e-6)
    snr_proxy = rms / (rms_std + 1e-6)
    return float(min(1.0, max(0.3, snr_proxy / 10.0)))


# ---------------------------------------------------------------------------
# Stage 1: label alignment
# ---------------------------------------------------------------------------
def align_speech_probs(raw_results: list) -> dict:
    """Convert speech model output list → unified probability dict."""
    probs = _zero_probs()
    for item in raw_results:
        unified = SPEECH_LABEL_MAP.get(item["label"])
        if unified:
            probs[unified] = float(item["score"])
    return probs


def align_text_probs(raw_results: list) -> dict:
    """Convert text model output list → unified probability dict (calm=0)."""
    probs = _zero_probs()
    for item in raw_results:
        unified = TEXT_LABEL_MAP.get(item["label"])
        if unified:
            probs[unified] = float(item["score"])
    return probs


# ---------------------------------------------------------------------------
# Stage 2a: dynamic weight fusion
# ---------------------------------------------------------------------------
def dynamic_fusion(p_acoustic: dict, p_text: dict,
                   audio_quality: float, asr_confidence: float) -> tuple:
    """
    Returns (p_merged, w_acoustic, w_text).
    Weights are proportional to max-prob × quality signal.
    """
    w_a_raw = max(p_acoustic.values()) * audio_quality
    w_t_raw = max(p_text.values()) * asr_confidence
    total = w_a_raw + w_t_raw
    if total < 1e-9:
        w_a, w_t = 0.5, 0.5
    else:
        w_a = w_a_raw / total
        w_t = w_t_raw / total

    # Acoustic floor: ensure acoustic weight is at least 0.5.
    # Prevents semantically-neutral speech (e.g. RAVDESS fixed sentences) from
    # letting a high-confidence neutral text result dominate the fusion.
    ACOUSTIC_FLOOR = 0.5
    if w_a < ACOUSTIC_FLOOR:
        w_a = ACOUSTIC_FLOOR
        w_t = 1.0 - w_a

    p_merged = {
        label: w_a * p_acoustic[label] + w_t * p_text[label]
        for label in UNIFIED_LABELS
    }
    p_merged = _normalise(p_merged)
    return p_merged, round(w_a, 4), round(w_t, 4)


# ---------------------------------------------------------------------------
# Stage 2b: conflict detection
# ---------------------------------------------------------------------------
def detect_conflict(p_acoustic: dict, p_text: dict) -> tuple:
    """
    Returns (inconsistent: bool, masked: bool, conflict_score: float).
    masked=True when acoustic is calm/neutral but text is negative.
    """
    primary_a = max(p_acoustic, key=p_acoustic.get)
    primary_t = max(p_text, key=p_text.get)

    inconsistent = False
    masked = False
    conflict_score = 0.0

    if primary_a != primary_t:
        conflict_score = abs(p_acoustic[primary_a] - p_text[primary_t])
        if conflict_score > 0.3:
            inconsistent = True
            # Emotion masking: calm/neutral voice but negative text content
            if primary_a in ("calm", "neutral") and primary_t in ("sadness", "fear", "anger", "disgust"):
                masked = True

    return inconsistent, masked, round(conflict_score, 4)


# ---------------------------------------------------------------------------
# Stage 2c: VA mapping
# ---------------------------------------------------------------------------
def compute_va(p_merged: dict) -> tuple:
    """Weighted sum of preset VA coordinates → (valence, arousal, intensity)."""
    v = sum(p_merged[label] * VA_MAP[label][0] for label in UNIFIED_LABELS if label in VA_MAP)
    a = sum(p_merged[label] * VA_MAP[label][1] for label in UNIFIED_LABELS if label in VA_MAP)
    intensity = math.sqrt(v ** 2 + a ** 2)
    return round(v, 4), round(a, 4), round(intensity, 4)


# ---------------------------------------------------------------------------
# Stage 2d: complex emotion inference (Plutchik rules)
# ---------------------------------------------------------------------------
def infer_complex_emotion(p_merged: dict, valence: float, arousal: float,
                          features: dict, both_low: bool) -> str:
    p_fear    = p_merged.get("fear", 0)
    p_anger   = p_merged.get("anger", 0)
    p_sadness = p_merged.get("sadness", 0)
    p_joy     = p_merged.get("joy", 0)
    p_calm    = p_merged.get("calm", 0)
    p_disgust = p_merged.get("disgust", 0)
    speech_rate = features.get("speech_rate", 0)
    rms = features.get("rms", 0)

    # --- Dominant-probability path (works even when VA is near-origin) ---
    # Find the top two emotions by probability
    sorted_probs = sorted(p_merged.items(), key=lambda x: x[1], reverse=True)
    top1_label, top1_score = sorted_probs[0]
    top2_label, top2_score = sorted_probs[1]

    # If confidence is very low across the board, fall back to uncertain
    if both_low and top1_score < 0.35:
        return "uncertain"

    # --- VA-quadrant rules (relaxed thresholds: 0.15 instead of 0.3) ---
    V_THRESH = 0.15
    A_THRESH = 0.15

    # Negative high-arousal quadrant
    if valence < -V_THRESH and arousal > A_THRESH:
        high_speech_rate = speech_rate > 200
        if p_fear > p_anger and (high_speech_rate or p_fear > 0.25):
            return "anxiety"
        # anger dominant: refine with Plutchik combinations
        if p_anger >= p_fear:
            if p_disgust > 0.2:
                return "contempt"       # anger + disgust → contempt (蔑视)
            if p_sadness > 0.2:
                return "aggressiveness" # anger + anticipation proxy → aggressiveness (攻击性)
            return "frustration"        # pure anger in context → frustration (挫败感)
        return "fear"

    # Negative low-arousal quadrant
    if valence < -V_THRESH and arousal < -A_THRESH:
        if arousal < -0.4 and rms < 0.05:
            return "depression_tendency"
        if p_fear > 0.2 and p_sadness > 0.2:
            return "helplessness"
        if p_disgust > 0.2 and p_sadness > 0.2:
            return "remorse"            # disgust + sadness → remorse (懊悔)
        return "sadness"

    # Positive high-arousal quadrant
    if valence > V_THRESH and arousal > A_THRESH:
        return "excitement" if arousal > 0.4 else "optimism"

    # Positive low-arousal quadrant
    if valence > V_THRESH and arousal < -A_THRESH:
        return "calm" if (p_calm > 0.2 or arousal < -0.3) else "contentment"

    # --- Near-origin: use dominant probability to infer complex emotion ---
    if top1_score >= 0.35:
        mapping = {
            "anger":   "frustration" if p_disgust < 0.2 else "contempt",
            "fear":    "anxiety" if p_sadness > 0.15 else "fear",
            "sadness": "helplessness" if p_fear > 0.15 else "sadness",
            "disgust": "contempt" if p_anger > 0.15 else "disgust",
            "joy":     "optimism",
            "calm":    "contentment",
            "surprise": "excitement" if valence >= 0 else "anxiety",
            "neutral": "neutral",
        }
        return mapping.get(top1_label, "neutral")

    return "neutral"


# ---------------------------------------------------------------------------
# Text helpers
# ---------------------------------------------------------------------------
def translate_to_english(text: str) -> str:
    global zh_en_translator
    if zh_en_translator is None:
        return text

    translated = zh_en_translator(text)
    if isinstance(translated, list) and translated:
        return translated[0].get("translation_text", text)
    if isinstance(translated, dict):
        return translated.get("translation_text", text)
    return text


def analyze_text_emotion(text: str) -> tuple:
    """Returns (unified_probs dict, asr_confidence float, detected_lang, translated_text)."""
    try:
        lang = detect(text)
    except Exception:
        lang = "en"

    translated = text
    if lang.startswith("zh"):
        try:
            translated = translate_to_english(text)
        except Exception:
            pass

    raw = text_classifier(translated)
    # pipeline with return_all_scores=True returns [[{label, score}, ...]]
    scores = raw[0] if isinstance(raw[0], list) else raw
    probs = align_text_probs(scores)
    top_score = max(s["score"] for s in scores)
    return probs, round(float(top_score), 4), lang, translated


# ---------------------------------------------------------------------------
# Flask routes
# ---------------------------------------------------------------------------
@app.route('/predict', methods=['POST'])
def predict():
    global speech_classifier, speech_to_text, text_classifier, models_ready, models_error
    if not models_ready or speech_classifier is None:
        try:
            load_model()
        except Exception:
            return jsonify({'error': 'Models not loaded', 'detail': models_error}), 503
    if 'audio' not in request.files:
        return jsonify({'error': 'No audio file provided'}), 400

    audio_file = request.files['audio']
    if audio_file.filename == '':
        return jsonify({'error': 'No file selected'}), 400

    temp_path = None
    try:
        ext = os.path.splitext(audio_file.filename)[1] or '.wav'
        with tempfile.NamedTemporaryFile(suffix=ext, delete=False) as f:
            temp_path = f.name
            audio_file.save(temp_path)

        # Load & preprocess audio
        waveform, sample_rate = torchaudio.load(temp_path)
        if waveform.shape[0] > 1:
            waveform = torch.mean(waveform, dim=0, keepdim=True)
        if sample_rate != 16000:
            waveform = torchaudio.transforms.Resample(sample_rate, 16000)(waveform)
        audio_array = waveform.squeeze().numpy()

        # Edge case: audio too short (< 1 second)
        low_quality = len(audio_array) < 16000

        # --- Acoustic channel ---
        acoustic_features = extract_acoustic_features(audio_array)
        audio_quality = 0.5 if low_quality else estimate_audio_quality(acoustic_features)

        # 传递 waveform 数组给 pipeline，避免不同后端对文件格式解析差异
        speech_audio_input = {"array": audio_array, "sampling_rate": 16000}
        asr_audio_input = {"raw": audio_array, "sampling_rate": 16000}
        speech_raw = speech_classifier(speech_audio_input, top_k=None)
        p_acoustic = align_speech_probs(speech_raw)
        speech_top_label = max(p_acoustic, key=p_acoustic.get)
        speech_top_score = round(p_acoustic[speech_top_label] * 100, 2)

        # --- Text channel ---
        # 优先使用前端传来的 transcript，如果没有则使用 Whisper 识别
        frontend_transcript = request.form.get('transcript', '').strip()

        if frontend_transcript:
            # 使用前端浏览器识别的文本（更准确）
            transcript_text = frontend_transcript
            print(f"使用前端识别的文本: {transcript_text}")
        else:
            # 回退到 Whisper 识别（同样传数组避免文件后端问题）
            transcription = speech_to_text(asr_audio_input, generate_kwargs={"task": "transcribe", "language": "zh"})
            transcript_text = transcription.get("text", "").strip()
            print(f"使用 Whisper 识别的文本: {transcript_text}")

        p_text = _zero_probs()
        asr_confidence = 0.0
        text_top_label = None
        text_top_score = None
        detected_lang = None
        translated_text_val = None

        if transcript_text:
            p_text, asr_confidence, detected_lang, translated_text_val = analyze_text_emotion(transcript_text)
            text_top_label = max(p_text, key=p_text.get)
            text_top_score = round(p_text[text_top_label] * 100, 2)
        else:
            asr_confidence = 0.0

        # --- Stage 2: fusion pipeline ---
        p_merged, w_acoustic, w_text = dynamic_fusion(p_acoustic, p_text, audio_quality, asr_confidence)
        inconsistent, masked, conflict_score = detect_conflict(p_acoustic, p_text)
        valence, arousal, intensity = compute_va(p_merged)

        # Confidence: weighted mean of top scores
        top_acoustic = max(p_acoustic.values())
        top_text = max(p_text.values()) if any(p_text.values()) else 0.0
        confidence = round(w_acoustic * top_acoustic + w_text * top_text, 4)
        both_low = top_acoustic < 0.4 and top_text < 0.4

        complex_emotion = infer_complex_emotion(p_merged, valence, arousal, acoustic_features, both_low)
        basic_emotion = max(p_merged, key=p_merged.get)

        return jsonify({
            # Structured output (design doc §6.1)
            "basic_emotion":    basic_emotion,
            "complex_emotion":  complex_emotion,
            "valence":          valence,
            "arousal":          arousal,
            "intensity":        intensity,
            "masked":           masked,
            "inconsistent":     inconsistent,
            "confidence":       confidence,
            "acoustic_weight":  w_acoustic,
            "text_weight":      w_text,
            # Per-model details for UI
            "speech_emotion":   speech_top_label,
            "speech_confidence": speech_top_score,
            "text_emotion":     text_top_label,
            "text_confidence":  text_top_score,
            "transcript":       transcript_text,
            "detected_language": detected_lang,
            "translated_text":  translated_text_val,
            "low_quality":      low_quality,
            "conflict_score":   conflict_score,
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 500
    finally:
        if temp_path and os.path.exists(temp_path):
            os.remove(temp_path)


if __name__ == '__main__':
    try:
        load_model()
    except Exception:
        pass
    port = int(os.environ.get('PORT', 5003))
    app.run(host='0.0.0.0', port=port, debug=False)
