# 音乐流媒体平台实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个部署在飞牛 NAS 上的个人音乐流媒体平台，支持家庭成员在手机上播放和管理个人音乐库

**Architecture:** 前后端分离架构 - Node.js API + React Web管理端 + Kotlin Android客户端，通过 Docker 容器化部署

**Tech Stack:** Node.js/Express, Prisma/SQLite, FFmpeg, React/Vite, Kotlin/Jetpack Compose/ExoPlayer

---

## 项目文件结构

```
muses/
├── backend/                    # Node.js API 服务
│   ├── src/
│   │   ├── index.ts           # 入口文件
│   │   ├── app.ts            # Express 应用
│   │   ├── config/
│   │   │   └── index.ts      # 配置
│   │   ├── middleware/
│   │   │   ├── auth.ts       # 认证中间件
│   │   │   └── error.ts      # 错误处理
│   │   ├── routes/
│   │   │   ├── auth.ts       # 认证路由
│   │   │   ├── artists.ts    # 艺术家路由
│   │   │   ├── albums.ts     # 专辑路由
│   │   │   ├── songs.ts      # 歌曲路由
│   │   │   ├── playlists.ts  # 播放列表路由
│   │   │   └── favorites.ts  # 收藏路由
│   │   ├── services/
│   │   │   ├── music.ts      # 音乐扫描服务
│   │   │   ├── transcode.ts  # 转码服务
│   │   │   └── stream.ts     # 流媒体服务
│   │   ├── controllers/
│   │   │   ├── auth.ts
│   │   │   ├── artists.ts
│   │   │   ├── albums.ts
│   │   │   ├── songs.ts
│   │   │   ├── playlists.ts
│   │   │   └── favorites.ts
│   │   └── utils/
│   │       └── logger.ts     # 日志工具
│   ├── prisma/
│   │   └── schema.prisma     # 数据库模型
│   ├── package.json
│   ├── tsconfig.json
│   └── Dockerfile
├── web/                       # React Web 管理界面
│   ├── src/
│   │   ├── main.tsx
│   │   ├── App.tsx
│   │   ├── api/
│   │   │   └── client.ts     # API 客户端
│   │   ├── components/
│   │   │   ├── Layout.tsx
│   │   │   ├── Player.tsx
│   │   │   └── ...
│   │   ├── pages/
│   │   │   ├── Login.tsx
│   │   │   ├── Library.tsx
│   │   │   ├── Playlists.tsx
│   │   │   └── Settings.tsx
│   │   └── stores/
│   │       └── player.ts     # 播放状态管理
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   └── Dockerfile
├── android/                   # Kotlin Android 客户端
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/muses/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── api/
│   │   │   │   │   └── ApiClient.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   ├── repository/
│   │   │   │   │   └── local/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/
│   │   │   │   │   ├── screens/
│   │   │   │   │   └── components/
│   │   │   │   └── player/
│   │   │   │       └── PlayerService.kt
│   │   │   ├── res/
│   │   │   │   └── values/
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── gradle/
├── docker-compose.yml
└── README.md
```

---

## Phase 1: 后端核心（1-2周）

### Task 1: 初始化后端项目

**Files:**
- Create: `backend/package.json`
- Create: `backend/tsconfig.json`
- Create: `backend/src/index.ts`
- Create: `backend/src/app.ts`
- Create: `backend/prisma/schema.prisma`

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "muses-backend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "tsx watch src/index.ts",
    "build": "tsc",
    "start": "node dist/index.js",
    "db:generate": "prisma generate",
    "db:push": "prisma db push"
  },
  "dependencies": {
    "@prisma/client": "^5.10.0",
    "bcryptjs": "^2.4.3",
    "dotenv": "^16.4.0",
    "express": "^4.18.2",
    "fluent-ffmpeg": "^2.1.2",
    "jsonwebtoken": "^9.0.2",
    "multer": "^1.4.5-lts.1",
    "music-metadata": "^7.14.0",
    "zod": "^3.22.4"
  },
  "devDependencies": {
    "@types/bcryptjs": "^2.4.6",
    "@types/express": "^4.17.21",
    "@types/fluent-ffmpeg": "^2.1.24",
    "@types/jsonwebtoken": "^9.0.5",
    "@types/multer": "^1.4.11",
    "@types/node": "^20.11.0",
    "prisma": "^5.10.0",
    "tsx": "^4.7.0",
    "typescript": "^5.3.3"
  }
}
```

- [ ] **Step 2: 创建 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "lib": ["ES2022"],
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

- [ ] **Step 3: 创建 Prisma schema**

```prisma
// backend/prisma/schema.prisma

generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "sqlite"
  url      = env("DATABASE_URL")
}

model User {
  id           Int          @id @default(autoincrement())
  username     String       @unique
  passwordHash String
  createdAt    DateTime     @default(now())
  playlists    Playlist[]
  favorites    Favorite[]
}

model Artist {
  id        Int     @id @default(autoincrement())
  name      String
  coverArt  String?
  albums    Album[]
  songs     Song[]
}

model Album {
  id        Int      @id @default(autoincrement())
  title     String
  artistId  Int
  artist    Artist   @relation(fields: [artistId], references: [id])
  coverArt  String?
  year      Int?
  songs     Song[]
}

model Song {
  id          Int       @id @default(autoincrement())
  title       String
  albumId     Int
  album       Album     @relation(fields: [albumId], references: [id])
  artistId    Int
  artist      Artist    @relation(fields: [artistId], references: [id])
  duration    Int       // seconds
  filePath    String
  fileFormat  String
  bitrate     Int
  trackNumber Int?
  createdAt   DateTime  @default(now())
  playlistSongs PlaylistSong[]
  favorites   Favorite[]
}

model Playlist {
  id        Int              @id @default(autoincrement())
  userId    Int
  user      User             @relation(fields: [userId], references: [id])
  name      String
  createdAt DateTime         @default(now())
  songs     PlaylistSong[]
}

model PlaylistSong {
  playlistId Int
  playlist   Playlist @relation(fields: [playlistId], references: [id])
  songId     Int
  song       Song     @relation(fields: [songId], references: [id])
  position   Int
  @@id([playlistId, songId])
}

model Favorite {
  userId    Int
  user      User   @relation(fields: [userId], references: [id])
  songId    Int
  song      Song   @relation(fields: [songId], references: [id])
  createdAt DateTime @default(now())
  @@id([userId, songId])
}
```

- [ ] **Step 4: 创建基础 Express 应用**

```typescript
// backend/src/app.ts
import express from 'express';
import cors from 'cors';
import { errorHandler } from './middleware/error.js';

export function createApp() {
  const app = express();

  app.use(cors());
  app.use(express.json());
  app.use(express.urlencoded({ extended: true }));

  // Routes will be added here

  app.use(errorHandler);

  return app;
}
```

- [ ] **Step 5: 创建入口文件**

```typescript
// backend/src/index.ts
import { createApp } from './app.js';
import dotenv from 'dotenv';

dotenv.config();

const app = createApp();
const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Muses API running on port ${PORT}`);
});
```

- [ ] **Step 6: 创建错误处理中间件**

```typescript
// backend/src/middleware/error.ts
import { Request, Response, NextFunction } from 'express';

export class AppError extends Error {
  constructor(public statusCode: number, message: string) {
    super(message);
    this.name = 'AppError';
  }
}

export function errorHandler(
  err: Error | AppError,
  req: Request,
  res: Response,
  next: NextFunction
) {
  if (err instanceof AppError) {
    return res.status(err.statusCode).json({
      error: err.message
    });
  }

  console.error(err);
  return res.status(500).json({
    error: 'Internal server error'
  });
}
```

- [ ] **Step 7: 创建配置**

```typescript
// backend/src/config/index.ts
import dotenv from 'dotenv';

dotenv.config();

export const config = {
  port: process.env.PORT || 3000,
  jwtSecret: process.env.JWT_SECRET || 'your-secret-key',
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || '30d',
  databaseUrl: process.env.DATABASE_URL || 'file:./data/database.db',
  musicPath: process.env.MUSIC_PATH || '/music',
  cachePath: process.env.TRANSCODE_CACHE_PATH || './cache'
};
```

- [ ] **Step 8: 安装依赖并生成 Prisma Client**

