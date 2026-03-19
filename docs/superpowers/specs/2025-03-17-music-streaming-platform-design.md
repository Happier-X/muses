# 音乐流媒体平台设计文档

**项目名称**: Muses - NAS 音乐流媒体平台
**创建日期**: 2025-03-17
**状态**: 设计阶段

---

## 1. 项目概述

### 1.1 目标
构建一个部署在飞牛 NAS 上的个人音乐流媒体平台，支持家庭成员（2-5人）在手机上播放和管理个人音乐库。

### 1.2 核心功能
- 音乐库管理（艺术家/专辑/歌曲浏览）
- 播放列表管理
- 收藏功能
- 音频自动转码（支持混合格式）
- 多用户支持
- 流式播放

### 1.3 技术约束
- 部署环境：飞牛 NAS（支持 Docker）
- 客户端：uniappx (Android/iOS)
- 音频格式：混合格式（FLAC/APE/MP3/AAC 等）
- 网络环境：家庭内网 + 外网访问（可选）

---

## 2. 技术栈

### 2.1 后端服务
- **框架**: Node.js + Express.js
- **ORM**: Prisma
- **数据库**: SQLite
- **认证**: JWT
- **音频处理**: FFmpeg + fluent-ffmpeg
- **文件上传**: Multer

### 2.2 Web 管理界面
- **框架**: React + Vite
- **样式**: Tailwind CSS
- **组件库**: shadcn/ui

### 2.3 移动客户端
- **框架**: uniappx (UVue)
- **语法**: Vue 3
- **播放器**: uni.getBackgroundAudioManager()
- **网络**: uni.request
- **本地存储**: uni.getStorageSync

---

## 3. 系统架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         NAS 服务器                          │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │  音乐存储    │  │   SQLite     │  │   FFmpeg         │   │
│  │  /music     │  │   数据库      │  │   转码服务        │   │
│  └──────┬──────┘  └──────┬───────┘  └──────────────────┘   │
│         │                │
│  ┌──────▼────────────────▼───────┐                            │
│  │      Node.js API 服务         │                            │
│  │  (音乐管理·用户认证·转码控制)   │                            │
│  └──────────────┬────────────────┘                            │
└─────────────────┼──────────────────────────────────────────┘
                  │ REST API
                  │ (HTTP/HTTPS)
          ┌───────┴────────┐
          │                │
    ┌─────▼─────┐   ┌──────▼──────┐
    │ uniappx   │   │  Web 管理端  │
    │  移动端    │   │   (React)   │
    └───────────┘   └─────────────┘
```

### 3.2 部署架构

```
/muses/
├── docker-compose.yml
├── backend/              # Node.js API
│   ├── Dockerfile
│   ├── package.json
│   └── src/
├── web/                  # React 管理界面
│   ├── Dockerfile
│   └── ...
├── client/               # uniappx 移动端
│   ├── pages/
│   ├── components/
│   ├── utils/
│   └── manifest.json
├── ssl/                  # SSL 证书
└── data/
    ├── database/         # SQLite 数据库
    └── cache/            # 转码缓存
```

---

## 4. 数据模型

### 4.1 数据库表结构

```sql
-- 用户表
users {
  id: primary key
  username: string (unique)
  password_hash: string
  created_at: timestamp
}

-- 艺术家表
artists {
  id: primary key
  name: string
  cover_art: string (optional)
}

-- 专辑表
albums {
  id: primary key
  title: string
  artist_id: foreign key → artists
  cover_art: string (optional)
  year: integer (optional)
}

-- 歌曲表
songs {
  id: primary key
  title: string
  album_id: foreign key → albums
  artist_id: foreign key → artists
  duration: integer (seconds)
  file_path: string
  file_format: string (flac/mp3/aac/etc)
  bitrate: integer
  track_number: integer (optional)
  created_at: timestamp
}

-- 播放列表表
playlists {
  id: primary key
  user_id: foreign key → users
  name: string
  created_at: timestamp
}

-- 播放列表歌曲关联表
playlist_songs {
  playlist_id: foreign key → playlists
  song_id: foreign key → songs
  position: integer (排序)
  primary key (playlist_id, song_id)
}

-- 收藏表
favorites {
  user_id: foreign key → users
  song_id: foreign key → songs
  created_at: timestamp
  primary key (user_id, song_id)
}
```

### 4.2 ER 图

```
users (1) ──< (N) playlists
users (1) ──< (N) favorites
artists (1) ──< (N) albums
artists (1) ──< (N) songs
albums (1) ──< (N) songs
playlists (1) ──< (N) songs (通过 playlist_songs)
songs (1) ──< (N) favorites
```

---

## 5. API 设计

### 5.1 认证相关

```
POST   /api/auth/register      # 注册
POST   /api/auth/login         # 登录
GET    /api/auth/me            # 获取当前用户信息
```

### 5.2 音乐库

```
GET    /api/artists            # 获取所有艺术家
GET    /api/artists/:id        # 获取艺术家详情
GET    /api/albums             # 获取所有专辑
GET    /api/albums/:id         # 获取专辑详情
GET    /api/songs              # 获取所有歌曲
GET    /api/songs/:id          # 获取歌曲详情
GET    /api/songs/:id/stream   # 流式播放歌曲
POST   /api/library/scan       # 扫描音乐库（新增/更新）
```

### 5.3 播放列表

```
GET    /api/playlists          # 获取所有播放列表
POST   /api/playlists          # 创建播放列表
GET    /api/playlists/:id      # 获取播放列表详情
PUT    /api/playlists/:id      # 更新播放列表
DELETE /api/playlists/:id      # 删除播放列表
POST   /api/playlists/:id/songs # 添加歌曲到播放列表
DELETE /api/playlists/:id/songs/:songId # 从播放列表删除歌曲
```

### 5.4 收藏

```
GET    /api/favorites          # 获取收藏列表
POST   /api/favorites/:songId  # 添加收藏
DELETE /api/favorites/:songId  # 取消收藏
```

---

## 6. 音频转码系统

### 6.1 转码策略

```javascript
// 客户端请求时携带能力信息
GET /api/songs/:id/stream
Headers: {
  "Accept-Encoding": "opus,aac,mp3",
  "Max-Bitrate": "320000",
  "X-Device-Type": "mobile"
}

