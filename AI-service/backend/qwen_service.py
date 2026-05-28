import json
import os

from dotenv import load_dotenv
from openai import OpenAI
from music_tools import recommend_music


BASE_DIR = os.path.dirname(__file__)
DOTENV_PATH = os.path.join(BASE_DIR, ".env")
load_dotenv(DOTENV_PATH)

COMPANION_TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "recommend_music",
            "description": "Recommend suitable local music when the user asks for music, songs, relaxing audio, sleep music, or similar help.",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "The user's music request or current situation.",
                    },
                    "emotion": {
                        "type": "string",
                        "description": "The user's current emotion label if available.",
                    },
                    "limit": {
                        "type": "integer",
                        "description": "Number of tracks to recommend, from 1 to 5.",
                    },
                },
                "required": ["query"],
            },
        },
    }
]

MUSIC_INTENT_KEYWORDS = (
    "推荐音乐", "推荐歌曲", "推荐歌", "想听歌", "想听音乐", "听点音乐",
    "来点音乐", "来首歌", "放首歌", "放音乐", "找首歌", "音乐推荐",
    "睡前音乐", "放松音乐", "冥想音乐", "music", "song", "songs",
)


def _has_music_intent(text: str):
    lowered = str(text or "").lower()
    return any(keyword in lowered for keyword in MUSIC_INTENT_KEYWORDS)

def raw_qwen_reply(user_text: str, system_promt: str = None):
    qwen_api_key = (os.getenv("DASHSCOPE_API_KEY") or "").strip()
    if not qwen_api_key:
        raise RuntimeError("未配置 DASHSCOPE_API_KEY")
    client = OpenAI(api_key=qwen_api_key, base_url="https://dashscope.aliyuncs.com/compatible-mode/v1")
    messages = []
    if system_promt:
        messages.append({"role": "system", "content": system_promt})
    messages.append({"role": "user", "content": user_text})
    completion = client.chat.completions.create(
        model="qwen3.5-plus-2026-04-20",
        messages=messages,
        extra_body={"enable_thinking": False},
    )
    content = completion.choices[0].message.content if completion.choices else ""
    return content.strip() if content else ""


def _run_companion_tool(name: str, arguments: dict, user_id: int = 10001, jwt_token: str = None):
    if name == "recommend_music":
        return recommend_music(
            query=arguments.get("query", ""),
            emotion=arguments.get("emotion"),
            limit=arguments.get("limit", 3),
            user_id=user_id,
            jwt_token=jwt_token,
        )
    raise ValueError(f"Unsupported tool: {name}")


def build_companion_prompt(user_text: str, detected_emotion: str = None, history: list = None):
    """构建陪伴式回复提示词（支持多轮历史）。

    Args:
        user_text: 用户输入的文本
        detected_emotion: 从语音识别出的情绪（可选）
        history: 历史消息列表，元素格式: {"role": "user|assistant", "content": "..."}
    """
    emotion_context = ""
    if detected_emotion:
        emotion_map = {
            "joy": "开心、愉悦",
            "anger": "愤怒、生气",
            "fear": "恐惧、害怕",
            "sadness": "悲伤、难过",
            "anxiety": "焦虑、不安",
            "frustration": "挫败、沮丧",
            "excitement": "兴奋、激动",
            "calm": "平静、放松",
            "neutral": "平静、中性",
            "helplessness": "无助、无力",
            "depression_tendency": "抑郁倾向",
            "contempt": "轻蔑、不屑",
            "disgust": "厌恶、反感",
            "surprise": "惊讶、意外",
            "optimism": "乐观、积极",
            "contentment": "满足、知足",
            "remorse": "懊悔、后悔",
            "aggressiveness": "攻击性、激进",
            "uncertain": "不确定"
        }
        emotion_desc = emotion_map.get(detected_emotion, detected_emotion)
        emotion_context = f"\n\n【语音情绪分析】用户当前的语音情绪倾向为：{emotion_desc}。请结合这一情绪状态给出更贴合的回复。"

    messages = [
        {
            "role": "system",
            "content": (
                "你是一名温柔、专业、克制的情绪陪伴助手。"
                "任务：根据用户话语判断其大致情绪，并给出简短开导与陪伴建议。"
                "请严格使用 JSON 回复，格式为："
                '{"emotion":"情绪标签","reply":"给用户的话"}。'
                "其中 emotion 仅从 [平静, 焦虑, 压力大, 开心, 迷茫, 孤独] 中选一个最贴近的。"
                "reply 语气温暖，不说教，不超过120字。"
                + emotion_context
            ),
        }
    ]

    if history:
        for item in history:
            role = item.get("role")
            content = item.get("content")
            if role in ("user", "assistant") and content:
                messages.append({"role": role, "content": content})

    messages.append({"role": "user", "content": user_text})
    return messages


def analyze_emotion_by_qwen(user_text: str):
    """通过 Qwen 输出单一情绪标签。"""
    qwen_api_key = (os.getenv("DASHSCOPE_API_KEY") or "").strip()
    if not qwen_api_key:
        raise RuntimeError("未配置 DASHSCOPE_API_KEY")

    client = OpenAI(
        api_key=qwen_api_key,
        base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
    )

    completion = client.chat.completions.create(
        model="qwen3.5-plus-2026-04-20",
        messages=[
            {
                "role": "system",
                "content": "请只返回一个情绪词，不要解释：平静/焦虑/悲伤/愤怒/压力大/开心/迷茫",
            },
            {"role": "user", "content": user_text},
        ],
        extra_body={"enable_thinking": True},
    )

    content = completion.choices[0].message.content if completion.choices else ""
    return (content or "平静").strip()