```bash
cd backend
npm install
npx prisma generate
```

- [ ] **Step 9: 提交代码**

```bash
git add backend/
git commit -m "feat(backend): initialize project with Express, Prisma, TypeScript"
```

---

### Task 2: 用户认证系统

**Files:**
- Modify: `backend/src/app.ts`
- Create: `backend/src/middleware/auth.ts`
- Create: `backend/src/controllers/auth.ts`
- Create: `backend/src/routes/auth.ts`

- [ ] **Step 1: 创建认证中间件**

```typescript
// backend/src/middleware/auth.ts
import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config/index.js';

export interface AuthRequest extends Request {
  userId?: number;
}

export function authMiddleware(
  req: AuthRequest,
  res: Response,
  next: NextFunction
) {
  const authHeader = req.headers.authorization;

  if (!authHeader?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'No token provided' });
  }

  const token = authHeader.slice(7);

  try {
    const decoded = jwt.verify(token, config.jwtSecret) as { userId: number };
    req.userId = decoded.userId;
    next();
  } catch {
    return res.status(401).json({ error: 'Invalid token' });
  }
}
```

- [ ] **Step 2: 创建认证控制器**

```typescript
// backend/src/controllers/auth.ts
import { Request, Response } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { PrismaClient } from '@prisma/client';
import { config } from '../config/index.js';
import { AppError } from '../middleware/error.js';

const prisma = new PrismaClient();

export async function register(req: Request, res: Response) {
  const { username, password } = req.body;

  if (!username || !password) {
    throw new AppError(400, 'Username and password required');
  }

  const existing = await prisma.user.findUnique({ where: { username } });
  if (existing) {
    throw new AppError(400, 'Username already exists');
  }

  const passwordHash = await bcrypt.hash(password, 10);
  const user = await prisma.user.create({
    data: { username, passwordHash }
  });

  const token = jwt.sign({ userId: user.id }, config.jwtSecret, {
    expiresIn: config.jwtExpiresIn
  });

  res.json({ token, user: { id: user.id, username: user.username } });
}

export async function login(req: Request, res: Response) {
  const { username, password } = req.body;

  if (!username || !password) {
    throw new AppError(400, 'Username and password required');
  }

  const user = await prisma.user.findUnique({ where: { username } });
  if (!user) {
    throw new AppError(401, 'Invalid credentials');
  }

  const valid = await bcrypt.compare(password, user.passwordHash);
  if (!valid) {
    throw new AppError(401, 'Invalid credentials');
  }

  const token = jwt.sign({ userId: user.id }, config.jwtSecret, {
    expiresIn: config.jwtExpiresIn
  });

  res.json({ token, user: { id: user.id, username: user.username } });
}

export async function getMe(req: AuthRequest, res: Response) {
  const user = await prisma.user.findUnique({
    where: { id: req.userId },
    select: { id: true, username: true, createdAt: true }
  });

  if (!user) {
    throw new AppError(404, 'User not found');
  }

  res.json(user);
}
```

- [ ] **Step 3: 创建认证路由**

```typescript
// backend/src/routes/auth.ts
import { Router } from 'express';
import { register, login, getMe } from '../controllers/auth.js';
import { authMiddleware, AuthRequest } from '../middleware/auth.js';

const router = Router();

router.post('/register', register);
router.post('/login', login);
router.get('/me', authMiddleware, (req: AuthRequest, res: Response) => getMe(req, res));

export default router;
```

- [ ] **Step 4: 注册路由到 app.ts**

```typescript
// backend/src/app.ts (添加路由)
import authRoutes from './routes/auth.js';

// 在 createApp() 函数中添加:
app.use('/api/auth', authRoutes);
```

- [ ] **Step 5: 提交代码**

```bash
git add backend/
git commit -m "feat(backend): add user authentication with JWT"
```

---

### Task 3: 音乐扫描与元数据解析

**Files:**
- Create: `backend/src/services/music.ts`
- Create: `backend/src/controllers/artists.ts`
- Create: `backend/src/controllers/albums.ts`
- Create: `backend/src/controllers/songs.ts`
- Create: `backend/src/routes/artists.ts`
- Create: `backend/src/routes/albums.ts`
- Create: `backend/src/routes/songs.ts`

- [ ] **Step 1: 创建音乐扫描服务**

```typescript
// backend/src/services/music.ts
import fs from 'fs';
import path from 'path';
import * as mm from 'music-metadata';
import { PrismaClient } from '@prisma/client';
import { config } from '../config/index.js';

const prisma = new PrismaClient();

interface ScannedFile {
  filePath: string;
  fileName: string;
  format: string;
  metadata: {
    title?: string;
    artist?: string;
    album?: string;
    year?: number;
    track?: number;
    duration?: number;
    bitrate?: number;
  };
}

async function parseMetadata(filePath: string): Promise<ScannedFile> {
  const metadata = await mm.parseFile(filePath);
  const fileName = path.basename(filePath);
  const format = path.extname(filePath).slice(1).toLowerCase();

  return {
    filePath,
    fileName,
    format,
    metadata: {
      title: metadata.common.title || fileName.replace(/\.[^.]+$/, ''),
      artist: metadata.common.artist,
      album: metadata.common.album,
      year: metadata.common.year,
      track: metadata.common.track.no,
      duration: Math.round(metadata.format.duration || 0),
      bitrate: metadata.format.bitrate
    }
  };
}

async function getOrCreateArtist(name: string) {
  let artist = await prisma.artist.findFirst({ where: { name } });
  if (!artist) {
    artist = await prisma.artist.create({ data: { name } });
  }
  return artist;
}

async function getOrCreateAlbum(title: string, artistId: number, year?: number) {
  let album = await prisma.album.findFirst({
    where: { title, artistId }
  });
  if (!album) {
    album = await prisma.album.create({
      data: { title, artistId, year }
    });
  }
  return album;
}

export async function scanMusicLibrary() {
  const musicPath = config.musicPath;
  const audioExtensions = ['.mp3', '.flac', '.aac', '.m4a', '.ogg', '.wav', '.ape'];

  const files: string[] = [];

  function walkDir(dir: string) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        walkDir(fullPath);
      } else if (audioExtensions.includes(path.extname(entry.name).toLowerCase())) {
        files.push(fullPath);
      }
    }
  }

  if (!fs.existsSync(musicPath)) {
    throw new Error(`Music path does not exist: ${musicPath}`);
  }

  walkDir(musicPath);

  let added = 0;
  let updated = 0;

  for (const filePath of files) {
    const existing = await prisma.song.findFirst({ where: { filePath } });

    if (existing) {
      const stat = fs.statSync(filePath);
      if (stat.mtime <= existing.createdAt) {
        continue;
      }
    }

    const scanned = await parseMetadata(filePath);
    const artist = await getOrCreateArtist(scanned.metadata.artist || 'Unknown Artist');
    const album = await getOrCreateAlbum(
      scanned.metadata.album || 'Unknown Album',
      artist.id,
      scanned.metadata.year
    );

    if (existing) {
      await prisma.song.update({
        where: { id: existing.id },
        data: {
          title: scanned.metadata.title,
          albumId: album.id,
          artistId: artist.id,
          duration: scanned.metadata.duration || 0,
          fileFormat: scanned.format,
          bitrate: Math.round((scanned.metadata.bitrate || 0) / 1000),
          trackNumber: scanned.metadata.track
        }
      });
      updated++;
    } else {
      await prisma.song.create({
        data: {
          title: scanned.metadata.title || 'Unknown',
          albumId: album.id,
          artistId: artist.id,
          duration: scanned.metadata.duration || 0,
          filePath,
          fileFormat: scanned.format,
          bitrate: Math.round((scanned.metadata.bitrate || 0) / 1000),
          trackNumber: scanned.metadata.track
        }
      });
      added++;
    }
  }

  return { added, updated, total: files.length };
}

export async function getAllArtists() {
  return prisma.artist.findMany({
    include: {
      _count: { select: { songs: true, albums: true } }
    },
    orderBy: { name: 'asc' }
  });
}

export async function getArtistById(id: number) {
  return prisma.artist.findUnique({
    where: { id },
    include: {
      albums: { include: { _count: { select: { songs: true } } } },
      songs: true
    }
  });
}

export async function getAllAlbums() {
  return prisma.album.findMany({
    include: {
      artist: true,
      _count: { select: { songs: true } }
    },
    orderBy: { title: 'asc' }
  });
}

export async function getAlbumById(id: number) {
  return prisma.album.findUnique({
    where: { id },
    include: {
      artist: true,
      songs: { orderBy: { trackNumber: 'asc' } }
    }
  });
}

export async function getAllSongs() {
  return prisma.song.findMany({
    include: { artist: true, album: true },
    orderBy: { title: 'asc' }
  });
}

export async function getSongById(id: number) {
  return prisma.song.findUnique({
    where: { id },
    include: { artist: true, album: true }
  });
}
```

