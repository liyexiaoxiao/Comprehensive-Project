import os
from flask import send_file, jsonify

# 音乐文件目录
MUSIC_DIR = os.path.join(os.path.dirname(__file__), 'music')

# 确保音乐目录存在
if not os.path.exists(MUSIC_DIR):
    os.makedirs(MUSIC_DIR)

# 情绪到音乐文件的映射
EMOTION_MAPPING = {
    'joy': 'joy.mp3',
    'sadness': 'sadness.mp3',
    'anger': 'anger.mp3',
    'fear': 'fear.mp3',
    'love': 'love.mp3',
    'surprise': 'surprise.mp3',
    'neutral': 'neutral.mp3'
}

def get_music_file(emotion):
    """
    根据情绪获取对应的音乐文件
    
    Args:
        emotion (str): 情绪标签
        
    Returns:
        tuple: (file_path, error_message)
    """
    try:
        # 获取对应的音乐文件名
        filename = EMOTION_MAPPING.get(emotion.lower(), 'neutral.mp3')
        file_path = os.path.join(MUSIC_DIR, filename)
        
        # 检查文件是否存在
        if not os.path.exists(file_path):
            return None, "音乐文件未找到"
            
        return file_path, None
        
    except Exception as e:
        return None, str(e)

def get_music_list():
    """
    获取所有可用的音乐文件列表
    
    Returns:
        list: 音乐文件列表
    """
    try:
        if not os.path.exists(MUSIC_DIR):
            return []
            
        return [f for f in os.listdir(MUSIC_DIR) if f.endswith('.mp3')]
        
    except Exception:
        return [] 