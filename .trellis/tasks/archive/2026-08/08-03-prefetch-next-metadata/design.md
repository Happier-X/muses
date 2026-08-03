# 设计：WebDAV 下一首元信息预取

## 边界

| 层 | 职责 |
|----|------|
| `queue.peekNext` | 无副作用解析下一首（已有） |
| `controller.prefetchNextTrack` | 统一调度：WebDAV 音频预取 + **元信息预取** |
| `lyrics` / `cover` / `metadata` match API | 只返回命中，不写库、不碰 playerState（已有） |
| `library.storage.upsertSong` | 预取成功写库 |
| `playerState` / mediaSession | **预取路径不写**；切歌仍由 `playSong` 读库 |

## 核心思路

1. **库是中转站**：预取只 upsert 下一首 `SongItem`；真正切歌时现有 `playSong` 读库上屏。
2. **隔离 token**：新增 `metadataPrefetchToken`（或 per-song generation），**禁止**递增/复用 `lyricsMatchToken` / `onlineCoverToken` / `onlineTextToken`，以免掐断当前曲匹配。
3. **不复用写 state 的 ForSong helper**：抽取或平行实现 `prefetch*ForLibrary(song, token)`：校验 token + 目标 `song.id`，只 upsert，不改 `state.*`。
4. **范围**：仅 `next.sourceType === 'webdav' && next.id !== currentSongId`；与音频预取同 gate（音频另需密码；元信息在线匹配一般不需 WebDAV 密码，封面下载走 `cacheRemoteCover`）。

## 数据流

```
playSong → playing
  └─ void prefetchNextTrack(currentId)
       ├─ peekNext()
       ├─ [webdav] void prefetchWebDavAudioFile(...)   // 已有
       └─ [webdav] void prefetchNextMetadata(next, token)
            ├─ matchOnlineLyrics → shouldPersistOnlineLyrics? upsert lyrics*
            ├─ 无安全 cover? matchOnlineCoverRemote → cacheRemoteCover → upsert coverUri
            └─ needsOnlineTextMeta? matchOnlineTextMeta → merge → upsert title/artist/album
```

队列变更 → `reschedulePrefetchAfterQueueChange` → 再次 `prefetchNextTrack`（新 token 作废旧写库）。

## 写库策略（对齐现有）

- **歌词**：`shouldPersistOnlineLyrics`；tags 带 `lyricsSource: 'online'` + `lyricsFormat`；保留已有 cover/扫描字段，避免 upsert 抹掉。
- **封面**：`toSafeCoverUri`；已有安全封面 skip；禁止 data:/远程 URL 入库。
- **文本**：`needsOnlineTextMeta` + `mergeTextMetaFillEmpty`；强 title / 已有 artist·album 不覆盖。

## 并发与串曲

- 每次调度 `metadataPrefetchToken++`，启动时捕获 `token` 与 `targetSongId`。
- 每个 await 之后：`token !== metadataPrefetchToken` 或目标 id 已不是「本次 next」则 return（可选：仍允许 upsert 到 **该 songId 条目** 若认为库侧无害——**推荐更严：token 过期即丢弃**，避免队列已指向别的 next 时浪费写与竞态）。
- **推荐策略**：token 过期丢弃整次写库；同一 `songId` 若再次成为 next 会重新调度。
- 三路 match 可 `Promise.all` 并行；单路 catch 吞掉，互不影响。

## 与当前曲匹配关系

- 预取与当前曲 match **并行**；共享进程内负缓存/LRU 可接受（已有 cover/lyrics 负缓存）。
- 不共享写 state 的 token。
- 若预取写库的 song 稍后成为 current，`playSong` 读到升级后的库数据；若当前曲 match 仍在飞，仍按 current token 更新 state（可能与库双写，质量规则一致则无害）。

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 误写 playerState | 预取路径零 `state.lyrics/coverUri` 赋值；单测锁定 |
| 掐断当前 match | 独立 token |
| 封面 cache 占磁盘 | 沿用现有 `cacheRemoteCover` 与仅补缺 |
| 网络与当前曲争用 | 仅下一首一条；失败静默；C 范围已收窄 |
| controller 膨胀 | 预取写库逻辑可收拢为文件内 private helpers，避免复制 paste 过大时可抽 `prefetchMetadata.ts` |

## 兼容

- 不改 `peekNext` 语义、不改音频预取契约。
- 不改 HPopup / UI。
- 本地下一首行为与今日一致（切歌后再 match）。

## 回滚

- 去掉 `prefetchNextMetadata` 调用即回退到仅音频预取；写库数据可保留无害。