- [ ] **Step 2: 创建控制器**

```typescript
// backend/src/controllers/artists.ts
import { Response } from 'express';
import { getAllArtists, getArtistById } from '../services/music.js';
import { AppError } from '../middleware/error.js';

export async function listArtists(req: Response) {
  const artists = await getAllArtists();
  return artists;
}

export async function getArtist(req: Response, id: number) {
  const artist = await getArtistById(id);
  if (!artist) {
    throw new AppError(404, 'Artist not found');
  }
  return artist;
}

// backend/src/controllers/albums.ts
export async function listAlbums(req: Response) {
  return getAllAlbums();
}

export async function getAlbum(req: Response, id: number) {
  const album = await getAlbumById(id);
  if (!album) {
    throw new AppError(404, 'Album not found');
  }
  return album;
}

// backend/src/controllers/songs.ts
export async function listSongs(req: Response) {
  return getAllSongs();
}

export async function getSong(req: Response, id: number) {
  const song = await getSongById(id);
  if (!song) {
    throw new AppError(404, 'Song not found');
  }
  return song;
}

export async function scanLibrary(req: Request, res: Response) {
  const result = await scanMusicLibrary();
  return result;
}
```

- [ ] **Step 3: 创建路由**

```typescript
// backend/src/routes/artists.ts
import { Router } from 'express';
import { listArtists, getArtist } from '../controllers/artists.js';

const router = Router();

router.get('/', listArtists);
router.get('/:id', (req, res) => getArtist(res, parseInt(req.params.id)));

export default router;

// backend/src/routes/albums.ts
import { Router } from 'express';
import { listAlbums, getAlbum } from '../controllers/albums.js';

const router = Router();

router.get('/', listAlbums);
router.get('/:id', (req, res) => getAlbum(res, parseInt(req.params.id)));

export default router;

// backend/src/routes/songs.ts
import { Router } from 'express';
import { listSongs, getSong } from '../controllers/songs.js';

const router = Router();

router.get('/', listSongs);
router.get('/:id', (req, res) => getSong(res, parseInt(req.params.id)));

export default router;
```

- [ ] **Step 4: 注册路由**

```typescript
// backend/src/app.ts
import artistsRoutes from './routes/artists.js';
import albumsRoutes from './routes/albums.js';
import songsRoutes from './routes/songs.js';

app.use('/api/artists', artistsRoutes);
app.use('/api/albums', albumsRoutes);
app.use('/api/songs', songsRoutes);
```

- [ ] **Step 5: 添加扫描端点**

```typescript
// backend/src/controllers/songs.ts 新增
import { Request, Response } from 'express';
import { scanMusicLibrary } from '../services/music.js';

export async function scanLibrary(req: Request, res: Response) {
  const result = await scanMusicLibrary();
  return result;
}

// backend/src/routes/songs.ts 新增
router.post('/scan', scanLibrary);
```

- [ ] **Step 6: 提交代码**

```bash
git add backend/
git commit -m "feat(backend): add music library scanning and metadata parsing"
```

---

### Task 4: 播放列表与收藏功能

**Files:**
- Create: `backend/src/controllers/playlists.ts`
- Create: `backend/src/controllers/favorites.ts`
- Create: `backend/src/routes/playlists.ts`
- Create: `backend/src/routes/favorites.ts`

- [ ] **Step 1: 创建播放列表控制器**

```typescript
// backend/src/controllers/playlists.ts
import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { AuthRequest } from '../middleware/auth.js';
import { AppError } from '../middleware/error.js';

const prisma = new PrismaClient();

export async function listPlaylists(req: AuthRequest, res: Response) {
  return prisma.playlist.findMany({
    where: { userId: req.userId },
    include: {
      songs: {
        include: { song: { include: { artist: true, album: true } } },
        orderBy: { position: 'asc' }
      },
      _count: { select: { songs: true } }
    },
    orderBy: { createdAt: 'desc' }
  });
}

export async function createPlaylist(req: AuthRequest, res: Response) {
  const { name } = req.body;

  if (!name) {
    throw new AppError(400, 'Playlist name required');
  }

  return prisma.playlist.create({
    data: { userId: req.userId!, name }
  });
}

export async function getPlaylist(req: AuthRequest, res: Response) {
  const id = parseInt(req.params.id);
  const playlist = await prisma.playlist.findFirst({
    where: { id, userId: req.userId },
    include: {
      songs: {
        include: { song: { include: { artist: true, album: true } } },
        orderBy: { position: 'asc' }
      }
    }
  });

  if (!playlist) {
    throw new AppError(404, 'Playlist not found');
  }

  return playlist;
}

export async function updatePlaylist(req: AuthRequest, res: Response) {
  const id = parseInt(req.params.id);
  const { name } = req.body;

  const playlist = await prisma.playlist.findFirst({
    where: { id, userId: req.userId }
  });

  if (!playlist) {
    throw new AppError(404, 'Playlist not found');
  }

  return prisma.playlist.update({
    where: { id },
    data: { name }
  });
}

export async function deletePlaylist(req: AuthRequest, res: Response) {
  const id = parseInt(req.params.id);

  const playlist = await prisma.playlist.findFirst({
    where: { id, userId: req.userId }
  });

  if (!playlist) {
    throw new AppError(404, 'Playlist not found');
  }

  await prisma.playlist.delete({ where: { id } });
  return { success: true };
}

export async function addSongToPlaylist(req: AuthRequest, res: Response) {
  const playlistId = parseInt(req.params.id);
  const { songId } = req.body;

  const playlist = await prisma.playlist.findFirst({
    where: { id: playlistId, userId: req.userId }
  });

  if (!playlist) {
    throw new AppError(404, 'Playlist not found');
  }

  const maxPosition = await prisma.playlistSong.aggregate({
    where: { playlistId },
    _max: { position: true }
  });

  const position = (maxPosition._max.position || 0) + 1;

  await prisma.playlistSong.create({
    data: { playlistId, songId, position },
    ignoreDuplicates: true
  });

  return { success: true };
}

export async function removeSongFromPlaylist(req: AuthRequest, res: Response) {
  const playlistId = parseInt(req.params.id);
  const songId = parseInt(req.params.songId);

  const playlist = await prisma.playlist.findFirst({
    where: { id: playlistId, userId: req.userId }
  });

  if (!playlist) {
    throw new AppError(404, 'Playlist not found');
  }

  await prisma.playlistSong.delete({
    where: { playlistId_songId: { playlistId, songId } }
  });

  return { success: true };
}
```

- [ ] **Step 2: 创建收藏控制器**

```typescript
// backend/src/controllers/favorites.ts
import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { AuthRequest } from '../middleware/auth.js';
import { AppError } from '../middleware/error.js';

const prisma = new PrismaClient();

export async function listFavorites(req: AuthRequest, res: Response) {
  return prisma.favorite.findMany({
    where: { userId: req.userId },
    include: {
      song: { include: { artist: true, album: true } }
    },
    orderBy: { createdAt: 'desc' }
  });
}

export async function addFavorite(req: AuthRequest, res: Response) {
  const songId = parseInt(req.params.songId);

  const song = await prisma.song.findUnique({ where: { id: songId } });
  if (!song) {
    throw new AppError(404, 'Song not found');
  }

  return prisma.favorite.create({
    data: { userId: req.userId!, songId }
  }).catch(() => ({ success: true }));
}

export async function removeFavorite(req: AuthRequest, res: Response) {
  const songId = parseInt(req.params.songId);

  await prisma.favorite.delete({
    where: { userId_songId: { userId: req.userId!, songId } }
  }).catch(() => {});

  return { success: true };
}
```

