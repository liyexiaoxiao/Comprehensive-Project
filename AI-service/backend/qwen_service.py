import os

from dotenv import load_dotenv
from openai import OpenAI


load_dotenv()

QWEN_API_KEY = os.getenv("DASHSCOPE_API_KEY")


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
    if not QWEN_API_KEY:
        raise RuntimeError("未配置 DASHSCOPE_API_KEY")

    client = OpenAI(
        api_key=QWEN_API_KEY,
        base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
    )

    completion = client.chat.completions.create(
        model="qwen3.5-plus",
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
    if not QWEN_API_KEY:
        raise RuntimeError("未配置 DASHSCOPE_API_KEY")

    client = OpenAI(api_key=QWEN_API_KEY, base_url="https://dashscope.aliyuncs.com/compatible-mode/v1")

    completion = client.chat.completions.create(
        model="qwen3.5-plus",
        messages=build_companion_prompt(user_text, detected_emotion, history),
        extra_body={"enable_thinking": True},
    )

    content = completion.choices[0].message.content if completion.choices else ""
    if not content:
        raise RuntimeError("模型未返回内容")
    return content


def stream_qwen_companion(user_text: str, detected_emotion: str = None, history: list = None):
    """流式调用 Qwen，逐段产出文本 token。"""
    if not QWEN_API_KEY:
        raise RuntimeError("未配置 DASHSCOPE_API_KEY")

    client = OpenAI(api_key=QWEN_API_KEY, base_url="https://dashscope.aliyuncs.com/compatible-mode/v1")

    stream = client.chat.completions.create(
        model="qwen3.5-plus",
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
