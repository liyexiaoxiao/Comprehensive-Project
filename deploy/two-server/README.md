# Two-Server Deployment

This directory contains a deployment layout for two machines:

- Server A: application server
- Server B: database server

## Server Allocation

Server A runs:

- `frontend` through Nginx on port `80`
- `api-gateway` on internal port `8080`
- `user-service` on internal port `8082`
- `music-service` on internal port `8081`
- `social-service` on internal port `8083`
- `data-service` on internal port `8084`
- `meditation-service` on internal port `8085`
- `python-backend` on internal port `5000`
- `ai-service` on internal port `5001`
- `rabbitmq` on internal port `5672`

Server B runs:

- MySQL on port `3306`
- MongoDB on port `27017`

The speech emotion recognition service is heavy. It is defined as an optional Docker Compose profile named `emotion-recog`; start it only when the server has enough memory and the Hugging Face model cache is prepared.

## Recommended Ports

Expose only these ports to users:

- Server A: `80` and optionally `443`

Restrict these ports to Server A's private IP or security group:

- Server B: `3306`, `27017`

Keep these local/private unless you are debugging through SSH tunnel:

- Server A: `8080`, `15672`

## Server B: Start Databases

Copy the db compose files to Server B, then create the environment file:

```bash
cd deploy/two-server/db-server
cp .env.db.example .env
```

Edit `.env`, then start:

```bash
docker compose -f docker-compose.db.yml --env-file .env up -d
```

The MySQL init script creates:

- `user_db`
- `emohealing_music_db`
- `emohealing_social_db`
- `meditation_db`

MongoDB databases are created on first write.

The application compose file overrides `user-service` with `SPRING_JPA_HIBERNATE_DDL_AUTO=update` because this repository does not include a MySQL table-creation script for `user_db`. The other Java services already use Hibernate `update` in their own config.

## Server A: Start Application Services

Copy the whole project to Server A, then create the environment file:

```bash
cd deploy/two-server/app-server
cp .env.app.example .env
```

Edit these values first:

- `DB_HOST`: Server B private IP
- `DB_PASSWORD`: MySQL root password from Server B
- `MONGO_URI`: MongoDB URI pointing to Server B
- `AI_MONGO_URI`: MongoDB URI pointing to Server B
- `JWT_SECRET`: same secret for gateway, user, and meditation services
- `DASHSCOPE_API_KEY`
- `SILICONFLOW_API_KEY`

Start the application stack:

```bash
docker compose -f docker-compose.app.yml --env-file .env up -d --build
```

Visit:

```text
http://SERVER_A_IP/
```

## Optional Emotion Recognition Service

The `emotion-recog` service loads local Hugging Face models and is memory-heavy. On a 4C4G machine, keep it disabled unless this feature is required.

Start it explicitly:

```bash
docker compose -f docker-compose.app.yml --env-file .env --profile emotion-recog up -d --build emotion-recog
```

The service expects model files in the `emotion_models` Docker volume at `/models/hf` because the code runs in offline mode.

## Useful Commands

Check running services:

```bash
docker compose -f docker-compose.app.yml --env-file .env ps
```

View logs:

```bash
docker compose -f docker-compose.app.yml --env-file .env logs -f api-gateway
docker compose -f docker-compose.app.yml --env-file .env logs -f user-service
docker compose -f docker-compose.app.yml --env-file .env logs -f frontend
```

Restart one service:

```bash
docker compose -f docker-compose.app.yml --env-file .env restart user-service
```

Rebuild after code changes:

```bash
docker compose -f docker-compose.app.yml --env-file .env up -d --build
```

## Notes For 4C4G Application Server

The compose file limits each Spring Boot service through `JAVA_TOOL_OPTIONS=-Xms128m -Xmx384m`. This is intentional for a 4 GB server. If the application server has 8 GB or more, increase this to `-Xmx512m` or `-Xmx768m`.

For a small demo, the recommended split is:

- Server A, 4C4G: application services, RabbitMQ, frontend
- Server B, 2C2G or 2C4G: MySQL and MongoDB

If memory is still tight, stop optional services first:

```bash
docker compose -f docker-compose.app.yml --env-file .env stop social-service data-service ai-service
```
