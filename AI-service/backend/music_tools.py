import os
import requests
from urllib.parse import quote

MUSIC_SERVICE_BASE_URL = os.getenv("MUSIC_SERVICE_URL", "http://localhost:8081/api/v1")
MUSIC_DIR = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "..", "backend", "music"))

EMOTION_TAG_NAME_MAP = {
    "joy": "高兴",
    "sadness": "悲伤",
    "anger": "愤怒",
    "fear": "恐惧",
    "love": "爱",
    "surprise": "惊讶",
    "neutral": "平静",
    "calm": "平静",
    "disgust": "厌恶",
}

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


def _get_emotion_tag_id(emotion_key: str):
    tag_name = EMOTION_TAG_NAME_MAP.get(emotion_key, "平静")
    try:
        resp = requests.get(f"{MUSIC_SERVICE_BASE_URL}/emotion-tags/by-name", params={"name": tag_name}, timeout=5)
        if resp.status_code == 200:
            return resp.json().get("id")
    except Exception as e:
        print(f"Failed to get emotion tag id for {tag_name}: {e}")
    return None


def recommend_music(query: str = "", emotion: str = None, limit: int = 3, user_id: int = 10001):
    selected_emotion = resolve_music_emotion(query, emotion)
    tag_id = _get_emotion_tag_id(selected_emotion)
    
    items = []
    label = EMOTION_LABELS.get(selected_emotion, "平静")
    
    if tag_id:
        try:
            payload = {"emotionTagId": tag_id, "limit": limit}
            headers = {"X-User-Id": str(user_id)}
            resp = requests.post(f"{MUSIC_SERVICE_BASE_URL}/me/music-recommendations/by-emotion", 
                                 json=payload, headers=headers, timeout=5)
            if resp.status_code == 200:
                resources = resp.json()
                for index, res in enumerate(resources, start=1):
                    filename = os.path.basename(res.get("fileUrl", ""))
                    if not filename:
                        source = res.get("source", "")
                        if source.startswith("python-public:"):
                            filename = source[len("python-public:"):]
                    
                    items.append({
                        "id": str(res.get("id")),
                        "title": res.get("title"),
                        "artist": res.get("artist") or label,
                        "duration": res.get("duration", 0),
                        "emotion": selected_emotion,
                        "filename": filename,
                        "cover": res.get("coverUrl") or f"/images/feature-img-{((index - 1) % 4) + 1}.jpg",
                        "tags": [label, selected_emotion],
                        "reason": f"根据你的情绪，为你推荐这首{label}的音乐。",
                        "url": res.get("fileUrl") or (f"/api/music/file/{quote(filename)}" if filename else None),
                    })
        except Exception as e:
            print(f"Failed to call music-service recommendation: {e}")

    # Fallback to local file scanning if API fails or returns nothing
    if not items:
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
