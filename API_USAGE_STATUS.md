# 接口使用状态清单

基于当前仓库代码静态扫描整理，口径如下：

- `已使用`：能在前端页面、store 或工具函数里找到实际调用。
- `间接使用`：没有显式 API 封装调用，但会被前端资源地址直接访问。
- `未接入`：后端已暴露，但仓库内未发现调用方。
- `已禁用`：接口仍存在，但当前实现明确返回不可用结果。
- `管理端未接入`：仅管理员接口，当前仓库内无管理端页面或脚本调用。
- `已移除`：已从当前代码清理下线。

## user-service

| 接口 | 状态 | 调用位置 / 备注 |
| --- | --- | --- |
| `POST /api/users/register` | 已使用 | `LoginPage.vue` |
| `POST /api/users/login` | 已使用 | `LoginPage.vue` |
| `POST /api/users/logout` | 已使用 | `ServicePage.vue`、`AdminDashboard.vue` |
| `GET /api/users/me` | 已使用 | `LoginPage.vue`、`PersonalSpace.vue` |
| `POST /api/users/me/avatar` | 已使用 | `PersonalSpace.vue` |
| `GET /api/users/avatars/{filename}` | 间接使用 | 头像 `avatarUrl` 作为资源地址直接访问 |
| `GET /api/users/search` | 已使用 | `PersonalSpace.vue` |
| `GET /api/users/summaries` | 已使用 | `PersonalSpace.vue` |
| `PATCH /api/users/update-info` | 已使用 | `PersonalSpace.vue` |
| `DELETE /api/users/delete-me` | 未接入 | 无前端调用 |
| `POST /api/users/change-password` | 未接入 | 无前端调用 |
| `DELETE /api/users/{id}` | 已使用 | `AdminDashboard.vue` 删除用户 |
| `PATCH /api/users/{id}` | 已使用 | `AdminDashboard.vue` 编辑用户资料 / 角色 / 状态 |
| `POST /api/users/{id}/reset-password` | 已使用 | `AdminDashboard.vue` 重置用户密码 |
| `GET /api/users/get-users` | 已使用 | `AdminDashboard.vue` 分页获取用户列表 |
| `GET /api/users/{id}` | 已使用 | `AdminDashboard.vue` 获取用户详情 |

## social-service

| 接口 | 状态 | 调用位置 / 备注 |
| --- | --- | --- |
| `GET /api/social/v1/me/mood-diaries` | 已使用 | `PersonalSpace.vue` |
| `GET /api/social/v1/me/mood-diaries/{diaryId}` | 未接入 | 无前端调用 |
| `POST /api/social/v1/me/mood-diaries` | 已使用 | `PersonalSpace.vue` |
| `PUT /api/social/v1/me/mood-diaries/{diaryId}` | 已使用 | `PersonalSpace.vue`，仅允许修改“今日日记” |
| `DELETE /api/social/v1/me/mood-diaries/{diaryId}` | 已使用 | `PersonalSpace.vue`，允许删除任意日期日记 |
| `GET /api/social/v1/posts` | 已使用 | `PersonalSpace.vue` |
| `GET /api/social/v1/posts/{postId}` | 未接入 | 无前端调用 |
| `GET /api/social/v1/me/posts` | 已使用 | `PersonalSpace.vue` |
| `POST /api/social/v1/me/posts` | 已使用 | `PersonalSpace.vue` |
| `PUT /api/social/v1/me/posts/{postId}` | 未接入 | 前端保留封装但无实际调用 |
| `DELETE /api/social/v1/me/posts/{postId}` | 已使用 | `PersonalSpace.vue` |
| `GET /api/social/v1/posts/{postId}/interactions` | 已使用 | `PersonalSpace.vue` |
| `POST /api/social/v1/posts/{postId}/like` | 已使用 | `PersonalSpace.vue` |
| `POST /api/social/v1/posts/{postId}/comments` | 已使用 | `PersonalSpace.vue` |
| `POST /api/social/v1/posts/{postId}/comments/{commentId}/like` | 已使用 | `PersonalSpace.vue` |
| `POST /api/social/v1/posts/{postId}/comments/{commentId}/replies` | 已使用 | `PersonalSpace.vue` |
| `GET /api/social/v1/me/social-notifications` | 已使用 | `PersonalSpace.vue` |
| `DELETE /api/social/v1/me/interactions/{interactionId}` | 未接入 | 前端保留封装但无实际调用 |
| `POST /api/social/v1/me/friend-requests` | 已使用 | `PersonalSpace.vue` |
| `GET /api/social/v1/me/friend-requests/received` | 已使用 | `PersonalSpace.vue` |
| `PUT /api/social/v1/me/friend-requests/{requestId}` | 已使用 | `PersonalSpace.vue` |
| `GET /api/social/v1/me/friends` | 已使用 | `PersonalSpace.vue` |
| `PUT /api/social/v1/me/friends/{friendshipId}/intimacy` | 未接入 | 前端保留封装但无实际调用 |
| `DELETE /api/social/v1/me/friends/{friendshipId}` | 已使用 | `PersonalSpace.vue` |
| `GET /api/social/v1/me/chat/conversations` | 已使用 | `PersonalSpace.vue` 聊天室子页 |
| `GET /api/social/v1/me/chat/with/{peerUserId}/messages` | 已使用 | `PersonalSpace.vue` 聊天室子页 |
| `POST /api/social/v1/me/chat/with/{peerUserId}/messages` | 已使用 | `PersonalSpace.vue` 聊天室子页 |
| `PUT /api/social/v1/me/chat/with/{peerUserId}/read` | 已使用 | `PersonalSpace.vue` 聊天室子页 |

