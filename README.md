# Muses 部署指南

本指南介绍如何使用 Docker Compose 部署 Muses 音乐服务。

## 前置要求

- Docker
- Docker Compose
- FFmpeg (可选，用于音频转码)

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd muses
```

### 2. 配置环境变量

编辑 `docker-compose.yml`，修改以下环境变量：

- `JWT_SECRET`: 设置一个安全的随机字符串
- `CORS_ORIGIN`: 设置为你的 NAS IP 地址或域名
- 音乐路径: 将 `/path/to/your/music` 替换为你的音乐文件夹路径

### 3. 创建数据目录

```bash
mkdir -p data/database data/cache
```

### 4. 启动服务

```bash
docker-compose up -d
```

### 5. 验证部署

- 访问 http://localhost 查看 Web 界面
- API 端点: http://localhost/api

## 服务架构

```
┌─────────────┐      ┌─────────────┐
│    Web      │      │  Music API  │
│  (Nginx)    │──────│   (Node)    │
│   Port 80   │      │  Port 3000  │
└─────────────┘      └─────────────┘
```

## 常用命令

```bash
# 启动所有服务
docker-compose up -d

# 查看服务日志
docker-compose logs -f

# 停止所有服务
docker-compose down

# 重新构建镜像
docker-compose build --no-cache
```

## 故障排除

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs music-api
docker-compose logs web
```

### 数据库问题

如果需要重置数据库：

```bash
docker-compose down
rm -rf data/database/*
docker-compose up -d
```

## 目录结构

```
muses/
├── backend/           # 后端服务
│   ├── Dockerfile
│   └── ...
├── web/               # 前端 Web
│   ├── Dockerfile
│   ├── nginx.conf
│   └── ...
├── data/              # 数据目录
│   ├── database/      # SQLite 数据库
│   └── cache/         # 转码缓存
├── docker-compose.yml
└── nginx.conf
```

## 环境变量说明

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| NODE_ENV | 运行环境 | production |
| JWT_SECRET | JWT 密钥 | (需设置) |
| DATABASE_URL | 数据库路径 | file:./data/database.db |
| MUSIC_PATH | 音乐文件路径 | /music |
| TRANSCODE_CACHE_PATH | 转码缓存路径 | /app/cache |
| PORT | 服务端口 | 3000 |
| CORS_ORIGIN | CORS 允许的源 | http://your-nas-ip:80 |
