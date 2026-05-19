#!/usr/bin/env python3
"""
将 backend/music 下的 Python 公共音乐批量注册到 music-service，并自动补齐情绪标签映射。

设计目标：
1. 幂等：同一首歌通过 source=python-public:<filename> 识别，重复执行不会重复创建。
2. 最小依赖：只使用 Python 标准库，适合本地直接执行。
3. 可回滚：支持 --dry-run 预览，不实际写入后端。
"""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional
from urllib import error, parse, request


EMOTION_LABELS = {
    "joy": "喜悦",
    "sadness": "悲伤",
    "anger": "愤怒",
    "fear": "恐惧",
    "love": "爱",
    "surprise": "惊喜",
    "neutral": "平静",
}

EMOTION_ALIASES = {
    "joy": "joy",
    "happy": "joy",
    "sad": "sadness",
    "sadness": "sadness",
    "anger": "anger",
    "angry": "anger",
    "fear": "fear",
    "anxiety": "fear",
    "love": "love",
    "surprise": "surprise",
    "neutral": "neutral",
    "calm": "neutral",
}

RESOURCE_SOURCE_PREFIX = "python-public:"
TAG_MAPPING_SOURCE = "migration:python-public"


class ApiError(RuntimeError):
    pass


@dataclass
class TrackSpec:
    filename: str
    emotion_key: str
    emotion_label: str
    title: str
    file_url: str
    source: str


class JsonApiClient:
    def __init__(self, base_url: str, default_headers: Optional[Dict[str, str]] = None, dry_run: bool = False):
        self.base_url = base_url.rstrip("/")
        self.default_headers = default_headers or {}
        self.dry_run = dry_run

    def _make_url(self, path: str) -> str:
        if path.startswith("http://") or path.startswith("https://"):
            return path
        return f"{self.base_url}/{path.lstrip('/')}"

    def request(self, method: str, path: str, payload: Optional[object] = None, headers: Optional[Dict[str, str]] = None):
        url = self._make_url(path)
        merged_headers = {"Accept": "application/json", **self.default_headers, **(headers or {})}
        body = None
        if payload is not None:
            body = json.dumps(payload).encode("utf-8")
            merged_headers["Content-Type"] = "application/json"

        if self.dry_run and method.upper() in {"POST", "PUT", "DELETE", "PATCH"}:
            print(f"[dry-run] {method.upper()} {url}")
            if payload is not None:
                print(json.dumps(payload, ensure_ascii=False, indent=2))
            return None

        req = request.Request(url=url, data=body, headers=merged_headers, method=method.upper())
        try:
            with request.urlopen(req, timeout=20) as response:
                raw = response.read().decode("utf-8")
                if not raw:
                    return None
                content_type = response.headers.get("Content-Type", "")
                if "application/json" in content_type:
                    return json.loads(raw)
                return raw
        except error.HTTPError as exc:
            raw = exc.read().decode("utf-8", errors="replace")
            raise ApiError(f"{method.upper()} {url} failed: HTTP {exc.code} {raw}") from exc
        except error.URLError as exc:
            raise ApiError(f"{method.upper()} {url} failed: {exc.reason}") from exc


def parse_args() -> argparse.Namespace:
    repo_root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description="迁移 Python 公共音乐到 music-service")
    parser.add_argument(
        "--music-dir",
        default=str(repo_root / "backend" / "music"),
        help="Python 公共音乐目录，默认 backend/music",
    )
    parser.add_argument(
        "--music-service-base",
        default="http://localhost:8081/api/v1",
        help="music-service 基础地址，默认 http://localhost:8081/api/v1",
    )
    parser.add_argument(
        "--python-base",
        default="http://localhost:5000",
        help="Python 音乐服务基础地址，默认 http://localhost:5000",
    )
    parser.add_argument(
        "--system-user-id",
        default="0",
        help="迁移时写入 music_resource.uploader_id 的系统用户 ID，默认 0",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="仅预览将执行的写操作，不实际提交",
    )
    return parser.parse_args()


def normalize_emotion_key(raw_value: str) -> Optional[str]:
    if not raw_value:
        return None
    lowered = str(raw_value).strip().lower()
    return EMOTION_ALIASES.get(lowered)


def infer_track_spec(filename: str, python_base: str) -> Optional[TrackSpec]:
    stem = Path(filename).stem
    parts = stem.split("_")
    emotion_key = normalize_emotion_key(parts[0] if parts else "")
    if not emotion_key:
        return None
    emotion_label = EMOTION_LABELS[emotion_key]
    suffix = parts[1] if len(parts) > 1 and parts[1].isdigit() else ""
    title = f"{emotion_label} {suffix}".strip()
    quoted_filename = parse.quote(filename)
    file_url = f"{python_base.rstrip('/')}/api/music/file/{quoted_filename}"
    return TrackSpec(
        filename=filename,
        emotion_key=emotion_key,
        emotion_label=emotion_label,
        title=title,
        file_url=file_url,
        source=f"{RESOURCE_SOURCE_PREFIX}{filename}",
    )


