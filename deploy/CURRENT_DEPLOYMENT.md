# Current Manual Deployment

This repository currently uses a two-machine manual deployment, but not the old
`deploy/two-server` layout. The active layout is:

- `deploy/one-server` for the main application server.
- `deploy/emotion-server` for the speech emotion recognition server.

## Main Server

- Public IP: `106.53.206.106`
- Project path: `~/Comprehensive-Project`
- Compose path: `deploy/one-server/docker-compose.no-ai.yml`
- Env file: `deploy/one-server/.env`

Services on the main server:

- `frontend`
- `api-gateway`
- `user-service`
- `music-service`
- `social-service`
- `meditation-service`
- `rabbitmq`
- `python-backend`
- `ai-service`
- `mongodb`
- `data-service`
- MySQL runs on the host, outside Docker.

Important files:

- `deploy/one-server/.env`
- `deploy/one-server/docker-compose.no-ai.yml`
- `deploy/one-server/nginx.conf`
- `backend/Dockerfile.deploy`
- `backend/requirements.deploy.txt`
- `AI-service/backend/Dockerfile.deploy`
- `AI-service/backend/requirements.deploy.txt`

The old `deploy/two-server` files are intentionally not part of the current
deployment path.

Do not run `docker-compose down -v` unless you intentionally want to delete Docker volumes.

Main server redeploy:

```bash
cd ~/Comprehensive-Project/deploy/one-server
sudo docker-compose -f docker-compose.no-ai.yml --env-file .env up -d --build
```

Force rebuild frontend after frontend changes:

```bash
cd ~/Comprehensive-Project/deploy/one-server
sudo docker-compose -f docker-compose.no-ai.yml --env-file .env build --no-cache frontend
sudo docker-compose -f docker-compose.no-ai.yml --env-file .env rm -sf frontend
sudo docker-compose -f docker-compose.no-ai.yml --env-file .env up -d frontend
```

## Emotion Recognition Server

- Public IP: `154.8.193.119`
- Project path: `~/Comprehensive-Project`
- Compose path: `deploy/emotion-server/docker-compose.emotion.yml`
- Hugging Face cache path: `~/hf_cache`

Services on the emotion server:

- `emotion-recog`

Required model cache directories under `~/hf_cache/hub`:

- `models--prithivMLmods--Speech-Emotion-Classification`
- `models--openai--whisper-tiny`
- `models--j-hartmann--emotion-english-distilroberta-base`
- `models--Helsinki-NLP--opus-mt-zh-en`

Emotion server redeploy:

```bash
cd ~/Comprehensive-Project/deploy/emotion-server
docker compose -f docker-compose.emotion.yml up -d --build
```

Check logs:

```bash
docker logs -f emohealing-emotion-recog
```

The main server `ai-service` must point to:

```env
EMOTION_SERVICE_URL=http://154.8.193.119:5003/predict
```

## Current URL Rules

Nginx on the main server routes:

- `/api/` -> `api-gateway:8080`
- `/py-api/` -> `python-backend:5000`
- `/ai-api/` -> `ai-service:5001`

Music resource `file_url` values in MySQL should use:

```text
/py-api/api/music/file/<filename>
```

Do not store:

```text
http://localhost:5000/...
```

Local frontend development must configure Vite proxy for `/py-api` if it uses the shared database.
