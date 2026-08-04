# 沉浸页 mode-bar 更多菜单与编辑歌曲信息

## Goal

在沉浸式播放页 mode-bar 的播放队列旁增加「更多」图标按钮；点击弹出仅含「编辑歌曲信息」的 `HBottomSheet`；表单可编辑 **title / artist / album / 封面 / 歌词 / ReplayGain（track dB）**。保存时**必写曲库**并刷新当前展示；**尽力**写回音频文件内嵌标签（失败不回滚库、须提示）。用户手改字段**永久**不被懒扫 / 在线补缺 / 扫描覆盖。

## Background

- mode-bar 现为循环 / 随机 / 队列三键（`PlayerPage.vue`）。
- `SongsPage` 已有 `ellipsisVertical` + `HBottomSheet` 歌曲操作，**无**编辑入口。
- `SongItem` 已含 title/artist/album/coverUri/lyrics/lyricsSource/lyricsFormat/replayGainTrackDb。
- 原生仅有 `AudioMetadataReader`（jaudiotagger 3.0.1），**无**写标签 API；本地为 SAF `content://`，WebDAV 写文件需缓存后 PUT。
- **RG = ReplayGain**（`replayGainTrackDb`）；设置「音量均衡」为总开关，本任务编辑单曲 dB。

## Decisions（已全部收敛）

| ID | 决策 |
|----|------|
| D1 | 深度 C：表单 + 写库 + 尽力写文件内嵌标签 |
| D2 | BottomSheet **仅**「编辑歌曲信息」+ 取消（不含加入歌单/加队列） |
| D3 | 更多键随有曲面板出现（empty-state 无 mode-bar） |
| D4 | 任意音源**必写库**；文件**尽力**；失败**不回滚库**、须非静默提示 |
| D5 | 字段：title / artist / album / 封面 / 歌词 / ReplayGain track dB |
| D6 | 手改字段**永久保护**（含 cover / lyrics / replayGain） |

## Requirements

- **R1**：mode-bar 队列键旁增加「更多」`HButton ghost` 圆图标（`ellipsisVertical`），`aria-label` 如「更多」。
- **R2**：点击打开 `HBottomSheet`，标题「歌曲操作」（或「更多」）；菜单项仅「编辑歌曲信息」与「取消」。
- **R3**：选择「编辑歌曲信息」打开编辑 UI（第二层 sheet / 全高 sheet / 内嵌表单，design 定），预填当前曲字段。
- **R4**：可编辑并保存：title（必填非空 trim）、artist、album、封面、歌词正文、ReplayGain track dB（合法数字或清空）。
- **R5**：保存成功路径：校验 → 更新 `SongItem`（含 `userEditedFields` 保护标记）→ `upsert`/持久化 → 若为当前曲则 `syncDisplayStateFromSong`、媒体会话文本/封面、歌词展示、RG 变化且音量均衡开启时 `setVolume` → **再**尽力写文件标签。
- **R6（D4）**：文件写失败不撤销库；Toast/文案区分「全部成功」与「已更新曲库，写入文件失败」；禁止完全静默。
- **R7（D6）**：凡用户本次或历史保存过的字段，`scanSongMetadata`、全量/增量扫描 upsert、在线封面/歌词/文本补缺、下一首元信息预取写库，**均不得覆盖**对应受保护字段。
- **R8**：沉浸 ghost 按下态沿用 `.player-overlay .h-button--ghost`；四键 mode-bar 窄屏仍可用（可略增 max-width）。
- **R9**：表单用 `@tanstack/vue-form` + `HInput` / `HTextarea`；不改 happier-ui 库默认、不改 MiniPlayer、不改播放状态机核心语义（仅增加编辑写库与展示同步）。
- **R10**：禁止全库 EBU 测响度；禁止把未解析的假 RG 当标签；用户显式输入的 dB（含 0）视为有效手改。

## Acceptance Criteria

- [ ] AC1：有当前曲时 mode-bar 在队列旁可见「更多」。
- [ ] AC2：点击弹出 BottomSheet，仅见「编辑歌曲信息」与取消（无加入歌单/加队列）。
- [ ] AC3：可编辑并保存 title/artist/album/封面/歌词/RG；曲库持久化；当前曲 UI（含 MiniPlayer 若展示同源）与媒体会话在适用时更新；RG 变更在均衡开启时影响音量。
- [ ] AC4：local/webdav 在能力允许时文件侧尽力写入；失败时库仍为新值且有明确提示。
- [ ] AC5：手改字段后，懒扫与在线封面/歌词/文本补缺不再覆盖这些字段。
- [ ] AC6：empty 无当前曲时不出现更多键（随面板）。
- [ ] AC7：`component-guidelines` / `features-player` / library 相关规范与实现一致。

## Out of Scope

- MiniPlayer 更多菜单；SongsPage 列表同步加「编辑」（可后续复用服务，本任务不强制）
- 加入歌单 / 添加到队列（D2）
- 曲目号、年份、批量编辑、撤销、清除手改保护 UI
- 文件失败事务回滚 / 双相位提交
- iOS 写标签
- 全库响度扫描

## Risks

- SAF 写回与 WebDAV PUT 失败面大（D4 已接受库/文件分裂）
- 封面选图与嵌入、歌词 format、RG TXXX 写兼容
- 播放中写正在播放文件可能失败（提示即可）
- mode-bar 四键布局
- `userEditedFields` 遗漏任一写库路径会导致手改被盖

## 非目标说明

- 「RG」仅指 ReplayGain，不是 ripgrep。
