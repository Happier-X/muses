# Muses 音乐流媒体平台

部署在 NAS 上的个人音乐流媒体服务。

## 快速开始

```bash
# 1. 克隆项目
git clone https://github.com/Happier-X/muses.git
cd muses

# 2. 复制配置
cp .env.example .env

# 3. 编辑 .env，修改 JWT_SECRET

# 4. 启动服务
docker-compose up -d
```

## 配置

编辑 `docker-compose.yml` 修改音乐路径：

```yaml
volumes:
  - /你的音乐文件夹:/music:ro
```

## 访问

- **地址**: `http://NAS_IP:3000`

## 命令

```bash
# 启动
docker-compose up -d

# 停止
docker-compose down

# 查看日志
docker-compose logs -f
```

## 环境变量

| 变量 | 说明 |
|------|------|
| JWT_SECRET | JWT 密钥（必填） |
| MUSIC_PATH | 音乐文件路径 (/music) |
| PORT | 服务端口 (3000) |
