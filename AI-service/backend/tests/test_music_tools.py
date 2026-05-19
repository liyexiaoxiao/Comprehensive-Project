import music_tools


def test_resolve_music_emotion_should_prefer_explicit_emotion_alias():
    assert music_tools.resolve_music_emotion("anything", "anxiety") == "fear"
    assert music_tools.resolve_music_emotion("anything", "contentment") == "neutral"


def test_resolve_music_emotion_should_fallback_to_query_keywords():
    assert music_tools.resolve_music_emotion("I feel very sad today") == "sadness"
    assert music_tools.resolve_music_emotion("please play sleep music") == "neutral"


def test_recommend_music_should_filter_by_emotion_and_limit(monkeypatch):
    monkeypatch.setattr(
        music_tools,
        "list_music_files",
        lambda: ["joy_1.mp3", "joy_2.mp3", "neutral_1.mp3", "anger_1.mp3"],
    )

    result = music_tools.recommend_music(query="", emotion="optimism", limit=1)

    assert result["type"] == "music_recommendation"
    assert result["emotion"] == "joy"
    assert [item["filename"] for item in result["items"]] == ["joy_1.mp3"]


def test_recommend_music_should_fallback_to_neutral_candidates(monkeypatch):
    monkeypatch.setattr(music_tools, "list_music_files", lambda: ["neutral_1.mp3", "neutral_2.mp3"])

    result = music_tools.recommend_music(query="", emotion="anger", limit=5)

    assert result["emotion"] == "anger"
    assert [item["filename"] for item in result["items"]] == ["neutral_1.mp3", "neutral_2.mp3"]
