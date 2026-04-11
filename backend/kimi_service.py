import os
from openai import OpenAI


KIMI_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
ALLOWED_EMOTIONS = [
    "平静",
    "放松",
    "专注",
    "疲惫",
    "焦虑",
    "悲伤",
    "孤独",
    "喜悦",
    "愤怒",
    "充满希望",
]


def generate_meditation_guide(emotion_name: str) -> str:
    """根据情绪标签生成冥想引导词。"""
    if emotion_name not in ALLOWED_EMOTIONS:
        raise RuntimeError("不支持的情绪类型")

    client = OpenAI(api_key="your api key", base_url=KIMI_BASE_URL)

    completion = client.chat.completions.create(
        model="kimi-k2.5",
        messages=[
            {
                "role": "system",
                "content": (
                    "你是一名专业冥想引导师。请根据用户情绪生成强针对性的中文冥想引导词。"
                    "必须做到不同情绪输出明显不同，避免模板化复用。"
                    "请严格按以下要求："
                    "1）时长约1~2分钟可朗读，直接输出自然段，不要标题和分点；"
                    "2）包含呼吸节奏提示（如吸4拍、停2拍、呼6拍），并与该情绪匹配；"
                    "3）每种情绪必须使用不同意象与关注点："
                    "平静=维持稳定与觉察；"
                    "放松=身体扫描与肌肉放松；"
                    "专注=注意力锚定与分心回收；"
                    "疲惫=恢复能量与温和休息；"
                    "焦虑=安全感、放慢呼吸、降低警觉；"
                    "悲伤=允许情绪流动与自我安抚；"
                    "孤独=连接感与被陪伴感；"
                    "喜悦=延展积极感受与感恩；"
                    "愤怒=卸力释放与降温；"
                    "充满希望=愿景巩固与行动意图；"
                    "4）开头先点名当前情绪，中段给对应练习，结尾给一句该情绪专属收束语；"
                    "5）禁止医学诊断、说教和泛化鸡汤。"
                ),
            },
            {
                "role": "user",
                "content": (
                    f"用户选择的冥想背景音情绪是：{emotion_name}。"
                    "请输出只针对这个情绪的冥想引导，不要写其他情绪，不要与常见通用冥想文案相似。"
                ),
            },
        ],
        temperature=0.9,
    )

    content = completion.choices[0].message.content if completion.choices else ""
    if not content:
        raise RuntimeError("Kimi 未返回冥想引导词")

    return content.strip()
