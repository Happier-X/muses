# 下一首元信息预加载

## Goal

在现有「下一首 WebDAV 音频预取」同一调度范围与时机上，为 **WebDAV 下一首** 预取在线歌词、封面、标题/艺术家/专辑等到曲库，使切到该曲时 `playSong` 读库即可尽量带齐展示数据，减少空封面、空歌词与弱标题闪烁。

## 背景（仓库已确认）

### 现有音频预取

- `prefetchNextTrack(currentSongId)`：`peekNext()` → 仅 WebDAV 且 `id !== current` → `prefetchWebDavAudioFile`。
- 触发：`playSong` 成功 `playing`；队列 / `setRepeatMode` / `toggleShuffle` 变更后 `reschedulePrefetchAfterQueueChange`（playing/paused）。
- 失败静默；密码不进 state/日志。

### 现有当前曲元信息（切歌后）

- 库内字段立刻上屏；异步 `matchOnlineLyricsForSong`、`scanSongMetadata` → 封面/文本在线补缺。
- Helper **硬绑定** `state.currentSong?.id` 与全局 token；预取不可直接复用写 state 路径。

### 历史

- 任务 `07-12-prefetch-next-track` 明确 Out of Scope「在线歌词预取」；本任务补齐该缺口，范围仍限 WebDAV 下一首。

## Decisions

| 决策 | 结论 |
|------|------|
| MVP 深度 | **C**：仅 **WebDAV 下一首** 的 **在线** 元信息补缺/升级写库 |
| 不包含 | 本地下一首元信息预取；下一首文件内嵌标签预扫（`scanSongMetadata`） |
| 与音频预取关系 | 同调度点并行；互不阻塞；均可失败静默 |
| 写库 | 是；切歌仍走现有 `playSong` 读库，**禁止**预取写当前 `playerState` |
| 歌词 | 对齐当前曲：可发起在线匹配；**仅** `shouldPersistOnlineLyrics` 为真时写库（严格更优） |
| 封面 | 仅当库内无安全 `coverUri` 时匹配并 `cacheRemoteCover` 后写安全 URI |
| 文本 | `needsOnlineTextMeta` 为真时匹配；`mergeTextMetaFillEmpty` 后写回 |
| 单曲循环自身 | 不预取（与音频一致） |
| N+2 / 整队 | 不做 |

## Requirements

1. **R1** 在 `prefetchNextTrack`（或等价统一入口）中，当 `peekNext()` 为 WebDAV 且非当前曲时，除音频预取外调度元信息预取。
2. **R2** 元信息预取至少覆盖：在线歌词、在线封面、在线文本（title/artist/album 按现有补缺规则）。
3. **R3** 成功结果 **upsert 写回曲库**；策略与当前曲一致（歌词质量序、封面仅安全 URI、文本仅补空/弱 title）。
4. **R4** 预取 **不得** 修改当前曲 `playerState` 的 lyrics/cover/title 等展示字段，也不得因共用全局 token 中断当前曲匹配。
5. **R5** 使用独立 prefetch generation / songId 校验；队列变更或当前曲变化后，过期预取结果不得写库（或写库前再次确认目标仍是「当时的 next id」且条目仍存在）。
6. **R6** 失败静默；不阻塞播放、切歌、音频预取。
7. **R7** 本地下一首、空队列、单曲循环自身：不跑元信息预取。
8. **R8** 禁止改 `node_modules`；密码/远程 URL 不进 reactive state / 日志 / 不当持久化。
9. **R9** 同步 `.trellis/spec/frontend/features-player.md` 预取契约。
10. **R10** 为可抽纯函数/调度边界补充合理单测（generation 丢弃、非 webdav 跳过、不写 playerState）。

## Acceptance Criteria

- [ ] AC1：播放 WebDAV 队列中曲目且下一首为另一首 WebDAV 时，会在后台尝试预取该曲歌词/封面/文本并在成功时写库。
- [ ] AC2：预取进行中，当前曲歌词/封面/进度/播放状态不被下一首结果覆盖。
- [ ] AC3：快速切歌或队列变更后，过期预取不得把错误歌曲的歌词/封面写入错误条目（token/generation 生效）。
- [ ] AC4：本地下一首、单曲循环自身、空队列不触发元信息预取。
- [ ] AC5：预取失败不影响当前播放与音频预取。
- [ ] AC6：切到已预取成功的下一首时，`playSong` 首帧可从库读到已写字段（网络曾成功前提下）。
- [ ] AC7：lint/typecheck（及新增单测）通过；player spec 已更新。

## Out of Scope

- 本地下一首在线预取
- 下一首 `scanSongMetadata` / 内嵌标签预扫
- N+2、整队预热
- 迷你条「即将播放」UI、媒体会话提前显示下一首
- 改在线源优先级算法
- 用户开关 / 仅 Wi-Fi 预取

## Task Type

Complex — 需 `design.md` + `implement.md`。
