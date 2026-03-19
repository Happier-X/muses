# Muses

个人音乐流媒体平台，支持在移动端（Expo）和 Web 端管理和播放音乐。

## 项目结构

```
muses/
├── app/          # Expo 移动端应用
└── backend/      # Express REST API
```

## 快速开始

### 1. 后端

```bash
cd backend

# 安装依赖
npm install

# 配置环境变量
cp .env.example .env
# 编辑 .env，修改 JWT_SECRET 和音乐路径

# 初始化数据库
npx prisma db push

# 启动开发服务器
npm run dev
```

### 2. 移动端

```bash
cd app

# 安装依赖
npm install

# 启动 Expo
npx expo start
```

## 技术栈

### 移动端 (app/)

- **框架**: Expo SDK 55 + React Native 0.83
- **路由**: expo-router
- **UI**: HeroUI Native + Tailwind CSS (Uniwind)
- **状态**: Zustand
- **数据**: TanStack Query
- **音频**: expo-audio

### 后端 (backend/)

- **框架**: Express.js
- **ORM**: Prisma + SQLite
- **认证**: JWT + bcryptjs
- **验证**: Zod

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录 |
| GET | /api/artists | 歌手列表 |
| GET | /api/artists/:id | 歌手详情 |
| GET | /api/albums | 专辑列表 |
| GET | /api/albums/:id | 专辑详情 |
| GET | /api/songs | 歌曲列表 |
| GET | /api/songs/:id | 歌曲详情 |
| GET | /api/playlists | 播放列表 |
| POST | /api/playlists | 创建播放列表 |
| GET | /api/favorites | 收藏列表 |
| POST | /api/favorites/:songId | 收藏歌曲 |

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| DATABASE_URL | 数据库路径 | file:./data/database.db |
| JWT_SECRET | JWT 密钥 | - |
| PORT | 服务端口 | 3000 |
| MUSIC_PATH | 音乐文件目录 | /music |
| CORS_ORIGIN | 允许的跨域来源 | http://localhost:5173 |
