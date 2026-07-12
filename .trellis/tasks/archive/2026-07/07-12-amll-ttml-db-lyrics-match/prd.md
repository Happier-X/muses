# 接入 amll-ttml-db 歌词匹配

## Goal

从 [amll-ttml-db](https://github.com/amll-dev/amll-ttml-db) 为当前播放歌曲自动匹配 TTML 歌词，在沉浸式歌词页以 AMLL 逐词形式展示，提升歌词覆盖率与表现力。

## Issue

- GitHub #8：`引入 https://github.com/amll-dev/amll-ttml-db 来实现歌词匹配`

## Background / Confirmed Facts

### 现有歌词链路

- `SongItem.lyrics` / `lyricsSource: 'embedded' | 'sidecar'` 存于本地曲库（`src/features/library/types.ts`）。
- 扫描入库时从标签读取歌词；`playerState.lyrics` 来自当前歌曲。
- `PlayerPage.vue` 用 `@applemusic-like-lyrics/lyric` 的 `parseLrc` 解析 LRC 后交给 `LyricPlayer`。
- 空态文案：`当前歌曲没有内嵌歌词，也没有找到同目录同名 .lrc 文件。`
- 同包已依赖 `@applemusic-like-lyrics/lyric`，导出 **`parseTTML`**，可解析 amll-ttml-db 的 TTML。

### amll-ttml-db

- 索引：`metadata/raw-lyrics-index.jsonl`（约 3k 行 / ~1.5MB；字段含 `musicName` / `artists` / `album` / `rawLyricFile` 等）。
- 歌词：`raw-lyrics/<file>.ttml`。
- 公开 CDN（jsDelivr）可读，不可把整库打进 APK。

## Decisions

| 决策 | 结论 |
|------|------|
| 触发时机 | 无论有无本地歌词，开播/切歌后都自动匹配 |
| 优先级 | 在线优先；匹配成功用 TTML；失败回退本地，再回退空态 |
| 匹配选取 | 歌名+歌手（可选专辑）打分，自动取最佳一条；无多候选 UI |
| 持久化 | 不写回 `SongItem`；运行时按 songId 缓存 |
| MVP UI | 静默匹配 + 空态/匹配中状态文案；无设置开关、无手动重匹配 |

## Requirements

1. **R1** 切歌/开播后自动尝试 amll-ttml-db 匹配（不依赖本地是否已有歌词）。
2. **R2** 匹配成功：用在线 TTML 驱动 `LyricPlayer`（`parseTTML`）。
3. **R3** 匹配失败/无结果/网络错误：回退本地 `embedded`/`sidecar`；再无则空态。
4. **R4** 匹配键：歌名 + 歌手（可选专辑）规范化后打分，取最高分一条；低于阈值视为无匹配。
5. **R5** 索引：首次从 CDN 拉取 `raw-lyrics-index.jsonl`，内存缓存；TTML 按 `rawLyricFile` 按需拉取，按 songId 缓存。
6. **R6** 不写回 `SongItem` / 不改曲库 schema。
7. **R7** 快速切歌：过期匹配结果不得覆盖当前歌曲展示（token / songId 校验）。
8. **R8** 歌词页状态：
   - 匹配中：有本地词可先展示本地；无本地词显示「正在匹配在线歌词…」类文案。
   - 匹配失败且无本地：更新空态文案（说明未匹配到在线歌词）。
9. **R9** 不阻塞播放：匹配失败静默回退，不弹错误打断播放。
10. **R10** 单测覆盖：打分/规范化、最佳选取、缓存命中、切歌丢弃过期结果、parseTTML 路径；lint / type-check 通过。
11. **R11** 同步 frontend spec（features-player / component-guidelines 中歌词来源与空态约定）。

## Acceptance Criteria

- [ ] AC1：开播/切歌后无论是否有本地歌词都会发起匹配（可观测：请求或状态）。
- [ ] AC2：匹配成功后歌词页展示 TTML 逐词（`parseTTML` 结果）。
- [ ] AC3：匹配失败时有本地歌词则展示本地；无本地则空态，且文案区分匹配失败。
- [ ] AC4：匹配中无本地词时显示「匹配中」类状态，不一直空白。
- [ ] AC5：快速切歌不会把上一首的在线歌词贴到当前曲。
- [ ] AC6：曲库 `SongItem` 无新增写回；重启后需重新匹配（运行时缓存可丢）。
- [ ] AC7：相关单测 + lint + type-check 通过。
- [ ] AC8：spec 已同步。

## Out of Scope

- 整库打进安装包
- 歌词投稿 / 审核
- 多候选选择 UI
- 设置开关 / 手动重匹配按钮
- 写回 `SongItem` 或离线持久化 TTML
- 网易云 / QQ / Spotify 平台 ID 精确匹配（本地无 ID）
- 修改 `node_modules/@applemusic-like-lyrics/*`

## Task Type

Complex（需 design.md + implement.md）