- [ ] **Step 3: 创建路由**

```typescript
// backend/src/routes/playlists.ts
import { Router } from 'express';
import { authMiddleware } from '../middleware/auth.js';
import {
  listPlaylists,
  createPlaylist,
  getPlaylist,
  updatePlaylist,
  deletePlaylist,
  addSongToPlaylist,
  removeSongFromPlaylist
} from '../controllers/playlists.js';

const router = Router();

router.use(authMiddleware);
router.get('/', listPlaylists);
router.post('/', createPlaylist);
router.get('/:id', getPlaylist);
router.put('/:id', updatePlaylist);
router.delete('/:id', deletePlaylist);
router.post('/:id/songs', addSongToPlaylist);
router.delete('/:id/songs/:songId', removeSongFromPlaylist);

export default router;

// backend/src/routes/favorites.ts
import { Router } from 'express';
import { authMiddleware } from '../middleware/auth.js';
import { listFavorites, addFavorite, removeFavorite } from '../controllers/favorites.js';

const router = Router();

router.use(authMiddleware);
router.get('/', listFavorites);
router.post('/:songId', addFavorite);
router.delete('/:songId', removeFavorite);

export default router;
```

- [ ] **Step 4: 注册路由**

```typescript
// backend/src/app.ts
import playlistsRoutes from './routes/playlists.js';
import favoritesRoutes from './routes/favorites.js';

app.use('/api/playlists', playlistsRoutes);
app.use('/api/favorites', favoritesRoutes);
```

- [ ] **Step 5: 提交代码**

```bash
git add backend/
git commit -m "feat(backend): add playlists and favorites functionality"
```

---

### Task 5: 后端 Dockerfile

**Files:**
- Create: `backend/Dockerfile`
- Create: `backend/.dockerignore`

- [ ] **Step 1: 创建 Dockerfile**

```dockerfile
# backend/Dockerfile
FROM node:20-alpine

WORKDIR /app

# Install FFmpeg
RUN apk add --no-cache ffmpeg

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy source
COPY . .

# Generate Prisma client
RUN npx prisma generate

# Build TypeScript
RUN npm run build

# Create cache directory
RUN mkdir -p /app/cache

EXPOSE 3000

CMD ["npm", "start"]
```

- [ ] **Step 2: 创建 .dockerignore**

```
node_modules
dist
.git
*.log
.env
.env.local
.cache
```

- [ ] **Step 3: 提交代码**

```bash
git add backend/
git commit -m "feat(backend): add Dockerfile for containerization"
```

---

## Phase 2: 音频功能（1周）

### Task 6: FFmpeg 转码服务

**Files:**
- Create: `backend/src/services/transcode.ts`

- [ ] **Step 1: 创建转码服务**

```typescript
// backend/src/services/transcode.ts
import ffmpeg from 'fluent-ffmpeg';
import fs from 'fs';
import path from 'path';
import { config } from '../config/index.js';

// Ensure cache directory exists
const cacheDir = path.join(config.cachePath, 'transcoded');
if (!fs.existsSync(cacheDir)) {
  fs.mkdirSync(cacheDir, { recursive: true });
}

export type TranscodeFormat = 'aac' | 'mp3' | 'opus';
export type Quality = 'low' | 'medium' | 'high';

const qualitySettings: Record<Quality, { bitrate: string; sampleRate: string }> = {
  low: { bitrate: '128k', sampleRate: '44100' },
  medium: { bitrate: '256k', sampleRate: '44100' },
  high: { bitrate: '320k', sampleRate: '48000' }
};

function getCacheKey(filePath: string, format: TranscodeFormat, quality: Quality): string {
  const stat = fs.statSync(filePath);
  const hash = `${path.basename(filePath)}-${stat.mtime.getTime()}-${format}-${quality}`;
  return hash.replace(/[^a-zA-Z0-9]/g, '_');
}

function getOutputPath(cacheKey: string, format: TranscodeFormat): string {
  return path.join(cacheDir, `${cacheKey}.${format}`);
}

export async function transcode(
  filePath: string,
  format: TranscodeFormat,
  quality: Quality
): Promise<string> {
  const cacheKey = getCacheKey(filePath, format, quality);
  const outputPath = getOutputPath(cacheKey, format);

  // Check cache
  if (fs.existsSync(outputPath)) {
    return outputPath;
  }

  const settings = qualitySettings[quality];

  return new Promise((resolve, reject) => {
    let command = ffmpeg(filePath)
      .audioCodec(format === 'mp3' ? 'libmp3lame' : 'aac')
      .audioBitrate(settings.bitrate)
      .audioFrequency(parseInt(settings.sampleRate));

    if (format === 'aac') {
      command = command.format('adts');
    } else if (format === 'mp3') {
      command = command.format('mp3');
    }

    command
      .on('end', () => resolve(outputPath))
      .on('error', (err) => reject(err))
      .save(outputPath);
  });
}

export function getBestFormat(acceptEncoding: string | undefined): TranscodeFormat {
  if (!acceptEncoding) return 'aac';

  const encodings = acceptEncoding.split(',').map(e => e.trim().toLowerCase());

  if (encodings.includes('aac')) return 'aac';
  if (encodings.includes('mp3')) return 'mp3';
  if (encodings.includes('opus')) return 'opus';

  return 'aac';
}

export function getQualityFromBitrate(maxBitrate: number | undefined): Quality {
  if (!maxBitrate || maxBitrate <= 128000) return 'low';
  if (maxBitrate <= 256000) return 'medium';
  return 'high';
}

export async function cleanupCache(maxSizeMB: number = 10240) {
  const files = fs.readdirSync(cacheDir)
    .map(f => {
      const filePath = path.join(cacheDir, f);
      const stat = fs.statSync(filePath);
      return { filePath, size: stat.size, mtime: stat.mtime };
    })
    .sort((a, b) => a.mtime.getTime() - b.mtime.getTime());

  let totalSize = files.reduce((sum, f) => sum + f.size, 0);
  const maxSizeBytes = maxSizeMB * 1024 * 1024;

  for (const file of files) {
    if (totalSize <= maxSizeBytes) break;
    fs.unlinkSync(file.filePath);
    totalSize -= file.size;
  }
}
```

- [ ] **Step 2: 创建流媒体服务**

```typescript
// backend/src/services/stream.ts
import fs from 'fs';
import path from 'path';
import { Response } from 'express';
import { getSongById } from './music.js';
import { transcode, getBestFormat, getQualityFromBitrate } from './transcode.js';
import { AppError } from '../middleware/error.js';
import { config } from '../config/index.js';

export async function streamSong(
  id: number,
  res: Response,
  options: {
    acceptEncoding?: string;
    maxBitrate?: number;
    deviceType?: string;
  }
) {
  const song = await getSongById(id);

  if (!song) {
    throw new AppError(404, 'Song not found');
  }

  const { acceptEncoding, maxBitrate, deviceType } = options;

  // Determine if transcoding is needed
  const format = getBestFormat(acceptEncoding);
  const quality = getQualityFromBitrate(maxBitrate);

  const needsTranscode = song.fileFormat !== 'mp3' || song.fileFormat !== 'aac';

  let streamPath: string;
  let contentType: string;
  let stat: fs.Stats;

  if (needsTranscode) {
    try {
      streamPath = await transcode(song.filePath, format, quality);
      contentType = format === 'mp3' ? 'audio/mpeg' : 'audio/aac';
    } catch (err) {
      // Fallback to original file
      streamPath = song.filePath;
      contentType = getContentType(song.fileFormat);
    }
  } else {
    streamPath = song.filePath;
    contentType = getContentType(song.fileFormat);
  }

  stat = fs.statSync(streamPath);

  // Handle Range requests for seeking
  const range = req.headers.range;
  if (range) {
    const parts = range.replace(/bytes=/, '').split('-');
    const start = parseInt(parts[0], 10);
    const end = parts[1] ? parseInt(parts[1], 10) : stat.size - 1;
    const chunksize = end - start + 1;

    res.writeHead(206, {
      'Content-Range': `bytes ${start}-${end}/${stat.size}`,
      'Accept-Ranges': 'bytes',
      'Content-Length': chunksize,
      'Content-Type': contentType
    });

    const stream = fs.createReadStream(streamPath, { start, end });
    stream.pipe(res);
  } else {
    res.writeHead(200, {
      'Content-Length': stat.size,
      'Content-Type': contentType,
      'Accept-Ranges': 'bytes'
    });

    const stream = fs.createReadStream(streamPath);
    stream.pipe(res);
  }
}

function getContentType(format: string): string {
  const types: Record<string, string> = {
    mp3: 'audio/mpeg',
    aac: 'audio/aac',
    flac: 'audio/flac',
    ogg: 'audio/ogg',
    wav: 'audio/wav',
    m4a: 'audio/mp4',
    alac: 'audio/alac'
  };
  return types[format.toLowerCase()] || 'audio/mpeg';
}
```

