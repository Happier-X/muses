# 设计：amll-ttml-db 歌词匹配

## 边界

| 层 | 职责 |
|----|------|
| `src/features/lyrics/`（新建） | 索引拉取/缓存、打分匹配、TTML 拉取、运行时缓存 |
| `src/features/player/controller.ts` | 切歌时触发匹配；写 `playerState` 展示歌词与状态 |
| `src/views/PlayerPage.vue` | 按格式 `parseLrc` / `parseTTML`；空态/匹配中文案 |
| CDN | 只读 jsDelivr；无自建后端 |

不改：扫描入库、`SongItem` schema、native 元数据读取。

## 数据流

```
playSong(song)
  → 先设 state.lyrics = 本地 lyrics（可空）
  → state.onlineLyricsStatus = 'matching'
  → matchOnlineLyrics(song)  // async, tokenized by songId
       1. ensureIndex()      // 内存缓存 raw-lyrics-index.jsonl
       2. best = score(title, artist, album?)
       3. if best: fetch TTML → cache[songId]
       4. if still current song:
            成功 → state.lyrics = ttmlText; state.lyricsFormat = 'ttml'; status = 'ready'
            失败 → 保持本地; format = 'lrc'|null; status = 'miss'|'error'
  → PlayerPage:
       format==='ttml' → parseTTML(lyrics).lines
       else → parseLrc(normalizeLrc(lyrics))
```

## 合约

### player 状态扩展（建议）

```ts
// PlayerState 增量
lyricsFormat: 'lrc' | 'ttml' | null
onlineLyricsStatus: 'idle' | 'matching' | 'ready' | 'miss' | 'error'
```

- `lyrics` 仍为展示用字符串（LRC 或 TTML 原文）。
- `lyricsFormat` 决定解析器；切歌时重置。
- 不扩展 `SongItem.lyricsSource`（不写回）。

### 匹配 API（建议）

```ts
// features/lyrics/amllTtmlDb.ts
matchAmllTtmlLyrics(query: {
  songId: string
  title: string
  artist?: string
  album?: string
}): Promise<{
  ok: true
  ttml: string
  rawLyricFile: string
  score: number
} | {
  ok: false
  reason: 'no-match' | 'network' | 'parse' | 'aborted'
}>
```

### 打分（MVP）

1. 规范化：小写、去空白、去常见括号后缀 `(Live)` / `「」` 等。
2. 歌名：完全相等 > 包含/被包含 > 否。
3. 歌手：任一 artist token 命中加分；无歌手信息不重罚。
4. 专辑：可选加分。
5. 阈值：歌名至少「包含」级，否则视为无匹配。

### CDN

- Index: `https://cdn.jsdelivr.net/gh/amll-dev/amll-ttml-db@main/metadata/raw-lyrics-index.jsonl`
- File:  `https://cdn.jsdelivr.net/gh/amll-dev/amll-ttml-db@main/raw-lyrics/<rawLyricFile>`
- 超时与失败：`fetch` + 超时；失败 `reason: 'network'`，不抛到 UI 层。

### 缓存

| 缓存 | 键 | 生命周期 |
|------|-----|----------|
| 索引 | 单例内存 | 进程内；可选 sessionStorage 减少冷启动（MVP 可仅内存） |
| TTML | songId → { ttml, rawLyricFile } | 进程内 Map；切歌不丢，便于切回 |
| 负缓存 | songId → miss | 短时，避免同曲狂刷 |

### 竞态

- `playSong` 递增 `lyricsMatchToken`；回调仅当 `token` 与 `currentSong.id` 一致时写 state。

## 展示

| 条件 | 行为 |
|------|------|
| matching + 有本地 | 先显示本地 LRC |
| matching + 无本地 | 「正在匹配在线歌词…」 |
| ready + ttml | parseTTML → LyricPlayer |
| miss/error + 有本地 | 本地 LRC |
| miss/error + 无本地 | 空态：说明未匹配到在线歌词，也无本地歌词 |

## 权衡

| 方案 | 取舍 |
|------|------|
| 运行时缓存 vs 写回曲库 | 已选运行时：简单、不污染 schema；代价是冷启动重拉 |
| 自动最佳 vs 多候选 | 已选自动：零 UI；错配风险接受 |
| 在线优先 | TTML 质量更好；误匹配盖本地 → 后续可加回退开关 |

## 回滚

- 功能开关：删除/旁路 `matchAmllTtmlLyrics` 调用即可回退到纯本地链路。
- 无数据迁移。
