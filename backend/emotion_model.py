from transformers import pipeline
from langdetect import detect
from deep_translator import GoogleTranslator


emotion_classifier = pipeline(
    "text-classification",
    model="j-hartmann/emotion-english-distilroberta-base",
    return_all_scores=False,
    framework="pt"  # 强制使用 PyTorch 
)


def translate_to_english(text):
    """
    使用 GoogleTranslator 将中文翻译成英文（如需翻译）
    """
    return GoogleTranslator(source='auto', target='en').translate(text)

def analyze_emotion(text):
    """
    对输入文本进行语言检测、翻译（如需），再进行情绪识别
    """
    try:
        lang = detect(text)
    except Exception:
        lang = 'en'  # 默认英文，防止 langdetect 出错

    print(f"📘 Detected language: {lang}")

    if lang == 'zh-cn' or lang == 'zh-tw' or lang.startswith('zh'):
        translated = translate_to_english(text)
        print(f"🔁 Translated to English: {translated}")
        text = translated
    else:
        print(f"🔁 No translation needed: {text}")

    result = emotion_classifier(text)
    print(f" Emotion Result: {result[0]}")
    return result[0]['label']

