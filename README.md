# EmotionHealing — 情感化冥想辅助系统

基于多模态情感计算的智能冥想助手，融合语音情绪识别、大语言模型对话与个性化音乐推荐，为用户提供从情绪感知到冥想疗愈的全链路体验。

## 接口状态文档

- 当前仓库的接口接入/闲置状态清单见 [`API_USAGE_STATUS.md`](API_USAGE_STATUS.md)

## 核心功能

### 🎯 多模态情感分析
- **文本情绪识别** — 基于 DistilRoBERTa 的 7 类基础情绪分类，支持中英文自动检测与翻译
- **语音情绪识别** — 双通道融合架构（声学特征 + 文本语义），支持 VA 情感模型与复合情绪推理
- **情绪可视化** — ECharts 驱动的个人情绪趋势图表与历史快照

### 🤖 AI 情感陪伴
- **多模态对话** — Qwen3.5-Plus 驱动的情感陪伴聊天，支持音乐推荐 Tool Calling
- **冥想引导生成** — Kimi K2.5 根据 10 种情绪状态生成个性化冥想指导
- **AI 社交互动** — 自动生成社交动态回复，增强社区参与感
- **语音合成 (TTS)** — CosyVoice2 将 AI 回复转为自然语音（8 种音色可选）

### 🎵 智能音乐推荐
- 基于情感标签的音乐库管理与自动推荐
- Qwen3-Omni-Captioner 自动分析音频情感标签
- 用户偏好追踪（喜欢 / 收藏 / 屏蔽）
- 个人歌单与官方歌单系统

### 🧘 冥想与正念
- 冥想会话管理（开始 / 计时 / 结束 / 记录）
- 圆形计时器可视化组件
- 迷你任务系统 — 每日正念小目标
- 花园系统 — 解锁植物奖励

### 👥 社交空间
- 心情日记 — 记录每日情绪状态
- 动态广场 — 发布心情动态（支持匿名）
- 好友系统 — 好友请求 / 亲密度评分
- 内容审核 — 敏感词过滤

### 🛠 后台管理
- 用户管理与角色控制
- 用户反馈收集
- 官方歌单配置
- 敏感词词库管理

## 技术栈

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.x | 前端框架（Composition API + `<script setup>`） |
| Vite | 6.x | 构建工具 |
| Vue Router | 4.x | 客户端路由 |
| Pinia | 3.x | 状态管理 |
| Element Plus | 2.x | UI 组件库 |
| Axios | - | HTTP 请求 |
| ECharts | 6.x | 情绪数据可视化 |
| Web Speech API | - | 浏览器语音识别 |

### 后端 — Java 微服务
| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3 / 4.0 | 微服务框架 |
| Spring Cloud Gateway | 2023.0.3 | API 网关 |
| Spring Data JPA | - | MySQL ORM |
| Spring Data MongoDB | - | MongoDB 操作 |
| Spring Security | - | 认证鉴权 |
| Spring AMQP | - | RabbitMQ 集成 |
| SpringDoc OpenAPI | - | Swagger 文档 |
| Bucket4j | - | 接口限流 |
| JJWT | - | JWT 令牌 |
| Lombok | - | 代码简化 |

### 后端 — Python 服务
| 技术 | 版本 | 用途 |
|------|------|------|
| Flask | 3.x | Web 框架 |
| Transformers / PyTorch | - | Hugging Face 模型推理 |
| langdetect | - | 语种检测 |
| deep-translator | - | 中文 → 英文翻译 |
| PyJWT | - | JWT 处理 |
| PyMongo | - | MongoDB 客户端 |
| OpenAI SDK | - | DashScope API 调用 |

### AI / ML 模型
| 模型 | 用途 |
|------|------|
| `j-hartmann/emotion-english-distilroberta-base` | 文本情绪分类（7 类） |
| `prithivMLmods/Speech-Emotion-Classification` | 语音声学情绪分类 |
| OpenAI Whisper-tiny | 语音转文字 |
| `Helsinki-NLP/opus-mt-zh-en` | 中文 → 英文翻译 |
| Qwen3.5-Plus (DashScope) | 情感陪伴对话 + Tool Calling |
| Kimi K2.5 (DashScope) | 冥想引导生成 |
| Qwen3-Omni-Captioner (DashScope) | 音频情感自动标签 |
| CosyVoice2-0.5B (SiliconFlow) | 文本转语音 (TTS) |

### 基础设施
| 技术 | 用途 |
|------|------|
| Docker / Docker Compose | 容器化部署 |
| Nginx | 反向代理 + 静态资源 |
| MySQL | 关系型数据库（5 个库） |
| MongoDB 7 | 日志与文档存储 |
| RabbitMQ | 异步消息队列 |

## 微服务一览

