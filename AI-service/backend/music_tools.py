import os
from urllib.parse import quote


MUSIC_DIR = os.path.join(os.path.dirname(__file__), "music")

EMOTION_ALIASES = {
    "anxiety": "fear",
    "fear": "fear",
    "helplessness": "sadness",
    "depression_tendency": "sadness",
    "remorse": "sadness",
    "sadness": "sadness",
    "frustration": "anger",
    "aggressiveness": "anger",
    "contempt": "anger",
    "anger": "anger",
    "disgust": "anger",
    "excitement": "joy",
    "optimism": "joy",
    "joy": "joy",
    "contentment": "neutral",
    "calm": "neutral",
    "neutral": "neutral",
    "surprise": "surprise",
    "love": "love",
}

KEYWORD_EMOTIONS = [
    (("焦虑", "紧张", "害怕", "恐惧", "担心", "panic", "anxious", "fear"), "fear"),
    (("难过", "悲伤", "低落", "沮丧", "无助", "sad", "down"), "sadness"),
    (("生气", "愤怒", "烦躁", "挫败", "火大", "angry", "mad"), "anger"),
    (("开心", "快乐", "高兴", "兴奋", "joy", "happy"), "joy"),
    (("惊喜", "惊讶", "surprise"), "surprise"),
    (("爱", "温柔", "陪伴", "love"), "love"),
    (("平静", "安静", "放松", "睡前", "舒缓", "冥想", "calm", "relax", "sleep"), "neutral"),
]

EMOTION_LABELS = {
    "anger": "愤怒",
    "fear": "安抚焦虑",
    "joy": "轻快",
    "love": "温柔",
    "neutral": "平静",
    "sadness": "低落陪伴",
    "surprise": "新鲜感",
}


def list_music_files():
    if not os.path.isdir(MUSIC_DIR):
        return []
    return sorted(
        filename for filename in os.listdir(MUSIC_DIR)
        if filename.lower().endswith(".mp3")
    )


def resolve_music_emotion(query: str = "", emotion: str = None):
    normalized_emotion = EMOTION_ALIASES.get(str(emotion or "").strip().lower())
    if normalized_emotion:
        return normalized_emotion

    lowered_query = str(query or "").lower()
    for keywords, target in KEYWORD_EMOTIONS:
        if any(keyword in lowered_query for keyword in keywords):
            return target

    return "neutral"


def recommend_music(query: str = "", emotion: str = None, limit: int = 3):
    selected_emotion = resolve_music_emotion(query, emotion)
    all_files = list_music_files()
    candidates = [
        filename for filename in all_files
        if filename.lower().startswith(f"{selected_emotion}_")
    ]
    if not candidates:
        candidates = [
            filename for filename in all_files
            if filename.lower().startswith("neutral_")
        ]
    if not candidates:
        candidates = all_files

    safe_limit = max(1, min(int(limit or 3), 5))
    label = EMOTION_LABELS.get(selected_emotion, "平静")
    items = []
    for index, filename in enumerate(candidates[:safe_limit], start=1):
        title = os.path.splitext(filename)[0]
        items.append({
            "id": f"tool-{title}",
            "title": title,
            "artist": label,
            "duration": 0,
            "emotion": selected_emotion,
            "filename": filename,
            "cover": f"/images/feature-img-{((index - 1) % 4) + 1}.jpg",
            "tags": [label, selected_emotion],
            "reason": f"这首更贴近你现在想要的{label}氛围。",
            "url": f"/api/music/file/{quote(filename)}",
        })

    return {
        "type": "music_recommendation",
        "emotion": selected_emotion,
        "items": items,
    }