## data-service

| 接口 | 状态 | 调用位置 / 备注 |
| --- | --- | --- |
| `POST /api/data/v1/logs/ai-chat` | 未接入 | 前端仅保留封装 |
| `POST /api/data/v1/logs/emotion-snapshot` | 未接入 | 前端仅保留封装 |
| `POST /api/data/v1/logs/user-behavior` | 已使用 | `PersonalSpace.vue`、`MusicPlayerPage.vue` |
| `GET /api/data/v1/me/ai-chat-logs` | 未接入 | 前端仅保留封装 |
| `GET /api/data/v1/me/emotion-snapshots` | 已使用 | `PersonalSpace.vue` |
| `GET /api/data/v1/me/user-behavior-logs` | 未接入 | 前端仅保留封装 |

## meditation-service

| 接口 | 状态 | 调用位置 / 备注 |
| --- | --- | --- |
| `GET /api/meditation/test` | 未接入 | 调试接口 |
| `GET /api/meditation/my-meditation-logs` | 已使用 | `Meditation.vue`、`PersonalSpace.vue` |
| `POST /api/meditation/my-meditation-logs` | 已使用 | `Meditation.vue` |
| `POST /api/meditation/my-meditation-logs/delete` | 未接入 | 前端保留封装但未使用 |
| `POST /api/meditation/start-countdown` | 已使用 | `Meditation.vue` 开始/继续倒计时 |
| `POST /api/meditation/stop-countdown` | 已使用 | `Meditation.vue` 暂停/重置倒计时 |
| `POST /api/meditation/complete-countdown` | 已使用 | `Meditation.vue` 倒计时完成后通知后端归档 |
| `GET /api/meditation/countdown-left` | 未接入 | 无前端调用 |
| `GET /api/meditation/garden/me` | 已使用 | `PersonalSpace.vue` |
| `POST /api/meditation/garden/me/reward` | 已使用 | `Meditation.vue` |
| `POST /api/meditation/garden/me/unlock-plant/{plantId}` | 未接入 | 前端保留封装但未使用 |
| `GET /api/meditation/admin/meditation-logs/{user_id}` | 已使用 | `AdminDashboard.vue` 查看指定用户冥想日志 |
| `POST /api/meditation/admin/meditation-logs/{user_id}` | 已使用 | `AdminDashboard.vue` 为指定用户新增冥想日志 |
| `POST /api/meditation/admin/meditation-logs/delete` | 已使用 | `AdminDashboard.vue` 删除指定冥想日志 |

## music-service


### `MusicApiController` 曲库 / 标签 / 推荐 / 新歌单接口