// 服务端决策逻辑
const transcodeDecision = {
  originalFormat: "flac",
  originalBitrate: 960000,

  // 决策规则:
  // 1. 客户端支持原始格式且带宽足够 → 直接传输
  // 2. 移动网络环境 → 转码为 AAC 128kbps
  // 3. WiFi 环境 → 转码为 AAC 256kbps
}
```

### 6.2 转码缓存

```
/music-cache/
  /transcoded/
    song-1-aac-128.mp3
    song-1-aac-256.mp3
    song-2-aac-128.mp3
```

- 首次转码后缓存到磁盘
- 基于原始文件修改时间检测缓存失效
- LRU 清理策略（限制总大小，如 10GB）

### 6.3 FFmpeg 命令示例

```bash
# FLAC → AAC 256kbps (WiFi)
ffmpeg -i input.flac -c:a aac -b:a 256k -ar 44100 output.aac

# FLAC → MP3 128kbps (移动网络)
ffmpeg -i input.flac -c:a libmp3lame -b:a 128k -ar 44100 output.mp3

# FLAC → AAC 320kbps (高质量)
ffmpeg -i input.flac -c:a aac -b:a 320k -ar 48000 output.aac
```

---

## 7. 安全性设计

### 7.1 认证流程

1. 用户注册/登录 → 返回 JWT Token
2. Token 存储在客户端（uni.setStorageSync）
3. 每次请求携带: `Authorization: Bearer <token>`
4. Token 过期时间: 30天

### 7.2 API 安全

- **HTTPS 强制**: 所有 API 通信加密
- **Rate Limiting**: 防止暴力破解
- **文件访问控制**: 音乐文件通过 API 代理，不直接暴露

### 7.3 数据安全

- **密码加密**: bcrypt 哈希
- **SQL 注入防护**: 使用 Prisma ORM
- **上传文件验证**: 只允许音频格式

---

## 8. Docker 部署配置

### 8.1 Docker Compose

```yaml
version: '3.8'
services:
  music-api:
    build: ./backend
    ports:
      - "3000:3000"
    volumes:
      - /path/to/music:/music:ro           # 音乐库（只读）
      - ./data/database:/app/data          # 数据库持久化
      - ./data/cache:/app/cache            # 转码缓存
      - /path/to/uploads:/app/uploads      # 上传目录
    environment:
      - NODE_ENV=production
      - JWT_SECRET=${JWT_SECRET}
      - MUSIC_PATH=/music
      - TRANSCODE_CACHE_PATH=/app/cache
    restart: unless-stopped
```

### 8.2 网络配置

- **内网访问**: http://nas-local-ip:3000
- **外网访问**: 配置 NAS 端口转发 + 域名（可选）
- **HTTPS**: Let's Encrypt 或自签名证书（可在 NAS 层面配置）

---

## 9. 开发阶段规划

### Phase 1: 后端核心（1-2周）
- [ ] 项目初始化 + Docker 配置
- [ ] 数据库 + Prisma 设置
- [ ] 用户认证系统
- [ ] 音乐文件扫描 + 元数据解析
- [ ] 基础 CRUD API

### Phase 2: 音频功能（1周）
- [ ] FFmpeg 转码集成
- [ ] 流式播放 API
- [ ] 转码缓存系统

### Phase 3: Web 管理界面（1-2周）
- [ ] React 项目搭建
- [ ] 音乐库浏览界面
- [ ] 播放列表管理
- [ ] 设置页面

### Phase 4: 移动客户端（2-3周）
- [ ] uniappx 项目搭建
- [ ] 登录/注册界面
- [ ] 音乐播放器（BackgroundAudioManager）
- [ ] 后台播放 + 锁屏控制
- [ ] 音乐库浏览
- [ ] 播放列表功能
- [ ] 收藏功能

### Phase 5: 部署与测试（1周）
- [ ] Docker 部署到 NAS
- [ ] SSL 证书配置
- [ ] 端到端测试
- [ ] 性能优化

**预计总时间**: 6-9周

---

## 10. 未来迭代功能

- [ ] 多设备播放进度同步
- [ ] 智能推荐系统
- [ ] 歌词显示
- [ ] 社交分享功能

---

## 附录

### A. 技术选型理由

1. **Node.js**: 生态丰富，音频库成熟，开发速度快
2. **SQLite**: NAS 部署友好，无需额外数据库服务
3. **FFmpeg**: 最成熟的音频处理工具，格式支持最广
4. **React**: Web 开发效率高，生态成熟
5. **uniappx**: 跨平台 Android/iOS，Vue 语法上手快

### B. 参考资源

- uniappx 官方文档: https://uniappx.com/
- FFmpeg 文档: https://ffmpeg.org/documentation.html
- Prisma 文档: https://www.prisma.io/docs
