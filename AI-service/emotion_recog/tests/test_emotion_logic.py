import sys
import types


fake_torch = types.ModuleType("torch")
fake_torch.cuda = types.SimpleNamespace(is_available=lambda: False)
fake_torch.mean = lambda waveform, dim=0, keepdim=True: waveform
sys.modules.setdefault("torch", fake_torch)

fake_torchaudio = types.ModuleType("torchaudio")
fake_torchaudio.load = lambda path: (_ for _ in ()).throw(RuntimeError("torchaudio.load is not mocked"))
fake_torchaudio.transforms = types.SimpleNamespace(
    Resample=lambda source, target: (lambda waveform: waveform)
)
sys.modules.setdefault("torchaudio", fake_torchaudio)

fake_transformers = types.ModuleType("transformers")
fake_transformers.pipeline = lambda *args, **kwargs: None
sys.modules.setdefault("transformers", fake_transformers)

fake_langdetect = types.ModuleType("langdetect")
fake_langdetect.detect = lambda text: "en"
sys.modules.setdefault("langdetect", fake_langdetect)

import app as emotion_app


def test_align_speech_probs_should_map_model_labels_to_unified_labels():
    probs = emotion_app.align_speech_probs([
        {"label": "HAP", "score": 0.7},
        {"label": "SAD", "score": 0.2},
        {"label": "UNKNOWN", "score": 0.9},
    ])

    assert probs["joy"] == 0.7
    assert probs["sadness"] == 0.2
    assert probs["anger"] == 0.0


def test_align_text_probs_should_ignore_unsupported_labels():
    probs = emotion_app.align_text_probs([
        {"label": "joy", "score": 0.8},
        {"label": "love", "score": 0.6},
    ])

    assert probs["joy"] == 0.8
    assert "love" not in probs


def test_dynamic_fusion_should_apply_acoustic_floor():
    p_acoustic = emotion_app._zero_probs()
    p_text = emotion_app._zero_probs()
    p_acoustic["calm"] = 0.2
    p_text["sadness"] = 0.9

    merged, w_acoustic, w_text = emotion_app.dynamic_fusion(
        p_acoustic,
        p_text,
        audio_quality=0.3,
        asr_confidence=0.9,
    )

    assert w_acoustic == 0.5
    assert w_text == 0.5
    assert merged["sadness"] > merged["calm"]


def test_detect_conflict_should_mark_masked_negative_text_with_calm_voice():
    p_acoustic = emotion_app._zero_probs()
    p_text = emotion_app._zero_probs()
    p_acoustic["calm"] = 0.9
    p_text["sadness"] = 0.5

    inconsistent, masked, conflict_score = emotion_app.detect_conflict(p_acoustic, p_text)

    assert inconsistent is True
    assert masked is True
    assert conflict_score == 0.4


def test_compute_va_should_return_weighted_coordinates():
    p_merged = emotion_app._zero_probs()
    p_merged["joy"] = 1.0

    valence, arousal, intensity = emotion_app.compute_va(p_merged)

    assert valence == 0.8
    assert arousal == 0.6
    assert intensity == 1.0


def test_infer_complex_emotion_should_detect_anxiety_in_negative_high_arousal():
    p_merged = emotion_app._zero_probs()
    p_merged["fear"] = 0.6
    p_merged["anger"] = 0.2

    result = emotion_app.infer_complex_emotion(
        p_merged,
        valence=-0.5,
        arousal=0.6,
        features={"speech_rate": 250, "rms": 0.2},
        both_low=False,
    )

    assert result == "anxiety"


def test_predict_should_reject_when_models_are_not_loaded(monkeypatch):
    monkeypatch.setattr(emotion_app, "speech_classifier", None)
    client = emotion_app.app.test_client()

    response = client.post("/predict")

    assert response.status_code == 500
    assert response.get_json()["error"] == "Models not loaded"