def list_public_music_files(music_dir: Path) -> List[str]:
    if not music_dir.exists():
        raise FileNotFoundError(f"音乐目录不存在: {music_dir}")
    return sorted(item.name for item in music_dir.iterdir() if item.is_file() and item.suffix.lower() == ".mp3")


def ensure_emotion_tags(api: JsonApiClient, labels: Iterable[str]) -> Dict[str, int]:
    existing = api.request("GET", "/emotion-tags") or []
    by_name = {item["tagName"]: int(item["id"]) for item in existing if item.get("tagName") and item.get("id") is not None}

    for label in labels:
        if label in by_name:
            continue
        created = api.request("POST", "/emotion-tags", {"tagName": label})
        if created and created.get("id") is not None:
            by_name[created["tagName"]] = int(created["id"])

    return by_name


def fetch_existing_resources(api: JsonApiClient) -> List[dict]:
    resources = api.request("GET", "/music-resources")
    return resources if isinstance(resources, list) else []


def find_existing_resource(resources: List[dict], track: TrackSpec) -> Optional[dict]:
    for resource in resources:
        if resource.get("source") == track.source:
            return resource
    for resource in resources:
        if resource.get("fileUrl") == track.file_url:
            return resource
    return None


def build_resource_payload(track: TrackSpec) -> dict:
    return {
        "title": track.title,
        "artist": track.emotion_label,
        "duration": 0,
        "fileUrl": track.file_url,
        "coverUrl": None,
        "source": track.source,
    }


def resource_needs_update(existing: dict, payload: dict) -> bool:
    comparable_keys = ("title", "artist", "duration", "fileUrl", "coverUrl", "source")
    for key in comparable_keys:
        if existing.get(key) != payload.get(key):
            return True
    return False


def upsert_resources(api: JsonApiClient, resources: List[dict], tracks: List[TrackSpec], system_user_id: str):
    created = 0
    updated = 0
    unchanged = 0
    results: Dict[str, int] = {}

    for track in tracks:
        payload = build_resource_payload(track)
        existing = find_existing_resource(resources, track)
        if existing is None:
            created_resource = api.request("POST", "/music-resources", payload, headers={"X-User-Id": system_user_id})
            if created_resource and created_resource.get("id") is not None:
                resources.append(created_resource)
                results[track.filename] = int(created_resource["id"])
            created += 1
            continue

        resource_id = existing.get("id")
        if resource_id is None:
            raise ApiError(f"已有资源缺少 id，无法更新: {existing}")
        results[track.filename] = int(resource_id)

        if resource_needs_update(existing, payload):
            updated_resource = api.request("PUT", f"/music-resources/{resource_id}", payload)
            if updated_resource:
                existing.update(updated_resource)
            else:
                existing.update(payload)
            updated += 1
        else:
            unchanged += 1

    return results, created, updated, unchanged


def chunked(items: List[dict], size: int) -> Iterable[List[dict]]:
    for start in range(0, len(items), size):
        yield items[start:start + size]


def apply_tag_mappings(api: JsonApiClient, tracks: List[TrackSpec], music_id_by_file: Dict[str, int], tag_id_by_name: Dict[str, int]):
    mapping_items = []
    for track in tracks:
        music_id = music_id_by_file.get(track.filename)
        tag_id = tag_id_by_name.get(track.emotion_label)
        if music_id is None or tag_id is None:
            continue
        mapping_items.append({
            "musicId": music_id,
            "tagIds": [tag_id],
            "source": TAG_MAPPING_SOURCE,
        })

    if not mapping_items:
        return 0

    applied = 0
    for batch in chunked(mapping_items, 100):
        api.request("POST", "/music-resources/tags/batch", {"items": batch})
        applied += len(batch)
    return applied


def main() -> int:
    args = parse_args()
    music_dir = Path(args.music_dir).resolve()
    api = JsonApiClient(args.music_service_base, dry_run=args.dry_run)

    try:
        filenames = list_public_music_files(music_dir)
        specs: List[TrackSpec] = []
        skipped_files: List[str] = []
        for filename in filenames:
            spec = infer_track_spec(filename, args.python_base)
            if spec is None:
                skipped_files.append(filename)
                continue
            specs.append(spec)

        if not specs:
            print("未发现可迁移的公共音乐文件。")
            return 0

        print(f"扫描到 {len(specs)} 首可迁移公共音乐。")
        if skipped_files:
            print("以下文件因无法识别情绪前缀被跳过：")
            for item in skipped_files:
                print(f"  - {item}")

        tag_id_by_name = ensure_emotion_tags(api, sorted({item.emotion_label for item in specs}))
        existing_resources = fetch_existing_resources(api)
        music_id_by_file, created, updated, unchanged = upsert_resources(
            api,
            existing_resources,
            specs,
            args.system_user_id,
        )
        mapped = apply_tag_mappings(api, specs, music_id_by_file, tag_id_by_name)

        print("迁移完成：")
        print(f"  - 创建资源: {created}")
        print(f"  - 更新资源: {updated}")
        print(f"  - 无变化资源: {unchanged}")
        print(f"  - 写入标签映射: {mapped}")
        if args.dry_run:
            print("当前为 dry-run，以上写操作均未真正提交。")
        return 0
    except (ApiError, FileNotFoundError) as exc:
        print(f"迁移失败: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
