from datetime import datetime, timezone
import sys
import types

fake_pymongo = types.ModuleType("pymongo")


class FakeCollection:
    def find(self, query):
        return self

    def sort(self, key, direction):
        return []

    def insert_one(self, doc):
        return None


class FakeDatabase:
    def __getitem__(self, name):
        return FakeCollection()


class FakeMongoClient:
    def __init__(self, uri):
        self.uri = uri

    def __getitem__(self, name):
        return FakeDatabase()


fake_pymongo.MongoClient = FakeMongoClient
sys.modules.setdefault("pymongo", fake_pymongo)

import app as backend_app


def test_parse_history_items_should_keep_only_valid_chat_messages():
    logs = [
        {"role": "user", "content": "hello"},
        {"role": "assistant", "content": "hi"},
        {"role": "system", "content": "skip"},
        {"role": "user", "content": ""},
    ]

    assert backend_app.parse_history_items(logs) == [
        {"role": "user", "content": "hello"},
        {"role": "assistant", "content": "hi"},
    ]


def test_build_tts_payload_should_return_audio_url(monkeypatch):
    monkeypatch.setattr(backend_app, "synthesize_speech", lambda reply, output_dir, voice=None: "voice.mp3")

    payload = backend_app.build_tts_payload("hello", "alex")

    assert payload == {"audio_url": "/api/audio/voice.mp3", "tts_error": None}


def test_build_tts_payload_should_capture_tts_error(monkeypatch):
    def raise_error(reply, output_dir, voice=None):
        raise RuntimeError("tts failed")

    monkeypatch.setattr(backend_app, "synthesize_speech", raise_error)

    payload = backend_app.build_tts_payload("hello")

    assert payload == {"audio_url": None, "tts_error": "tts failed"}


def test_companion_chat_should_reject_missing_user_id():
    client = backend_app.app.test_client()

    response = client.post("/api/companion/chat", json={"sessionId": "s1", "text": "hello"})

    assert response.status_code == 400
    assert response.get_json()["error"] == "userId is required"


def test_companion_chat_should_reject_empty_text():
    client = backend_app.app.test_client()

    response = client.post("/api/companion/chat", json={"userId": 7, "sessionId": "s1", "text": "   "})

    assert response.status_code == 400
    assert response.get_json()["error"] == "No text or audio provided"


def test_companion_chat_should_return_model_reply_and_save_logs(monkeypatch):
    saved_logs = []

    monkeypatch.setattr(backend_app, "fetch_history", lambda session_id, limit=20: [])
    monkeypatch.setattr(
        backend_app,
        "save_chat_log",
        lambda user_id, session_id, role, content, emotion_label=None: saved_logs.append(
            (user_id, session_id, role, content, emotion_label)
        ),
    )
    monkeypatch.setattr(
        backend_app,
        "query_qwen_companion_with_tools",
        lambda transcript, detected_emotion, history: {
            "raw": '{"emotion":"calm","reply":"I hear you"}',
            "emotion": "calm",
            "reply": "I hear you",
            "tool_results": [],
        },
    )
    monkeypatch.setattr(
        backend_app,
        "build_tts_payload",
        lambda reply, voice=None: {"audio_url": "/api/audio/fake.mp3", "tts_error": None},
    )
    client = backend_app.app.test_client()

    response = client.post(
        "/api/companion/chat",
        json={"userId": "7", "sessionId": "s1", "text": "hello", "detected_emotion": "joy"},
    )

    body = response.get_json()
    assert response.status_code == 200
    assert body["userId"] == 7
    assert body["reply"] == "I hear you"
    assert body["audio_url"] == "/api/audio/fake.mp3"
    assert saved_logs[0] == (7, "s1", "user", "hello", "joy")
    assert saved_logs[1] == (7, "s1", "assistant", "I hear you", "calm")


def test_companion_history_should_format_messages(monkeypatch):
    monkeypatch.setattr(
        backend_app,
        "fetch_history",
        lambda session_id, limit=200: [
            {
                "_id": "abc",
                "userId": 7,
                "sessionId": session_id,
                "role": "user",
                "content": "hello",
                "emotionLabel": "joy",
                "createdAt": datetime(2026, 5, 18, tzinfo=timezone.utc),
            }
        ],
    )
    client = backend_app.app.test_client()

    response = client.get("/api/companion/history?sessionId=s1")

    body = response.get_json()
    assert response.status_code == 200
    assert body["sessionId"] == "s1"
    assert body["messages"][0]["content"] == "hello"
    assert body["messages"][0]["createdAt"] == "2026-05-18T00:00:00+00:00"
