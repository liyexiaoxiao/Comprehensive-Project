# Current Deployment Files

当前线上部署只使用下面这些文件：

- `one-server/docker-compose.no-ai.yml`: 主服务器 compose。虽然文件名还叫 `no-ai`，但现在已经包含 `ai-service`、`mongodb`、`data-service`。
- `one-server/nginx.conf`: 前端 Nginx 反向代理，负责 `/api/`、`/py-api/`、`/ai-api/`。
- `one-server/.env.example`: 主服务器 `.env` 模板，不要提交真实密钥。
- `emotion-server/docker-compose.emotion.yml`: 情绪识别服务器 compose，挂载 `~/hf_cache`。
- `CURRENT_DEPLOYMENT.md`: 当前部署结构、更新命令和注意事项。

当前不再使用旧的 `deploy/two-server` 方案。主服务更新时在主服务器运行：

```bash
cd ~/Comprehensive-Project/deploy/one-server
sudo docker-compose -f docker-compose.no-ai.yml --env-file .env up -d --build
```

情绪识别服务一般不随主项目更新；需要单独更新时在情绪识别服务器运行：

```bash
cd ~/Comprehensive-Project/deploy/emotion-server
docker compose -f docker-compose.emotion.yml up -d --build
```
