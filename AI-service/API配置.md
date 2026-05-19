# AI-service API Key 获取与配置说明

AI-service 后端目前需要配置两类 API Key：

- `DASHSCOPE_API_KEY`：用于 `backend/qwen_service.py` 和 `backend/kimi_service.py`，调用阿里云百炼 / DashScope 的 OpenAI 兼容接口。
- `SILICONFLOW_API_KEY`：用于 `backend/tts_service.py`，调用 SiliconFlow 的语音合成接口。

请不要把真实 API Key 提交到 Git 仓库。

## 1. 获取 DASHSCOPE_API_KEY

`DASHSCOPE_API_KEY` 来自阿里云百炼（Model Studio / DashScope）。

获取步骤：

1. 打开阿里云百炼控制台或官方文档入口：<https://help.aliyun.com/zh/model-studio/get-api-key>
2. 登录阿里云账号。
3. 开通百炼 / DashScope 模型服务。
4. 进入 API Key 管理页面。
5. 新建或复制一个 API Key。
6. 将复制到的 Key 配置为环境变量 `DASHSCOPE_API_KEY`。

本项目中，Qwen 伴侣聊天和 Kimi 冥想引导都读取这个变量：

```python
os.getenv("DASHSCOPE_API_KEY")
```

## 2. 获取 SILICONFLOW_API_KEY

`SILICONFLOW_API_KEY` 来自 SiliconFlow（硅基流动），当前用于 TTS 语音合成。

获取步骤：

1. 打开 SiliconFlow API Key 页面：<https://cloud.siliconflow.cn/account/ak>
2. 注册或登录 SiliconFlow 账号。
3. 进入 API 密钥页面。
4. 点击创建 API 密钥。
5. 复制生成的 Key。
6. 将复制到的 Key 配置为环境变量 `SILICONFLOW_API_KEY`。

本项目中，TTS 服务读取这个变量：

```python
os.getenv("SILICONFLOW_API_KEY")
```

## 3. 在本项目中配置

推荐在 `AI-service/backend/.env` 中配置：

```env
DASHSCOPE_API_KEY=你的阿里云百炼APIKey
SILICONFLOW_API_KEY=你的SiliconFlowAPIKey

# 可选：不配置时会使用默认值
SILICONFLOW_TTS_MODEL=FunAudioLLM/CosyVoice2-0.5B
SILICONFLOW_TTS_VOICE=FunAudioLLM/CosyVoice2-0.5B:alex
```

然后在 `AI-service/backend` 目录启动服务：

```powershell
cd AI-service/backend
python app.py
```

如果你不想写 `.env`，也可以在 PowerShell 当前窗口临时设置：

```powershell
$env:DASHSCOPE_API_KEY="你的阿里云百炼APIKey"
$env:SILICONFLOW_API_KEY="你的SiliconFlowAPIKey"
python app.py
```

这种方式只对当前 PowerShell 窗口有效，关闭窗口后需要重新设置。

## 4. 常见问题

### 提示未配置 DASHSCOPE_API_KEY

检查：

- `.env` 文件是否放在 `AI-service/backend/.env`
- 变量名是否写成了 `DASHSCOPE_API_KEY`
- 启动服务前是否安装了 `python-dotenv`
- 是否在 `AI-service/backend` 目录启动后端

### 提示 SILICONFLOW_API_KEY is not configured

检查：

- `.env` 文件中是否写了 `SILICONFLOW_API_KEY`
- SiliconFlow 控制台里的 Key 是否复制完整
- Key 前后是否多了空格或引号

### API 调用失败或无权限

检查：

- 对应平台账号是否已完成服务开通
- 账号余额或免费额度是否可用
- 选择的模型是否在当前账号 / 地域可用
- Key 是否被删除、禁用或泄露后重置