| 服务 | 端口 | 技术栈 | 数据库 | 核心职责 |
|------|------|--------|--------|----------|
| **api-gateway** | 8080 | Spring Cloud Gateway | - | 统一入口、JWT 认证、路由转发 |
| **user-service** | 8082 | Spring Boot | MySQL `user_db` | 用户注册登录、权限、反馈 |
| **music-service** | 8081 | Spring Boot | MySQL `music_db` | 音乐管理、情感标签、推荐 |
| **social-service** | 8083 | Spring Boot | MySQL `social_db` | 社交动态、好友、聊天 |
| **meditation-service** | 8085 | Spring Boot | MySQL `meditation_db` | 冥想会话、任务、花园 |
| **data-service** | 8084 | Spring Boot | MongoDB `log_db` | 日志采集与查询 |
| **backend** | 5000 | Python Flask | - | 文本情绪分析、音乐文件服务 |
| **ai-service** | 5001 | Python Flask | MongoDB `ai_chat_log` | LLM 对话、TTS、冥想引导 |
| **emotion_recog** | 5003 | Python Flask | - | 语音情绪双通道识别 |

## 数据库设计

### MySQL（5 个数据库）

| 数据库 | 核心表 |
|--------|--------|
| `emohealing_user_db` | user_info, user_feedback, operation_log |
| `emohealing_music_db` | music_resource, emotion_tag, music_tag_mapping, user_preference, playlist, playlist_track, official_playlist_config |
| `emohealing_social_db` | mood_diary, social_post, social_interaction, friendship, friend_request, censor_word, chat_conversation, chat_message |
| `emohealing_meditation_db` | meditation_session, meditation_log, mini_mission, mini_mission_log, garden_inventory, unlocked_plant |

### MongoDB（2 个数据库）

| 数据库 | 集合 |
|--------|------|
| `emohealing_log_db` | ai_chat_log, emotion_snapshot, user_behavior_log |
| `comprehensive_project` | ai_chat_log |

## API 路由总览

所有 API 通过 Nginx 统一入口（生产）或 Vite Proxy（开发）：

| 路径前缀 | 目标服务 | 说明 |
|----------|----------|------|
| `/api/users/**` | user-service:8082 | 用户注册/登录/个人资料 |
| `/api/admin/**` | user-service:8082 | 管理功能 |
| `/api/feedback/**` | user-service:8082 | 用户反馈 |
| `/api/music/**` | music-service:8081 | 音乐管理与推荐 |
| `/api/social/**` | social-service:8083 | 社交功能 |
| `/api/data/**` | data-service:8084 | 日志数据 |
| `/api/meditation/**` | meditation-service:8085 | 冥想与任务 |
| `/py-api/*` | backend:5000 | 文本情绪分析 / 音乐文件 |
| `/ai-api/*` | ai-service:5001 | AI 对话 / TTS / 冥想引导 |
| `/predict` | emotion_recog:5003 | 语音情绪识别 |

## 快速开始

### 环境要求

| 组件 | 版本要求 |
|------|----------|
| Node.js | 16+ |
| Python | 3.8+ |
| Java | 17+ |
| Docker & Docker Compose | 最新版 |
| MySQL | 8.0+ |
| MongoDB | 7.0+ |

### 开发环境启动（Docker Compose）

```bash
# 1. 克隆项目
git clone <repo-url>
cd Comprehensive-Project

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 填入必要的 API Key 和配置

# 3. 启动基础设施 + 微服务
docker-compose up -d

# 4. 启动 Python 后端（独立终端）
cd backend
pip install -r requirements.txt
python app.py

# 5. 启动 AI 服务（独立终端）
cd AI-service/backend
pip install -r requirements.txt
python app.py

# 6. 启动前端开发服务器
cd frontend
npm install
npm run dev
```

### 生产环境部署（双服务器方案）

- **主服务器** — 前端 + API 网关 + 所有 Java 服务 + Python 服务 + RabbitMQ + MongoDB + Nginx
- **GPU 服务器** — emotion_recog 语音情绪识别服务

```bash
# 主服务器
cd deploy/one-server
docker-compose -f docker-compose.no-ai.yml --env-file .env up -d --build
```

## 开发环境

推荐使用 VSCode 并安装以下插件：

- **Volar** — Vue 3 语言支持
- **Python** — Python 支持
- **Extension Pack for Java** — Spring Boot 支持
- **ESLint** / **Prettier** — 代码规范
- **Docker** — 容器管理

### 端口分配

| 端口 | 服务 |
|------|------|
| 5173 | 前端开发服务器 (Vite) |
| 8080 | API 网关 |
| 8081 | music-service |
| 8082 | user-service |
| 8083 | social-service |
| 8084 | data-service |
| 8085 | meditation-service |
| 5000 | Python Flask 后端 |
| 5001 | AI 核心服务 |
| 5003 | 语音情绪识别服务 |
| 5672 | RabbitMQ |
| 27017 | MongoDB |
| 3306 | MySQL |