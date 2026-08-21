# 歌曲信息与歌词改用内嵌/同目录歌词文件优先

## Goal

歌曲的信息（标题/歌手/专辑/封面）和歌词改为仅使用本地来源——音频内嵌 tag / 内嵌歌词 / 同文件夹同名 `.lrc` 文件；播放器完全停止自动在线匹配，在线补全能力统一收敛到刮削页（用户主动操作）。

## 背景（代码证据）

- 扫描已读取内嵌 tag：`src/features/library/tags.ts` `normalizeTags`，有值字段标 `metaSources.* = 'embedded'`。
- 同目录同名 `.lrc`（sidecar）已在原生层支持：
  - 本地：`android/.../LocalLibraryPlugin.kt` `readSidecarLyrics`，命中标 `lyricsSource: 'sidecar'`；
  - WebDAV：`WebDavPlugin.kt` `readSidecarLyrics` / `buildSidecarLyricsUrl` 同规则。
- 当前播放器为**在线优先**（`.trellis/spec/frontend/state-management.md:247`）：amll TTML > 平台/LRCLIB 在线 LRC > 本地内嵌/sidecar > 空态；切歌后总是异步匹配（`controller.ts` `matchOnlineLyricsForSong`）。
- 播放器侧在线补全入口（本次移除对象）：
  - `controller.ts` `matchOnlineLyricsForSong`（约 636-740 行，调用点 398、1215）；
  - `controller.ts` `matchOnlineTextMetaForSong` / `matchOnlineCoverForSong`（约 843-980 行，调度点 986-994、1034-1041）；
  - `prefetchMetadata.ts` `prefetchNextMetadata`：WebDAV 下一首三路预取（在线歌词/封面补缺/文本补缺），由 `prefetchNextTrack`（controller.ts 约 1061 行）调度。
- 刮削页（`src/views/ScrapePage.vue` + `scrape/writeback.ts`）已具备封面/歌词/文本候选选择与写回能力，可作为唯一在线补全入口。
- 编辑元数据 sheet（PlayerPage 内 `onApplyCloudMeta` / `onApplyLyrics`）走表单提交，落库为 manual/embedded 语义，不受本次改动影响。
- 来源标记现状：
  - `LyricsSource = 'embedded' | 'sidecar' | 'online'`（`library/types.ts:3`）；`FieldSource = 'embedded' | 'cloud' | 'manual'`（`library/types.ts:16`）；
  - 播放器自动匹配写库标 `'online'`/`'cloud'`；刮削页写回文件失败时也标同样的 `'online'`/`'cloud'`（`scrape/writeback.ts:193-213`）——存量数据两者无法区分。

## 已确认决策（用户拍板）

| # | 决策 | 结论 |
|---|------|------|
| D1 | 在线歌词匹配去留 | 完全禁用（A）：播放器不再自动请求在线；在线补全收敛到刮削页 |
| D2 | 同目录歌词文件规则 | 维持现状（A）：只认同名 `.lrc`，不改原生层，不做翻译/双语扩展 |
| D3 | 存量在线数据 | 清理降级（B）：清除/忽略自动在线来源数据 |
| D4 | 清理范围 | 区分对待（b）：只清「播放器自动」数据；刮削页主动写回的值保留展示 |
| D5 | 存量无法区分的处置 | 引入新标记 `scrape`（增量）+ 存量 `online`/`cloud` 一律按自动数据清理（A） |

## Requirements

- R1 播放器完全禁用自动在线匹配：切歌/播放/预取不再请求在线歌词、在线封面、在线文本元信息；相关代码路径移除。
- R2 歌词展示仅来自内嵌歌词或同目录同名 `.lrc`（现有扫描链路不变）；无本地词显示空态，空态文案引导去刮削页。
- R3 歌曲信息（title/artist/album/cover）展示仅来自内嵌 tag 或用户手动操作（编辑 sheet 手改=manual、刮削页写回=scrape/embedded）；不再有自动 cloud 补缺。
- R4 同目录歌词文件维持「同名 `.lrc`」规则，原生层不改。
- R5 来源标记扩展：`LyricsSource` 与 `FieldSource` 增加 `'scrape'`；刮削页写回文件失败时改标 `scrape`（成功仍标 `embedded`）。
- R6 存量清理迁移（一次性）：库加载时清除所有 `lyricsSource === 'online'` 的歌词（lyrics/lyricsFormat/lyricsSource 一并清）及 `metaSources.* === 'cloud'` 的字段值与标记；`userEditedFields` 保护的手改字段不清理。

## Acceptance Criteria

- [ ] AC1 播放有内嵌歌词或同目录同名 `.lrc` 的歌曲，断网状态下歌词正常显示。
- [ ] AC2 播放无本地歌词的歌曲显示空态，文案提示可到刮削页获取；全程无任何在线歌词请求发出。
- [ ] AC3 全代码库中播放器链路（controller/prefetch）不再 import/调用 `matchOnlineLyrics`、`matchOnlineCoverRemote`、`matchOnlineTextMeta`。
- [ ] AC4 升级后首次加载曲库：存量 `lyricsSource='online'` 歌词与 `metaSources.*='cloud'` 字段被清除；`userEditedFields` 命中的字段保留；迁移只执行一次。
- [ ] AC5 刮削页写回：文件写入成功 → 值标 `embedded` 并正常展示；写入失败 → 值标 `scrape` 并正常展示。
- [ ] AC6 WebDAV 下一首音频预取保留；元信息三路在线预取移除后预取调度不报错。
- [ ] AC7 `pnpm lint`、type-check、单元测试全部通过；受影响模块的单测同步更新。

## Out of Scope

- 不改 Android 原生层（LocalLibraryPlugin.kt / WebDavPlugin.kt）。
- 不做翻译/双语 `.lrc` 文件配对。
- 不新增刮削页功能（现有候选/写回能力已满足）。
- 不动编辑元数据 sheet 的手动应用流程（已是 manual 语义）。