- [ ] **Step 3: 创建流媒体路由**

```typescript
// backend/src/routes/songs.ts 添加 stream 端点
import { Router } from 'express';
import { streamSong } from '../services/stream.js';

router.get('/:id/stream', async (req, res) => {
  const id = parseInt(req.params.id);
  const acceptEncoding = req.headers['accept-encoding'] as string | undefined;
  const maxBitrate = req.headers['max-bitrate'] ? parseInt(req.headers['max-bitrate'] as string) : undefined;
  const deviceType = req.headers['x-device-type'] as string | undefined;

  await streamSong(id, res, { acceptEncoding, maxBitrate, deviceType });
});
```

- [ ] **Step 4: 安装 ffmpeg (本地开发)**

```bash
# macOS
brew install ffmpeg

# Ubuntu/Debian
sudo apt install ffmpeg

# Windows
# Download from https://ffmpeg.org/download.html
```

- [ ] **Step 5: 提交代码**

```bash
git add backend/
git commit -m "feat(backend): add FFmpeg transcoding and streaming service"
```

---

## Phase 3: Web 管理界面（1-2周）

### Task 7: 初始化 React 项目

**Files:**
- Create: `web/package.json`
- Create: `web/tsconfig.json`
- Create: `web/vite.config.ts`
- Create: `web/tailwind.config.js`
- Create: `web/postcss.config.js`
- Create: `web/index.html`
- Create: `web/src/main.tsx`
- Create: `web/src/App.tsx`

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "muses-web",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.22.0",
    "axios": "^1.6.0",
    "zustand": "^4.5.0",
    "lucide-react": "^0.330.0",
    "clsx": "^2.1.0",
    "tailwind-merge": "^2.2.0",
    "class-variance-authority": "^0.7.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "@vitejs/plugin-react": "^4.2.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0",
    "tailwindcss": "^3.4.0",
    "typescript": "^5.3.0",
    "vite": "^5.1.0"
  }
}
```

- [ ] **Step 2: 创建配置文件**

```json
// web/tsconfig.json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}

// web/vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:3000',
        changeOrigin: true
      }
    }
  }
});

// web/tailwind.config.js
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        border: 'hsl(var(--border))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))'
      }
    }
  },
  plugins: []
};

// web/postcss.config.js
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {}
  }
};
```

- [ ] **Step 3: 创建入口文件**

```html
<!-- web/index.html -->
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Muses - 音乐流媒体</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

```typescript
// web/src/main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

```css
/* web/src/index.css */
@tailwind base;
@tailwind components;
@tailwind utilities;

:root {
  --background: 0 0% 100%;
  --foreground: 222.2 84% 4.9%;
  --border: 214.3 31.8% 91.4%;
}

body {
  background: hsl(var(--background));
  color: hsl(var(--foreground));
}
```

- [ ] **Step 4: 创建 App 组件**

```typescript
// web/src/App.tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './stores/auth';
import Layout from './components/Layout';
import Login from './pages/Login';
import Library from './pages/Library';
import Playlists from './pages/Playlists';
import Settings from './pages/Settings';

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const token = useAuthStore(state => state.token);
  return token ? <>{children}</> : <Navigate to="/login" />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<PrivateRoute><Layout /></PrivateRoute>}>
          <Route index element={<Library />} />
          <Route path="playlists" element={<Playlists />} />
          <Route path="settings" element={<Settings />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
```

- [ ] **Step 5: 创建 stores**

```typescript
// web/src/stores/auth.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  token: string | null;
  user: { id: number; username: string } | null;
  setAuth: (token: string, user: { id: number; username: string }) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      setAuth: (token, user) => set({ token, user }),
      logout: () => set({ token: null, user: null })
    }),
    { name: 'muses-auth' }
  )
);

// web/src/stores/player.ts
import { create } from 'zustand';

interface Song {
  id: number;
  title: string;
  duration: number;
  artist: { id: number; name: string };
  album: { id: number; title: string };
}

interface PlayerState {
  currentSong: Song | null;
  isPlaying: boolean;
  queue: Song[];
  currentIndex: number;
  play: (song: Song, queue?: Song[]) => void;
  pause: () => void;
  resume: () => void;
  next: () => void;
  prev: () => void;
}

export const usePlayerStore = create<PlayerState>((set, get) => ({
  currentSong: null,
  isPlaying: false,
  queue: [],
  currentIndex: 0,

  play: (song, queue) => {
    const newQueue = queue || [song];
    const index = queue ? newQueue.findIndex(s => s.id === song.id) : 0;
    set({ currentSong: song, isPlaying: true, queue: newQueue, currentIndex: index });
  },

  pause: () => set({ isPlaying: false }),
  resume: () => set({ isPlaying: true }),

  next: () => {
    const { queue, currentIndex } = get();
    if (currentIndex < queue.length - 1) {
      const nextIndex = currentIndex + 1;
      set({ currentSong: queue[nextIndex], currentIndex: nextIndex });
    }
  },

  prev: () => {
    const { queue, currentIndex } = get();
    if (currentIndex > 0) {
      const prevIndex = currentIndex - 1;
      set({ currentSong: queue[prevIndex], currentIndex: prevIndex });
    }
  }
}));
```

- [ ] **Step 6: 创建 API 客户端**

```typescript
// web/src/api/client.ts
import axios from 'axios';
import { useAuthStore } from '../stores/auth';

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
});

