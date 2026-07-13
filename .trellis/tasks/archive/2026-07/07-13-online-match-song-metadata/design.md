# 设计：在线匹配封面（iTunes + kw）

## 边界

| 层 | 职责 |
|----|------|
| `src/features/cover/`（新建） | 查询、多源编排、负缓存、类型 |
| `src/features/cover/providers/itunes.ts` | iTunes Search → artwork URL |
| `src/features/cover/providers/kw.ts` | 移植 any-listen kw 的 search + getPic |
| 原生 bridge（扩展现有或薄封装） | HTTP 下载图片 → `cache/covers/{sha}.jpg` → 返回 `file://` |
| `controller.ts` | 本地扫描后触发；token；写回与 UI/媒体会话同步 |
| 不改 | amll 歌词、seek、队列、capgo |

## 数据流

```
playSong(song)
  → 播放成功
  → scanSongMetadata（本地标签，可写 cover）
  → if 当前曲仍无 safe coverUri:
       matchOnlineCover(song, token)
         → iTunes search(title, artist[, album])
         → if artworkUrl: downloadToCache → localUri
         → else kw musicSearch → getPic → downloadToCache → localUri
         → if token ok && still current:
              upsertSong({ coverUri: localUri })
              syncDisplayStateFromSong
              syncMediaSessionSong
```

## 合约（示意）

```ts
// features/cover/types.ts
export type OnlineCoverQuery = {
  songId: string
  title: string
  artist?: string
  album?: string
}

export type OnlineCoverMatchResult =
  | { ok: true; remoteUrl: string; source: 'itunes' | 'kw' }
  | { ok: false; reason: 'no-match' | 'network' | 'aborted' }

// 编排：只返回远程 URL；落盘由 controller/native
export function matchOnlineCoverRemote(query: OnlineCoverQuery): Promise<OnlineCoverMatchResult>

// 原生：下载并返回安全 file URI
// AudioPlayerBridge.cacheRemoteCover?.({ url, cacheKey }) => { uri: string | null }
```

### iTunes

- `GET https://itunes.apple.com/search?term=...&entity=song&limit=5`
- 取最佳 track 的 `artworkUrl100`，替换为更大尺寸（如 `600x600`）
- 无密钥

### kw（移植要点）

- 搜索：`search.kuwo.cn/r.s?...&all={query}&...`（与扩展同源思路）
- 取图：`artistpicserver.kuwo.cn/pic.web?...&rid={id}` → 若 body 为 http URL 则采用
- 使用前端/原生 HTTP；注意 Android cleartext：优先 https 替换（扩展已有 kwcdn → kuwo.cn + https 逻辑）
- **不**拷贝 GPL 的 music-tag-web 代码；kw 参考 Apache-2.0 的 any-listen 扩展并注明出处

### 落盘

- 复用 `cache/covers` + sha256(cacheKey) 命名，与 `AudioMetadataReader.writeCover` 一致
- cacheKey 建议 `online:{songId}` 或 `online:{source}:{remoteUrl}`，避免与内嵌封面互相覆盖时可区分

### 并发与缓存

- `onlineCoverToken` 随 playSong 递增
- 进程内负缓存：`songId + queryKey` 短 TTL（如 30–60min）
- 已有 cover 直接 return

## 安全与合规

- 不写密码/日志里的敏感头
- `coverUri` 经 `toSafeCoverUri` / storage 校验
- 国内源：非官方接口，隔离在 provider 内，失败降级

## 风险

| 风险 | 缓解 |
|------|------|
| iTunes 华语 miss | kw 回退 |
| kw 接口变更 | 捕获失败；不阻塞播放 |
| cleartext HTTP | 尽量 https；Android 已有 cleartext 配置但优先 https |
| 误匹配封面 | 仅补缺；可后续加手动纠正 |
| 串曲 | token + currentSong.id |

## 回滚

- 去掉 controller 触发与 `features/cover` 即可恢复；已写封面可保留在库中。