| 接口 | 状态 | 调用位置 / 备注 |
| --- | --- | --- |
| `GET /api/music/v1/music-resources` | 已使用 | `musicStore.js` 用于公共曲库列表，`AdminDashboard.vue` / `AllPlaylistsPage.vue` 用于官方歌单曲目聚合展示 |
| `GET /api/music/v1/me/music-resources` | 已使用 | `musicStore.js` 用于同步当前用户上传音乐的资源元数据 |
| `GET /api/music/v1/music-resources/by-ids` | 未接入 | 无前端调用 |
| `GET /api/music/v1/music-resources/{id}` | 未接入 | 无前端调用 |
| `GET /api/music/v1/music-resources/{id}/tags` | 已使用 | `musicStore.js` 拉取公共曲库与推荐曲目的标签 |
| `DELETE /api/music/v1/music-resources/{musicId}/tags/{mappingId}` | 未接入 | 无前端调用 |
| `GET /api/music/v1/emotion-tags` | 已使用 | `musicStore.js` 解析推荐所需的情绪标签 ID，`AdminDashboard.vue` 维护官方歌单曲目标签时刷新标签字典 |
| `GET /api/music/v1/emotion-tags/by-name` | 未接入 | 无前端调用 |
| `GET /api/music/v1/me/music-preferences` | 已使用 | `musicStore.js` |
| `POST /api/music/v1/me/music-preferences` | 已使用 | `musicStore.js` |
| `DELETE /api/music/v1/me/music-preferences/{musicId}/{preferenceType}` | 已使用 | `musicStore.js` |
| `GET /api/music/v1/me/music-preferences/by-music/{musicId}` | 已使用 | `musicStore.js` / `MusicPlayerPage.vue` 切歌后同步当前单曲偏好状态 |
| `POST /api/music/v1/me/music-recommendations/by-emotion` | 已使用 | `musicStore.js`，服务页推荐列表 |
| `POST /api/music/v1/me/music-recommendations/next` | 已使用 | `musicStore.js`，播放器推荐下一首 |
| `POST /api/music/v1/music-resources` | 已使用 | `musicStore.js` 用户上传后同步注册资源，`AdminDashboard.vue` 官方歌单上传后同步注册资源 |
| `PUT /api/music/v1/music-resources/{musicId}` | 已使用 | `AdminDashboard.vue` 编辑官方歌单曲目元数据 |
| `DELETE /api/music/v1/music-resources/{musicId}` | 已使用 | `musicStore.js`、`AdminDashboard.vue` 删除资源元数据 |
| `POST /api/music/v1/music-resources/{musicId}/tags` | 已使用 | `musicStore.js` 用户上传音乐后写入确认过的情绪标签；`AdminDashboard.vue` 维护官方歌单曲目标签映射 |
| `POST /api/music/v1/music-resources/{musicId}/tags/ai` | 未接入 | 用户上传流程已改为上传前预识别，不再在落库后自动调用 |
| `POST /api/music/v1/music-resources/tags/ai-preview` | 已使用 | `PersonalSpace.vue` 上传弹窗中先对待上传音频做 AI 预识别并回填标签 |
| `POST /api/music/v1/music-resources/tags/batch` | 未接入 | 无前端调用 |
| `POST /api/music/v1/emotion-tags` | 已使用 | `AdminDashboard.vue` 为官方歌单上传 / 编辑时补齐缺失情绪标签 |
| `PUT /api/music/v1/emotion-tags/{tagId}` | 未接入 | 无前端调用 |
| `DELETE /api/music/v1/emotion-tags/{tagId}` | 未接入 | 无前端调用 |
| `GET /api/music/v1/official-playlists` | 已使用 | `AllPlaylistsPage.vue` 渲染官方歌单配置，`AdminDashboard.vue` 读取并回填管理表单 |
| `PUT /api/music/v1/official-playlists/{playlistKey}` | 已使用 | `AdminDashboard.vue` 持久化官方歌单名称 / 简介 / 封面情绪等配置 |
| `GET /api/music/v1/me/playlists` | 已使用 | `musicStore.js` |
| `GET /api/music/v1/me/playlists/{playlistId}` | 未接入 | 前端当前列表页无需单独详情接口 |
| `POST /api/music/v1/me/playlists` | 已使用 | `musicStore.js` |
| `PUT /api/music/v1/me/playlists/{playlistId}` | 未接入 | 无前端调用 |
| `DELETE /api/music/v1/me/playlists/{playlistId}` | 已使用 | `musicStore.js` |
| `POST /api/music/v1/me/playlists/{playlistId}/tracks` | 已使用 | `musicStore.js` |
| `DELETE /api/music/v1/me/playlists/{playlistId}/tracks/{musicId}` | 已使用 | `musicStore.js` |

## AI-service

| 接口 | 状态 | 调用位置 / 备注 |
| --- | --- | --- |
| `POST /api/meditation/guide` | 已使用 | `Meditation.vue` 生成情绪冥想引导文案 |
| `POST /api/companion/audio/analyze` | 已使用 | `ServicePage.vue` 语音陪伴场景下做转写与情绪分析 |
| `POST /api/companion/chat` | 已使用 | `ServicePage.vue` 陪伴对话主入口 |
| `POST /api/companion/tts` | 已使用 | `Meditation.vue` 冥想引导逐句 TTS 播报 |
| `GET /api/companion/history` | 未接入 | 前端暂无会话历史回放调用 |

## Python backend

这些接口不经过网关，前端通过 `VITE_PYTHON_BASE_URL` 直接访问。

| 接口 | 状态 | 调用位置 / 备注 |
| --- | --- | --- |
| `POST /api/analyze` | 已使用 | `Home.vue`、`ServicePage.vue` |
| `GET /api/music/{emotion}` | 已使用 | `MusicPlayerPage.vue` |
| `GET /api/music/{emotion}/next` | 已使用 | `MusicPlayerPage.vue` |
| `GET /api/music/{emotion}/prev` | 已使用 | `MusicPlayerPage.vue` |
| `GET /api/music/list` | 已使用 | `ServicePage.vue`、`PersonalSpace.vue` |
| `GET /api/music/file/{filename}` | 已使用 | `ServicePage.vue`、`MusicPlayerPage.vue`，以及 `getMusicFileUrl()` 资源直链 |
| `GET /api/music/uploads` | 已使用 | `musicStore.js` |
| `POST /api/music/uploads` | 已使用 | `musicStore.js` 用户上传音乐文件，`AdminDashboard.vue` 上传官方歌单音频文件 |
| `DELETE /api/music/uploads/{trackId}` | 已使用 | `musicStore.js` 删除用户上传源文件，`AdminDashboard.vue` 删除管理员上传的官方曲目源文件 |
