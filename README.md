# 情感化冥想辅助系统

这是一个基于语音控制的情感化冥想辅助系统，能够根据用户语音输入的文字内容分析情感，并推荐相应的音乐以及相关冥想资源。

## 接口状态文档

- 当前仓库的接口接入/闲置状态清单见 `API_USAGE_STATUS.md`

## 项目特点

- 实时情感分析
- 智能音乐推荐
- 响应式用户界面
- 支持音乐播放控制（上一首/下一首）

## 技术栈

### 前端
- Vue 3
- Vite
- Vue Router
- Pinia (状态管理)

### 后端
- Python
- Flask
- 情感分析模型
- 音乐服务模块

## 项目结构

```
HCI_final/
├── frontend/                # 前端项目目录
│   ├── src/
│   │   ├── components/     # 可复用组件
│   │   │   ├── PlayerControls.vue    # 音乐播放器控制组件
│   │   │   ├── CircleTimer.vue       # 圆形计时器组件
│   │   │   ├── HomePage.vue          # 首页组件
│   │   │   ├── QuotesDisplay.vue     # 名言展示组件
│   │   │   └── icons/               # 图标组件目录
│   │   │
│   │   ├── views/         # 页面组件
│   │   │   ├── Home.vue             # 主页视图
│   │   │   └── Meditation.vue       # 冥想页面视图
│   │   │
│   │   ├── router/        # 路由配置
│   │   │   └── index.js             # 路由主配置文件
│   │   │
│   │   ├── stores/        # 状态管理
│   │   │   └── speech.js            # 语音相关状态管理
│   │   │
│   │   ├── utils/         # 工具函数
│   │   ├── assets/        # 静态资源
│   │   ├── App.vue        # 根组件
│   │   └── main.js        # 应用入口文件
│   │
│   ├── public/            # 公共静态资源
│   ├── index.html         # HTML 入口文件
│   ├── vite.config.js     # Vite 配置文件
│   └── package.json       # 项目依赖配置
│
└── backend/                # 后端项目目录
    ├── app.py             # Flask 应用主文件
    │   ├── /api/analyze   # 情感分析接口
    │   ├── /api/music     # 音乐相关接口
    │   └── /api/music/list # 音乐列表接口
    │
    ├── emotion_model.py   # 情感分析模型
    │   └── analyze_emotion() # 情感分析核心函数
    │
    ├── music_service.py   # 音乐服务
    │   ├── get_music_file()  # 获取音乐文件
    │   ├── get_music_list()  # 获取音乐列表
    │   ├── get_next_music()  # 获取下一首音乐
    │   └── get_prev_music()  # 获取上一首音乐
    │
    ├── music/             # 音乐文件目录
    │   ├── happy/        # 快乐情绪音乐
    │   ├── sad/          # 悲伤情绪音乐
    │   ├── calm/         # 平静情绪音乐
    │   └── excited/      # 兴奋情绪音乐
    │
    └── requirements.txt   # Python 依赖配置
```

## 功能模块

1. **情感分析模块**
   - 分析用户输入文本的情感倾向
   - 支持多种情感类型的识别

2. **音乐推荐模块**
   - 根据情感分析结果推荐相应音乐
   - 支持音乐播放控制
   - 提供音乐列表管理

3. **用户界面**
   - 文本输入区域
   - 情感分析结果显示
   - 音乐播放器控制
   - 响应式设计

## 运行指南

### 环境要求
- Node.js 16+
- Python 3.8+
- npm 或 yarn

### 前端运行步骤

1. 进入前端目录
```bash
cd frontend
```

2. 安装依赖
```bash
npm install
```

3. 启动开发服务器
```bash
npm run dev
```

### 后端运行步骤

1. 进入后端目录
```bash
cd backend
```

2. 安装 Python 依赖
```bash
pip install -r requirements.txt
```

3. 启动 Flask 服务器
```bash
python app.py
```

## 开发环境配置

推荐使用 VSCode 作为开发工具，并安装以下插件：
- Volar (Vue 3 支持)
- Python
- ESLint
- Prettier

## 注意事项

1. 确保后端服务器在 5000 端口运行
2. 前端开发服务器默认运行在 5173 端口
3. 确保 music 目录中包含相应的音乐文件