def query_qwen_companion(user_text: str, detected_emotion: str = None, history: list = None):
    """调用 DashScope 兼容 OpenAI 接口，返回模型原始文本。"""
    qwen_api_key = (os.getenv("DASHSCOPE_API_KEY") or "").strip()
    if not qwen_api_key:
        raise RuntimeError("未配置 DASHSCOPE_API_KEY")

    client = OpenAI(api_key=qwen_api_key, base_url="https://dashscope.aliyuncs.com/compatible-mode/v1")

    completion = client.chat.completions.create(
        model="qwen3.5-plus-2026-04-20",
        messages=build_companion_prompt(user_text, detected_emotion, history),
        extra_body={"enable_thinking": True},
    )

    content = completion.choices[0].message.content if completion.choices else ""
    if not content:
        raise RuntimeError("模型未返回内容")
    return content


def query_qwen_companion_with_tools(user_text: str, detected_emotion: str = None, history: list = None, user_id: int = 10001, jwt_token: str = None):
    """Call Qwen with native tool calling and return parsed reply plus tool results."""
    qwen_api_key = (os.getenv("DASHSCOPE_API_KEY") or "").strip()
    if not qwen_api_key:
        raise RuntimeError("未配置 DASHSCOPE_API_KEY")

    client = OpenAI(api_key=qwen_api_key, base_url="https://dashscope.aliyuncs.com/compatible-mode/v1")
    messages = build_companion_prompt(user_text, detected_emotion, history)
    messages.insert(1, {
        "role": "system",
        "content": (
            "You may call recommend_music only when the user's latest message clearly "
            "expresses a desire to listen to music, asks for song/music/audio "
            "recommendations, asks to play music, or requests sleep, meditation, "
            "relaxation, or focus audio. Do not call recommend_music for greetings, "
            "small talk, general emotional sharing, or ordinary companionship unless "
            "the user explicitly mentions wanting music, songs, or audio. "
            "If no music tool is needed, answer normally as JSON only. "
            "After tool results are available, also reply as JSON only: "
            '{"emotion":"情绪标签","reply":"给用户的话"}。'
        ),
    })

    first_completion = client.chat.completions.create(
        model="qwen3.5-plus-2026-04-20",
        messages=messages,
        tools=COMPANION_TOOLS,
        tool_choice="auto",
        extra_body={"enable_thinking": False},
    )

    first_message = first_completion.choices[0].message if first_completion.choices else None
    if first_message is None:
        raise RuntimeError("模型未返回内容")

    tool_results = []
    tool_calls = getattr(first_message, "tool_calls", None) or []
    if tool_calls:
        messages.append(first_message.model_dump(exclude_none=True))
        for tool_call in tool_calls:
            function = tool_call.function
            arguments = json.loads(function.arguments or "{}")
            if not arguments.get("emotion") and detected_emotion:
                arguments["emotion"] = detected_emotion
            result = _run_companion_tool(function.name, arguments, user_id=user_id, jwt_token=jwt_token)
            tool_results.append(result)
            messages.append({
                "role": "tool",
                "tool_call_id": tool_call.id,
                "content": json.dumps(result, ensure_ascii=False),
            })

        final_completion = client.chat.completions.create(
            model="qwen3.5-plus-2026-04-20",
            messages=messages,
            extra_body={"enable_thinking": False},
        )
        final_message = final_completion.choices[0].message if final_completion.choices else None
        raw_content = final_message.content if final_message else ""
    else:
        raw_content = first_message.content or ""

    if not raw_content:
        raw_content = '{"emotion":"\\u5e73\\u9759","reply":"\\u6211\\u5728\\u8fd9\\u91cc\\uff0c\\u542c\\u89c1\\u4f60\\u4e86\\u3002\\u60f3\\u804a\\u4ec0\\u4e48\\u90fd\\u53ef\\u4ee5\\u6162\\u6162\\u8bf4\\u3002"}'

    emotion = "平静"
    reply = raw_content
    try:
        parsed = json.loads(raw_content)
        emotion = parsed.get("emotion", emotion)
        reply = parsed.get("reply", reply)
    except Exception:
        pass

    return {
        "raw": raw_content,
        "emotion": emotion,
        "reply": reply,
        "tool_results": tool_results,
    }


def stream_qwen_companion(user_text: str, detected_emotion: str = None, history: list = None):
    """流式调用 Qwen，逐段产出文本 token。"""
    qwen_api_key = (os.getenv("DASHSCOPE_API_KEY") or "").strip()
    if not qwen_api_key:
        raise RuntimeError("未配置 DASHSCOPE_API_KEY")

    client = OpenAI(api_key=qwen_api_key, base_url="https://dashscope.aliyuncs.com/compatible-mode/v1")

    stream = client.chat.completions.create(
        model="qwen3.5-plus-2026-04-20",
        messages=build_companion_prompt(user_text, detected_emotion, history),
        stream=True,
        extra_body={"enable_thinking": True},
    )

    for chunk in stream:
        if not chunk.choices:
            continue
        delta = chunk.choices[0].delta
        if delta and getattr(delta, "content", None):
            yield delta.content
