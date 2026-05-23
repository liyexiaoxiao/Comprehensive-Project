import os
import requests

# 通过网关调用 music-service（推荐）
# 网关会验证 JWT 并设置 X-User-Id
MUSIC_SERVICE_BASE_URL = os.getenv("MUSIC_SERVICE_URL", "http://api-gateway:8080/api/music/v1")

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


def resolve_music_emotion(query: str = "", emotion: str = None):
    normalized_emotion = EMOTION_ALIASES.get(str(emotion or "").strip().lower())
    if normalized_emotion:
        return normalized_emotion

    lowered_query = str(query or "").lower()
    for keywords, target in KEYWORD_EMOTIONS:
        if any(keyword in lowered_query for keyword in keywords):
            return target

    return "neutral"


def _get_emotion_tag_id(emotion_key: str, jwt_token: str = None):
    tag_name = EMOTION_TAG_NAME_MAP.get(emotion_key, "平静")
    try:
        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        resp = requests.get(
            f"{MUSIC_SERVICE_BASE_URL}/emotion-tags/by-name",
            params={"name": tag_name},
            headers=headers,
            timeout=5
        )
        if resp.status_code == 200:
            return resp.json().get("id")
    except Exception as e:
        print(f"Failed to get emotion tag id for {tag_name}: {e}")
    return None


def recommend_music(query: str = "", emotion: str = None, limit: int = 3, user_id: int = 10001, jwt_token: str = None):
    selected_emotion = resolve_music_emotion(query, emotion)
    tag_id = _get_emotion_tag_id(selected_emotion, jwt_token)

    items = []
    label = EMOTION_LABELS.get(selected_emotion, "平静")

    if not tag_id:
        print(f"No emotion tag found for {selected_emotion}")
        return {
            "type": "music_recommendation",
            "emotion": selected_emotion,
            "items": [],
            "error": f"未找到对应的情绪标签：{label}",
        }

    try:
        payload = {"emotionTagId": tag_id, "limit": limit}
        headers = {}
        if jwt_token:
            headers["Authorization"] = f"Bearer {jwt_token}"

        resp = requests.post(
            f"{MUSIC_SERVICE_BASE_URL}/me/music-recommendations/by-emotion",
            json=payload,
            headers=headers,
            timeout=10
        )

        if resp.status_code != 200:
            print(f"Music service returned status {resp.status_code}: {resp.text}")
            return {
                "type": "music_recommendation",
                "emotion": selected_emotion,
                "items": [],
                "error": f"音乐服务暂时不可用（状态码：{resp.status_code}）",
            }

        resources = resp.json()
        for index, res in enumerate(resources, start=1):
            file_url = res.get("fileUrl", "")

            items.append({
                "id": str(res.get("id")),
                "title": res.get("title") or "未命名音乐",
                "artist": res.get("artist") or label,
                "duration": res.get("duration", 0),
                "emotion": selected_emotion,
                "cover": res.get("coverUrl") or f"/images/feature-img-{((index - 1) % 4) + 1}.jpg",
                "tags": [label, selected_emotion],
                "reason": f"根据你的情绪，为你推荐这首{label}的音乐。",
                "url": file_url,
            })
    except requests.exceptions.Timeout:
        print(f"Music service request timeout")
        return {
            "type": "music_recommendation",
            "emotion": selected_emotion,
            "items": [],
            "error": "音乐服务响应超时，请稍后再试。",
        }
    except Exception as e:
        print(f"Failed to call music-service recommendation: {e}")
        return {
            "type": "music_recommendation",
            "emotion": selected_emotion,
            "items": [],
            "error": f"获取音乐推荐时出错：{str(e)}",
        }

    return {
        "type": "music_recommendation",
        "emotion": selected_emotion,
        "items": items,
    }