client.interceptors.request.use(config => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const authApi = {
  login: (username: string, password: string) => client.post('/auth/login', { username, password }),
  register: (username: string, password: string) => client.post('/auth/register', { username, password }),
  me: () => client.get('/auth/me')
};

export const libraryApi = {
  getArtists: () => client.get('/artists'),
  getAlbums: () => client.get('/albums'),
  getSongs: () => client.get('/songs'),
  scan: () => client.post('/songs/scan')
};

export const playlistsApi = {
  list: () => client.get('/playlists'),
  create: (name: string) => client.post('/playlists', { name }),
  get: (id: number) => client.get(`/playlists/${id}`),
  delete: (id: number) => client.delete(`/playlists/${id}`),
  addSong: (playlistId: number, songId: number) => client.post(`/playlists/${playlistId}/songs`, { songId }),
  removeSong: (playlistId: number, songId: number) => client.delete(`/playlists/${playlistId}/songs/${songId}`)
};

export const favoritesApi = {
  list: () => client.get('/favorites'),
  add: (songId: number) => client.post(`/favorites/${songId}`),
  remove: (songId: number) => client.delete(`/favorites/${songId}`)
};

export default client;
```

- [ ] **Step 7: 安装依赖并测试**

```bash
cd web
npm install
npm run dev
```

- [ ] **Step 8: 提交代码**

```bash
git add web/
git commit -m "feat(web): initialize React project with Vite and Tailwind"
```

---

### Task 8: Web 页面开发

**Files:**
- Create: `web/src/components/Layout.tsx`
- Create: `web/src/components/Player.tsx`
- Create: `web/src/pages/Login.tsx`
- Create: `web/src/pages/Library.tsx`
- Create: `web/src/pages/Playlists.tsx`
- Create: `web/src/pages/Settings.tsx`

- [ ] **Step 1: 创建 Layout 组件**

```typescript
// web/src/components/Layout.tsx
import { Outlet, Link, useLocation } from 'react-router-dom';
import { Music, ListMusic, Settings, PlayCircle } from 'lucide-react';
import Player from './Player';

export default function Layout() {
  const location = useLocation();

  const navs = [
    { path: '/', icon: Music, label: '音乐库' },
    { path: '/playlists', icon: ListMusic, label: '播放列表' },
    { path: '/settings', icon: Settings, label: '设置' }
  ];

  return (
    <div className="min-h-screen flex flex-col">
      <header className="h-14 bg-slate-900 flex items-center px-4">
        <h1 className="text-white font-bold text-lg flex items-center gap-2">
          <PlayCircle className="w-6 h-6" />
          Muses
        </h1>
      </header>

      <div className="flex-1 flex">
        <nav className="w-48 bg-slate-50 border-r p-2">
          {navs.map(nav => (
            <Link
              key={nav.path}
              to={nav.path}
              className={`flex items-center gap-2 px-3 py-2 rounded ${
                location.pathname === nav.path
                  ? 'bg-slate-200'
                  : 'hover:bg-slate-100'
              }`}
            >
              <nav.icon className="w-4 h-4" />
              {nav.label}
            </Link>
          ))}
        </nav>

        <main className="flex-1 p-4 pb-32">
          <Outlet />
        </main>
      </div>

      <Player />
    </div>
  );
}
```

- [ ] **Step 2: 创建 Player 组件**

```typescript
// web/src/components/Player.tsx
import { usePlayerStore } from '../stores/player';
import { Play, Pause, SkipBack, SkipForward } from 'lucide-react';

export default function Player() {
  const { currentSong, isPlaying, pause, resume, next, prev } = usePlayerStore();

  if (!currentSong) return null;

  const audioUrl = `/api/songs/${currentSong.id}/stream`;

  return (
    <div className="fixed bottom-0 left-0 right-0 h-24 bg-slate-900 border-t flex items-center px-4 gap-4">
      <audio
        src={audioUrl}
        autoPlay={isPlaying}
        onEnded={next}
        onPause={pause}
        onPlay={resume}
      />

      <div className="flex-1">
        <div className="text-white font-medium">{currentSong.title}</div>
        <div className="text-slate-400 text-sm">{currentSong.artist.name}</div>
      </div>

      <div className="flex items-center gap-2">
        <button onClick={prev} className="text-white p-2">
          <SkipBack className="w-5 h-5" />
        </button>
        <button
          onClick={isPlaying ? pause : resume}
          className="text-white p-2"
        >
          {isPlaying ? <Pause className="w-6 h-6" /> : <Play className="w-6 h-6" />}
        </button>
        <button onClick={next} className="text-white p-2">
          <SkipForward className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: 创建 Login 页面**

```typescript
// web/src/pages/Login.tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/client';
import { useAuthStore } from '../stores/auth';

export default function Login() {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const setAuth = useAuthStore(state => state.setAuth);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      const api = isRegister ? authApi.register : authApi.login;
      const { data } = await api(username, password);
      setAuth(data.token, data.user);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.error || '操作失败');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100">
      <form onSubmit={handleSubmit} className="bg-white p-8 rounded-lg shadow-md w-80">
        <h2 className="text-2xl font-bold mb-6 text-center">
          {isRegister ? '注册' : '登录'}
        </h2>

        {error && <div className="text-red-500 text-sm mb-4">{error}</div>}

        <input
          type="text"
          placeholder="用户名"
          value={username}
          onChange={e => setUsername(e.target.value)}
          className="w-full p-2 border rounded mb-4"
          required
        />

        <input
          type="password"
          placeholder="密码"
          value={password}
          onChange={e => setPassword(e.target.value)}
          className="w-full p-2 border rounded mb-6"
          required
        />

        <button type="submit" className="w-full bg-slate-900 text-white p-2 rounded">
          {isRegister ? '注册' : '登录'}
        </button>

        <button
          type="button"
          onClick={() => setIsRegister(!isRegister)}
          className="w-full text-sm text-slate-500 mt-4"
        >
          {isRegister ? '已有账号？登录' : '没有账号？注册'}
        </button>
      </form>
    </div>
  );
}
```

- [ ] **Step 4: 创建 Library 页面**

```typescript
// web/src/pages/Library.tsx
import { useEffect, useState } from 'react';
import { libraryApi } from '../api/client';
import { usePlayerStore } from '../stores/player';
import { Play, Heart, Disc } from 'lucide-react';

interface Artist {
  id: number;
  name: string;
  _count: { songs: number; albums: number };
}

interface Album {
  id: number;
  title: string;
  artist: { id: number; name: string };
  _count: { songs: number };
}

interface Song {
  id: number;
  title: string;
  duration: number;
  artist: { id: number; name: string };
  album: { id: number; title: string };
}

export default function Library() {
  const [activeTab, setActiveTab] = useState<'songs' | 'albums' | 'artists'>('songs');
  const [songs, setSongs] = useState<Song[]>([]);
  const [albums, setAlbums] = useState<Album[]>([]);
  const [artists, setArtists] = useState<Artist[]>([]);
  const { play, currentSong, isPlaying } = usePlayerStore();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    const [songsRes, albumsRes, artistsRes] = await Promise.all([
      libraryApi.getSongs(),
      libraryApi.getAlbums(),
      libraryApi.getArtists()
    ]);
    setSongs(songsRes.data);
    setAlbums(albumsRes.data);
    setArtists(artistsRes.data);
  };

  const formatDuration = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div>
      <div className="flex gap-2 mb-6">
        {(['songs', 'albums', 'artists'] as const).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 rounded ${
              activeTab === tab ? 'bg-slate-900 text-white' : 'bg-slate-100'
            }`}
          >
            {tab === 'songs' ? '歌曲' : tab === 'albums' ? '专辑' : '艺术家'}
          </button>
        ))}
      </div>

      {activeTab === 'songs' && (
        <div className="space-y-1">
          {songs.map(song => (
            <div
              key={song.id}
              className="flex items-center gap-3 p-2 hover:bg-slate-50 rounded cursor-pointer"
              onClick={() => play(song, songs)}
            >
              <button className="text-slate-400 hover:text-slate-900">
                {currentSong?.id === song.id && isPlaying ? (
                  <Play className="w-4 h-4" />
                ) : (
                  <Play className="w-4 h-4" />
                )}
              </button>
              <div className="flex-1">
                <div className="font-medium">{song.title}</div>
                <div className="text-sm text-slate-500">{song.artist.name}</div>
              </div>
              <div className="text-sm text-slate-400">
                {formatDuration(song.duration)}
              </div>
            </div>
          ))}
        </div>
      )}

      {activeTab === 'albums' && (
        <div className="grid grid-cols-4 gap-4">
          {albums.map(album => (
            <div key={album.id} className="p-3 border rounded hover:bg-slate-50">
              <div className="aspect-square bg-slate-200 rounded mb-2 flex items-center justify-center">
                <Disc className="w-12 h-12 text-slate-400" />
              </div>
              <div className="font-medium truncate">{album.title}</div>
              <div className="text-sm text-slate-500">{album.artist.name}</div>
            </div>
          ))}
        </div>
      )}

      {activeTab === 'artists' && (
        <div className="grid grid-cols-4 gap-4">
          {artists.map(artist => (
            <div key={artist.id} className="p-3 border rounded hover:bg-slate-50">
              <div className="aspect-square bg-slate-200 rounded-full mb-2 flex items-center justify-center">
                <span className="text-2xl">{artist.name[0]}</span>
              </div>
              <div className="font-medium text-center">{artist.name}</div>
              <div className="text-sm text-slate-500 text-center">
                {artist._count.albums} 张专辑 · {artist._count.songs} 首歌曲
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 5: 创建 Playlists 页面**

```typescript
// web/src/pages/Playlists.tsx
import { useEffect, useState } from 'react';
import { playlistsApi } from '../api/client';
import { Plus, Trash2 } from 'lucide-react';

interface Playlist {
  id: number;
  name: string;
  songs: { song: any }[];
  _count: { songs: number };
}

export default function Playlists() {
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [newName, setNewName] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  useEffect(() => {
    loadPlaylists();
  }, []);

  const loadPlaylists = async () => {
    const res = await playlistsApi.list();
    setPlaylists(res.data);
  };

  const createPlaylist = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;

    await playlistsApi.create(newName);
    setNewName('');
    setShowCreate(false);
    loadPlaylists();
  };

  const deletePlaylist = async (id: number) => {
    if (!confirm('确定删除这个播放列表？')) return;
    await playlistsApi.delete(id);
    loadPlaylists();
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold">播放列表</h2>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-1 bg-slate-900 text-white px-3 py-1 rounded"
        >
          <Plus className="w-4 h-4" />
          新建
        </button>
      </div>

      {showCreate && (
        <form onSubmit={createPlaylist} className="mb-4 flex gap-2">
          <input
            type="text"
            placeholder="播放列表名称"
            value={newName}
            onChange={e => setNewName(e.target.value)}
            className="flex-1 p-2 border rounded"
            autoFocus
          />
          <button type="submit" className="bg-slate-900 text-white px-4 py-2 rounded">
            创建
          </button>
          <button
            type="button"
            onClick={() => setShowCreate(false)}
            className="px-4 py-2 border rounded"
          >
            取消
          </button>
        </form>
      )}

      <div className="space-y-2">
        {playlists.map(playlist => (
          <div
            key={playlist.id}
            className="flex items-center gap-3 p-3 border rounded hover:bg-slate-50"
          >
            <div className="flex-1">
              <div className="font-medium">{playlist.name}</div>
              <div className="text-sm text-slate-500">
                {playlist._count.songs} 首歌曲
              </div>
            </div>
            <button
              onClick={() => deletePlaylist(playlist.id)}
              className="text-slate-400 hover:text-red-500"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        ))}

        {playlists.length === 0 && (
          <div className="text-center text-slate-400 py-8">
            暂无播放列表
          </div>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 6: 创建 Settings 页面**

```typescript
// web/src/pages/Settings.tsx
import { useState } from 'react';
import { libraryApi } from '../api/client';
import { useAuthStore } from '../stores/auth';
import { LogOut, RefreshCw } from 'lucide-react';

export default function Settings() {
  const [scanning, setScanning] = useState(false);
  const [message, setMessage] = useState('');
  const { logout, user } = useAuthStore();

  const handleScan = async () => {
    setScanning(true);
    setMessage('');
    try {
      const res = await libraryApi.scan();
      setMessage(`扫描完成：新增 ${res.data.added} 首，更新 ${res.data.updated} 首`);
    } catch (err: any) {
      setMessage('扫描失败：' + (err.response?.data?.error || err.message));
    }
    setScanning(false);
  };

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">设置</h2>

      <div className="mb-6">
        <h3 className="font-medium mb-2">当前用户</h3>
        <div className="text-slate-600">{user?.username}</div>
      </div>

      <div className="mb-6">
        <h3 className="font-medium mb-2">音乐库</h3>
        <button
          onClick={handleScan}
          disabled={scanning}
          className="flex items-center gap-2 bg-slate-900 text-white px-4 py-2 rounded disabled:opacity-50"
        >
          <RefreshCw className={`w-4 h-4 ${scanning ? 'animate-spin' : ''}`} />
          {scanning ? '扫描中...' : '扫描音乐库'}
        </button>
        {message && <div className="mt-2 text-sm text-slate-600">{message}</div>}
      </div>

      <div className="border-t pt-6">
        <button
          onClick={logout}
          className="flex items-center gap-2 text-red-500 hover:text-red-600"
        >
          <LogOut className="w-4 h-4" />
          退出登录
        </button>
      </div>
    </div>
  );
}
```

- [ ] **Step 7: 创建 Web 构建配置**

```json
// web/package.json (添加 build 脚本)
{
  "scripts": {
    "build": "tsc && vite build",
    // ...
  }
}
```

### Task 15: 部署脚本与配置

```bash
git add web/
git commit -m "feat(web): add React UI with all pages"
```

---

## Phase 4: 安卓客户端（2-3周）

### Task 9: 初始化 Kotlin 项目

**Files:**
- Create: `android/build.gradle.kts`
- Create: `android/settings.gradle.kts`
- Create: `android/gradle.properties`
- Create: `android/app/build.gradle.kts`
- Create: `android/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 创建 Gradle 配置**

```kotlin
// android/settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Muses"
include(":app")
```

```kotlin
// android/build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
    id("com.google.devtools.ksp") version "1.9.21-1.0.15" apply false
}
```

```properties
# android/gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

```kotlin
// android/app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.muses"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.muses"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.6"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Media
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-session:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")

    // Storage
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DI
    implementation("com.google.dagger:hilt-android:2.48.1")
    ksp("com.google.dagger:hilt-compiler:2.48.1")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 2: 创建 AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <application
        android:name=".MusesApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Muses">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Muses">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".player.PlayerService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="false">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>

    </application>

</manifest>
```

- [ ] **Step 3: 创建 Application 类**

```kotlin
// android/app/src/main/java/com/muses/MusesApp.kt
package com.muses

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MusesApp : Application()
```

- [ ] **Step 4: 创建 MainActivity**

```kotlin
// android/app/src/main/java/com/muses/MainActivity.kt
package com.muses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.muses.ui.MusesNavHost
import com.muses.ui.theme.MusesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusesNavHost()
                }
            }
        }
    }
}
```

- [ ] **Step 5: 创建 API 客户端**

```kotlin
// android/app/src/main/java/com/muses/api/ApiClient.kt
package com.muses.api

