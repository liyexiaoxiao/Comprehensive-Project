import os
from flask import send_file, jsonify
import random

# 音乐文件目录
MUSIC_DIR = os.path.join(os.path.dirname(__file__), 'music')

# 确保音乐目录存在
if not os.path.exists(MUSIC_DIR):
    os.makedirs(MUSIC_DIR)

# 情绪到音乐文件的映射
EMOTION_MAPPING = {
    'joy': ['joy_1.mp3', 'joy_2.mp3', 'joy_3.mp3', 'joy_4.mp3', 'joy_5.mp3'],
    'sadness': ['sadness_1.mp3', 'sadness_2.mp3', 'sadness_3.mp3', 'sadness_4.mp3', 'sadness_5.mp3'],
    'anger': ['anger_1.mp3', 'anger_2.mp3', 'anger_3.mp3', 'anger_4.mp3', 'anger_5.mp3'],
    'fear': ['fear_1.mp3', 'fear_2.mp3', 'fear_3.mp3', 'fear_4.mp3', 'fear_5.mp3'],
    'love': ['love_1.mp3', 'love_2.mp3', 'love_3.mp3', 'love_4.mp3', 'love_5.mp3'],
    'surprise': ['surprise_1.mp3', 'surprise_2.mp3', 'surprise_3.mp3', 'surprise_4.mp3', 'surprise_5.mp3'],
    'neutral': ['neutral_1.mp3', 'neutral_2.mp3', 'neutral_3.mp3', 'neutral_4.mp3', 'neutral_5.mp3']
}

# 当前播放的音乐索引
current_music_index = {}

def get_music_file(emotion):
    """
    根据情绪获取对应的音乐文件
    """
    try:
        # 获取对应的音乐文件列表
        filenames = EMOTION_MAPPING.get(emotion.lower(), ['neutral_1.mp3'])
        
        # 如果是新的情绪，初始化索引
        if emotion not in current_music_index:
            current_music_index[emotion] = 0
            
        # 获取当前索引的音乐文件
        filename = filenames[current_music_index[emotion]]
        file_path = os.path.join(MUSIC_DIR, filename)
        
        # 检查文件是否存在
        if not os.path.exists(file_path):
            return None, "音乐文件未找到"
            
        return file_path, None
        
    except Exception as e:
        return None, str(e)

def get_next_music(emotion):
    """
    获取下一首音乐
    """
    try:
        filenames = EMOTION_MAPPING.get(emotion.lower(), ['neutral_1.mp3'])
        
        # 更新索引
        current_music_index[emotion] = (current_music_index.get(emotion, 0) + 1) % len(filenames)
        
        return get_music_file(emotion)
        
    except Exception as e:
        return None, str(e)

def get_prev_music(emotion):
    """
    获取上一首音乐
    """
    try:
        filenames = EMOTION_MAPPING.get(emotion.lower(), ['neutral_1.mp3'])
        
        # 更新索引
        current_music_index[emotion] = (current_music_index.get(emotion, 0) - 1) % len(filenames)
        
        return get_music_file(emotion)
        
    except Exception as e:
        return None, str(e)

def get_music_list():
    """
    获取所有可用的音乐文件列表
    """
    try:
        if not os.path.exists(MUSIC_DIR):
            return []
            
        return [f for f in os.listdir(MUSIC_DIR) if f.endswith('.mp3')]
        
    except Exception:
        return [] 