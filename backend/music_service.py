import json
import os
import uuid
from datetime import datetime

from werkzeug.utils import secure_filename

# 音乐文件目录
MUSIC_DIR = os.path.join(os.path.dirname(__file__), 'music')
UPLOAD_DIR = os.path.join(os.path.dirname(__file__), 'uploaded_music')
UPLOAD_METADATA_FILE = os.path.join(UPLOAD_DIR, 'metadata.json')

# 确保音乐目录存在
for directory in (MUSIC_DIR, UPLOAD_DIR):
    if not os.path.exists(directory):
        os.makedirs(directory)

if not os.path.exists(UPLOAD_METADATA_FILE):
    with open(UPLOAD_METADATA_FILE, 'w', encoding='utf-8') as fp:
        json.dump([], fp, ensure_ascii=False)

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


def _load_uploaded_metadata():
    try:
        with open(UPLOAD_METADATA_FILE, 'r', encoding='utf-8') as fp:
            data = json.load(fp)
        return data if isinstance(data, list) else []
    except Exception:
        return []


def _save_uploaded_metadata(items):
    with open(UPLOAD_METADATA_FILE, 'w', encoding='utf-8') as fp:
        json.dump(items, fp, ensure_ascii=False, indent=2)


def _normalize_tags(tags):
    if tags is None:
        return []
    if isinstance(tags, str):
        source = tags.replace('，', ',').split(',')
    elif isinstance(tags, list):
        source = tags
    else:
        source = []

    normalized = []
    for item in source:
        tag = str(item).strip()
        if tag and tag not in normalized:
            normalized.append(tag)
    return normalized


def _safe_duration(duration):
    try:
        return max(0, int(float(duration)))
    except (TypeError, ValueError):
        return 0


def _normalize_uploaded_track(record):
    return {
        "id": record.get("id"),
        "title": record.get("title") or "未命名音乐",
        "artist": record.get("artist") or "",
        "duration": _safe_duration(record.get("duration")),
        "tags": _normalize_tags(record.get("tags")),
        "filename": record.get("filename"),
        "createdAt": record.get("createdAt"),
        "userId": record.get("userId"),
    }


def save_uploaded_music(file_storage, user_id=None, title=None, artist=None, duration=None, tags=None):
    """
    保存用户上传的 mp3 文件及其元数据
    """
    if file_storage is None or not getattr(file_storage, 'filename', ''):
        return None, "请选择要上传的 mp3 文件"

    original_filename = secure_filename(file_storage.filename)
    if not original_filename.lower().endswith('.mp3'):
        return None, "仅支持上传 mp3 文件"

    track_id = f"upload_{uuid.uuid4().hex}"
    saved_filename = f"{track_id}.mp3"
    target_path = os.path.join(UPLOAD_DIR, saved_filename)
    file_storage.save(target_path)

    normalized_tags = _normalize_tags(tags)
    metadata = _load_uploaded_metadata()
    metadata.append({
        "id": track_id,
        "filename": saved_filename,
        "title": (title or os.path.splitext(original_filename)[0]).strip() or "未命名音乐",
        "artist": (artist or "").strip(),
        "duration": _safe_duration(duration),
        "tags": normalized_tags,
        "createdAt": datetime.utcnow().isoformat() + "Z",
        "userId": str(user_id) if user_id not in (None, "") else None,
    })
    _save_uploaded_metadata(metadata)
    return _normalize_uploaded_track(metadata[-1]), None


def list_uploaded_music(user_id=None):
    """
    获取上传音乐列表；传入 user_id 时仅返回当前用户的上传
    """
    try:
        user_key = str(user_id) if user_id not in (None, "") else None
        items = _load_uploaded_metadata()
        if user_key is not None:
            items = [item for item in items if item.get("userId") == user_key]
        items = [item for item in items if os.path.exists(os.path.join(UPLOAD_DIR, item.get("filename", "")))]
        return [_normalize_uploaded_track(item) for item in sorted(items, key=lambda item: item.get("createdAt", ""), reverse=True)]
    except Exception:
        return []


def delete_uploaded_music(track_id, user_id=None):
    """
    删除上传的音乐文件及其元数据
    """
    try:
        user_key = str(user_id) if user_id not in (None, "") else None
        items = _load_uploaded_metadata()
        target = next((item for item in items if item.get("id") == track_id), None)
        if target is None:
            return False, "上传音乐不存在", 404
        if user_key is not None and target.get("userId") not in (None, user_key):
            return False, "无权删除该音乐", 403

        file_path = os.path.join(UPLOAD_DIR, target.get("filename", ""))
        if os.path.exists(file_path):
            os.remove(file_path)

        remaining = [item for item in items if item.get("id") != track_id]
        _save_uploaded_metadata(remaining)
        return True, None, 200
    except Exception as e:
        return False, str(e), 500

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


def get_music_file_by_name(filename):
    """
    根据文件名返回音乐文件路径
    """
    try:
        safe_name = os.path.basename(filename)
        if not safe_name.lower().endswith('.mp3'):
            return None, "仅支持 mp3 文件"

        search_paths = [
            os.path.join(MUSIC_DIR, safe_name),
            os.path.join(UPLOAD_DIR, safe_name),
        ]
        file_path = next((path for path in search_paths if os.path.exists(path)), None)
        if file_path is None:
            return None, "音乐文件未找到"

        return file_path, None
    except Exception as e:
        return None, str(e)