import com.muses.data.model.*
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("auth/me")
    suspend fun getMe(): User

    @GET("songs")
    suspend fun getSongs(): List<SongDto>

    @GET("artists")
    suspend fun getArtists(): List<ArtistDto>

    @GET("albums")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("playlists")
    suspend fun getPlaylists(): List<PlaylistDto>

    @POST("playlists")
    suspend fun createPlaylist(@Body name: String): PlaylistDto

    @GET("favorites")
    suspend fun getFavorites(): List<FavoriteDto>

    @POST("favorites/{songId}")
    suspend fun addFavorite(@Path("songId") songId: Int)

    @DELETE("favorites/{songId}")
    suspend fun removeFavorite(@Path("songId") songId: Int)
}

data class LoginRequest(val username: String, val password: String)
data class RegisterRequest(val username: String, val password: String)
data class AuthResponse(val token: String, val user: User)
```

- [ ] **Step 6: 创建数据模型**

```kotlin
// android/app/src/main/java/com/muses/data/model/Models.kt
package com.muses.data.model

data class User(val id: Int, val username: String)

data class SongDto(
    val id: Int,
    val title: String,
    val duration: Int,
    val artist: ArtistDto,
    val album: AlbumDto
) {
    fun getStreamUrl(baseUrl: String) = "$baseUrl/api/songs/$id/stream"
}

data class ArtistDto(val id: Int, val name: String)
data class AlbumDto(val id: Int, val title: String, val artist: ArtistDto)

data class PlaylistDto(
    val id: Int,
    val name: String,
    val songs: List<PlaylistSongDto>
)

data class PlaylistSongDto(val song: SongDto)

data class FavoriteDto(val song: SongDto)
```

- [ ] **Step 7: 创建 Repository**

```kotlin
// android/app/src/main/java/com/muses/data/repository/MusicRepository.kt
package com.muses.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.muses.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "muses_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService
) {
    companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    suspend fun login(username: String, password: String): AuthResponse {
        val response = api.login(LoginRequest(username, password))
        saveToken(response.token)
        return response
    }

    suspend fun register(username: String, password: String): AuthResponse {
        val response = api.register(RegisterRequest(username, password))
        saveToken(response.token)
        return response
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.map { it[TOKEN_KEY] }.first()
    }

    suspend fun logout() {
        context.dataStore.edit { it.remove(TOKEN_KEY) }
    }

    private suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }
}

@Singleton
class MusicRepository @Inject constructor(private val api: ApiService) {
    suspend fun getSongs() = api.getSongs()
    suspend fun getArtists() = api.getArtists()
    suspend fun getAlbums() = api.getAlbums()
    suspend fun getPlaylists() = api.getPlaylists()
    suspend fun createPlaylist(name: String) = api.createPlaylist(name)
    suspend fun getFavorites() = api.getFavorites()
    suspend fun addFavorite(songId: Int) = api.addFavorite(songId)
    suspend fun removeFavorite(songId: Int) = api.removeFavorite(songId)
}
```

- [ ] **Step 8: 创建 DI 模块**

```kotlin
// android/app/src/main/java/com/muses/di/NetworkModule.kt
package com.muses.di

