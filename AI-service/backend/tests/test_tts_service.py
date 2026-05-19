import types

import pytest

import tts_service


def test_normalize_tts_voice_should_resolve_alias():
    assert tts_service.normalize_tts_voice("alex") == "FunAudioLLM/CosyVoice2-0.5B:alex"


def test_normalize_tts_voice_should_keep_full_voice_id():
    full_voice_id = "FunAudioLLM/CosyVoice2-0.5B:anna"

    assert tts_service.normalize_tts_voice(full_voice_id) == full_voice_id


def test_normalize_tts_voice_should_fallback_to_default_for_unknown_voice():
    assert tts_service.normalize_tts_voice("unknown") == tts_service.DEFAULT_TTS_VOICE


def test_synthesize_speech_should_reject_empty_text(tmp_path):
    with pytest.raises(ValueError, match="TTS input text is empty"):
        tts_service.synthesize_speech("   ", str(tmp_path))


def test_synthesize_speech_should_require_api_key(tmp_path, monkeypatch):
    monkeypatch.setattr(tts_service, "SILICONFLOW_API_KEY", None)

    with pytest.raises(RuntimeError, match="SILICONFLOW_API_KEY is not configured"):
        tts_service.synthesize_speech("hello", str(tmp_path))


def test_synthesize_speech_should_save_audio_file(tmp_path, monkeypatch):
    captured_request = {}

    def fake_post(url, headers, json, timeout):
        captured_request["url"] = url
        captured_request["headers"] = headers
        captured_request["json"] = json
        captured_request["timeout"] = timeout
        return types.SimpleNamespace(status_code=200, content=b"mp3-bytes", text="ok")

    monkeypatch.setattr(tts_service, "SILICONFLOW_API_KEY", "test-key")
    monkeypatch.setattr(tts_service.requests, "post", fake_post)

    filename = tts_service.synthesize_speech(" hello ", str(tmp_path), "bella")

    assert filename.endswith(".mp3")
    assert (tmp_path / filename).read_bytes() == b"mp3-bytes"
    assert captured_request["headers"]["Authorization"] == "Bearer test-key"
    assert captured_request["json"]["input"] == "hello"
    assert captured_request["json"]["voice"] == "FunAudioLLM/CosyVoice2-0.5B:bella"


def test_synthesize_speech_should_raise_when_provider_fails(tmp_path, monkeypatch):
    def fake_post(url, headers, json, timeout):
        return types.SimpleNamespace(status_code=500, content=b"", text="provider failed")

    monkeypatch.setattr(tts_service, "SILICONFLOW_API_KEY", "test-key")
    monkeypatch.setattr(tts_service.requests, "post", fake_post)

    with pytest.raises(RuntimeError, match="provider failed"):
        tts_service.synthesize_speech("hello", str(tmp_path))
