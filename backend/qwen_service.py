import os
from openai import OpenAI


def build_companion_prompt(user_text: str):
    """构建陪伴式回复提示词。"""
    return [
        {
            "role": "system",
            "content": (
                "你是一名温柔、专业、克制的情绪陪伴助手。"
                "任务：根据用户话语判断其大致情绪，并给出简短开导与陪伴建议。"
                "请严格使用 JSON 回复，格式为："
                '{"emotion":"情绪标签","reply":"给用户的话"}。'
                "其中 emotion 仅从 [平静, 焦虑, 悲伤, 愤怒, 压力大, 开心, 迷茫, 孤独] 中选一个最贴近的。"
                "reply 语气温暖，不说教，不超过120字。"
            ),
        },
        {
            "role": "user",
            "content": user_text,
        },
    ]


def analyze_emotion_by_qwen(user_text: str):
    """通过 Qwen 输出单一情绪标签。"""
    client = OpenAI(
        api_key="your api key",
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


def query_qwen_companion(user_text: str):
    """调用 DashScope 兼容 OpenAI 接口，返回模型原始文本。"""
    client = OpenAI(
        api_key="your api key",
        base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
    )

    completion = client.chat.completions.create(
        model="qwen3.5-plus",
        messages=build_companion_prompt(user_text),
        extra_body={"enable_thinking": True},
    )

    content = completion.choices[0].message.content if completion.choices else ""
    if not content:
        raise RuntimeError("模型未返回内容")
    return content
