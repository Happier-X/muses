# Muses

个人音乐流媒体平台，移动端（Expo）和后端（Elysia）。

## 项目结构

```
muses/
├── app/       # Expo 移动端应用
└── backend/   # Elysia REST API
```

## 快速开始

### 后端

```bash
cd backend

# 安装依赖
npm install

# 配置环境变量
cp .env.example .env
# 编辑 .env，修改 JWT_SECRET 和 MUSIC_DIR

# 初始化数据库
npx prisma db push

# 启动开发服务器
npm run dev
```

### 移动端

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

- **框架**: Elysia.js
- **ORM**: Prisma + SQLite
- **认证**: @elysiajs/jwt + bcryptjs
- **文档**: @elysiajs/openapi (Scalar)

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /auth/register | 注册 |
| POST | /auth/login | 登录 |
| GET | /auth/me | 当前用户 |
| GET | /music | 歌曲列表 |
| GET | /music/:id | 歌曲详情 |
| POST | /music/scan | 扫描音乐目录 |
| GET | /music/search/:query | 搜索歌曲 |
| DELETE | /music/:id | 删除歌曲 |
| POST | /music/:id/play | 增加播放次数 |

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| DATABASE_URL | 数据库路径 | file:./data/database.db |
| JWT_SECRET | JWT 密钥 | - |
| MUSIC_DIR | 音乐文件目录 | /music |