import android.content.Context
import com.muses.api.ApiService
import com.muses.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // TODO: 修改为你的 NAS 地址
    private const val BASE_URL = "http://192.168.1.x:3000/"

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val token = runBlocking { AuthRepository(context, null).getToken() }
                val request = chain.request().newBuilder().apply {
                    token?.let { addHeader("Authorization", "Bearer $it") }
                }.build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
```

- [ ] **Step 9: 提交代码**

```bash
git add android/
git commit -m "feat(android): initialize Kotlin Android project with Compose"
```

---

### Task 10: 安卓 UI 开发

**Files:**
- Create: `android/app/src/main/java/com/muses/ui/theme/Theme.kt`
- Create: `android/app/src/main/java/com/muses/ui/screens/LoginScreen.kt`
- Create: `android/app/src/main/java/com/muses/ui/screens/LibraryScreen.kt`
- Create: `android/app/src/main/java/com/muses/ui/screens/PlayerScreen.kt`
- Create: `android/app/src/main/java/com/muses/player/PlayerService.kt`

- [ ] **Step 1: 创建主题**

```kotlin
// android/app/src/main/java/com/muses/ui/theme/Theme.kt
package com.muses.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1DB954),
    secondary = Color(0xFF1DB954),
    tertiary = Color(0xFF535353),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1DB954),
    secondary = Color(0xFF1DB954),
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun MusesTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

- [ ] **Step 2: 创建导航**

```kotlin
// android/app/src/main/java/com/muses/ui/MusesNavHost.kt
package com.muses.ui

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muses.ui.screens.LoginScreen
import com.muses.ui.screens.MainScreen

@Composable
fun MusesNavHost() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = {
                isLoggedIn = true
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("main") {
            MainScreen(onLogout = {
                isLoggedIn = false
                navController.navigate("login") {
                    popUpTo("main") { inclusive = true }
                }
            })
        }
    }
}
```

- [ ] **Step 3: 创建登录界面**

```kotlin
// android/app/src/main/java/com/muses/ui/screens/LoginScreen.kt
package com.muses.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var isRegister by mutableStateOf(false)

    suspend fun login(username: String, password: String): Boolean {
        isLoading = true
        error = null
        return try {
            authRepository.login(username, password)
            true
        } catch (e: Exception) {
            error = e.message
            false
        } finally {
            isLoading = false
        }
    }

    suspend fun register(username: String, password: String): Boolean {
        isLoading = true
        error = null
        return try {
            authRepository.register(username, password)
            true
        } catch (e: Exception) {
            error = e.message
            false
        } finally {
            isLoading = false
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Muses",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth()
        )

        if (viewModel.error != null) {
            Text(
                text = viewModel.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.viewModelScope.launch {
                    val success = if (viewModel.isRegister) {
                        viewModel.register(username, password)
                    } else {
                        viewModel.login(username, password)
                    }
                    if (success) onLoginSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading
        ) {
            Text(if (viewModel.isRegister) "注册" else "登录")
        }

        TextButton(
            onClick = { viewModel.isRegister = !viewModel.isRegister }
        ) {
            Text(if (viewModel.isRegister) "已有账号？登录" else "没有账号？注册")
        }
    }
}
```

- [ ] **Step 4: 创建主界面（包含音乐库和播放器）**

```kotlin
// android/app/src/main/java/com/muses/ui/screens/MainScreen.kt
package com.muses.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.data.model.SongDto
import com.muses.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    var songs by mutableStateOf<List<SongDto>>(emptyList())
    var isLoading by mutableStateOf(true)
    var currentSong by mutableStateOf<SongDto?>(null)
    var isPlaying by mutableStateOf(false)

    init {
        loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            isLoading = true
            songs = musicRepository.getSongs()
            isLoading = false
        }
    }

    fun playSong(song: SongDto) {
        currentSong = song
        isPlaying = true
    }

    fun togglePlay() {
        isPlaying = !isPlaying
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            Column {
                // Mini player
                viewModel.currentSong?.let { song ->
                    MiniPlayer(
                        song = song,
                        isPlaying = viewModel.isPlaying,
                        onPlayPause = { viewModel.togglePlay() }
                    )
                }

                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MusicNote, "Library") },
                        label = { Text("音乐库") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, "Playlists") },
                        label = { Text("播放列表") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, "Settings") },
                        label = { Text("设置") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> LibraryTab(viewModel, Modifier.padding(padding))
            1 -> PlaylistsTab(Modifier.padding(padding))
            2 -> SettingsTab(onLogout, Modifier.padding(padding))
        }
    }
}

@Composable
fun MiniPlayer(
    song: SongDto,
    isPlaying: Boolean,
    onPlayPause: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyMedium)
                Text(song.artist.name, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun LibraryTab(viewModel: LibraryViewModel, modifier: Modifier = Modifier) {
    if (viewModel.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(viewModel.songs) { song ->
                ListItem(
                    headlineContent = { Text(song.title) },
                    supportingContent = { Text(song.artist.name) },
                    leadingContent = {
                        IconButton(onClick = { viewModel.playSong(song) }) {
                            Icon(Icons.Default.PlayArrow, "Play")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistsTab(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("播放列表功能开发中")
    }
}

@Composable
fun SettingsTab(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Button(onClick = onLogout) {
            Text("退出登录")
        }
    }
}
```

- [ ] **Step 5: 创建播放器服务**

```kotlin
// android/app/src/main/java/com/muses/player/PlayerService.kt
package com.muses.player

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
```

- [ ] **Step 6: 提交代码**

```bash
git add android/
git commit -m "feat(android): add UI screens and player service"
```

---

## Phase 5: 部署与测试（1周）

### Task 11: Docker Compose 部署配置

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: 创建 docker-compose.yml**

```yaml
version: '3.8'

services:
  music-api:
    build: ./backend
    container_name: muses-api
    ports:
      - "3000:3000"
    volumes:
      - /path/to/your/music:/music:ro
      - ./data/database:/app/data
      - ./data/cache:/app/cache
    environment:
      - NODE_ENV=production
      - JWT_SECRET=change-this-to-a-secure-random-string
      - DATABASE_URL=file:./data/database.db
      - MUSIC_PATH=/music
      - TRANSCODE_CACHE_PATH=/app/cache
      - PORT=3000
    restart: unless-stopped

volumes:
  data:

networks:
  default:
    name: muses-network
```

- [ ] **Step 3: 创建部署说明 README**

```markdown
# Muses 部署指南

## 前置要求
1. 飞牛 NAS 已安装 Docker
2. 音乐文件存放在 `/path/to/your/music`

## 部署步骤

1. 克隆项目
```bash
git clone <your-repo-url>
cd muses
```

2. 修改配置
- 编辑 `docker-compose.yml` 中的：
  - `/path/to/your/music` - 你的音乐目录
  - `JWT_SECRET` - 安全密钥

3. 启动服务
```bash
docker-compose up -d
```

4. 访问
- Web 管理界面: http://your-nas-ip:80
- API: http://your-nas-ip:3000

5. 首次使用
- 打开 Web 界面注册账号
- 进入设置 → 扫描音乐库
```

- [ ] **Step 4: 提交代码**

```bash
git add docker-compose.yml
git commit -m "feat: add Docker Compose deployment configuration"
```

---

## 总结

此实施计划涵盖：

1. **后端核心** (1-2周)
   - Node.js + Express + Prisma + SQLite
   - 用户认证
   - 音乐库扫描与元数据解析
   - 播放列表与收藏

2. **音频功能** (1周)
   - FFmpeg 转码
   - 流媒体播放
   - 转码缓存

3. **Web 管理界面** (1-2周)
   - React + Vite + Tailwind
   - 登录/注册
   - 音乐库浏览
   - 播放列表管理
   - 设置页面

4. **安卓客户端** (2-3周)
   - Kotlin + Jetpack Compose
   - ExoPlayer 播放
   - 登录/注册
   - 音乐库浏览
   - 播放器

5. **部署** (1周)
   - Docker Compose
   - NAS 部署
