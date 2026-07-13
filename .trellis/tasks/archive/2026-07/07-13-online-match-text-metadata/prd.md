# 在线匹配标题 / 歌手 / 专辑

## Goal

为本地/WebDAV 曲库中 **文本元信息缺失** 的歌曲，在线补全 `artist` / `album`（仅补缺），写回曲库并刷新当前播放展示；与已有封面匹配、amll 歌词共同覆盖 Issue #18。

## Issue / 背景

- GitHub #18：封面、歌手/艺术家、专辑、歌词等
- **已完成**：在线封面（仅补缺 + 本地缓存写回）；在线 TTML 歌词（运行时、不写回）
- **本任务**：`artist` / `album` 文本补缺（`title` 本轮不改）

## Confirmed Facts

- `SongItem`：`title` 必有；`artist?` / `album?`；`upsertSong` → `muses:songs`
- 封面链路独立：`src/features/cover`；多源搜索结果含曲名/歌手/专辑
- 播放后懒扫描 + 在线封面 token 模式可复用

## Decisions

| 决策 | 结论 |
|------|------|
| 补全策略 | **仅补缺**；不覆盖已有非空 `artist`/`album` |
| MVP 字段 | **artist + album**；**不改 title** |
| 部分补全 | **有任一空字段即匹配**；写回时 **只填仍为空的字段** |
| 写回 | **`upsertSong` 写回 `muses:songs`** |
| 触发 | **播放时自动**（本地扫描结束后若仍缺字段则异步匹配） |
| 模块 | **独立** `src/features/metadata/`（或等价），与封面并行 |
| 源顺序 | **kw → tx → wy → kg → mg**（对齐 any-listen 国内段）；MVP **不含 iTunes** |
| 歌词 | **不做**（沿用 amll） |

## Requirements

1. **R1** 当 `artist` 或 `album` 为空（trim 后）时，可在线匹配并返回候选文本。
2. **R2** 默认源顺序 kw → tx → wy → kg → mg；任一源给出可用结果即停（或按设计取最优命中）。
3. **R3** 写回仅填充空字段；已有非空字段禁止覆盖。
4. **R4** 不修改 `title`。
5. **R5** 播放路径自动触发；不阻塞播放；token 防串曲；失败静默；负缓存。
6. **R6** 写回后若仍是当前曲：`syncDisplayStateFromSong` + 媒体会话 metadata 文本更新。
7. **R7** 与封面/歌词链路互不阻塞（可并行）。
8. **R8** 单测覆盖：仅补空、不覆盖、触发条件、源失败回退；spec 同步。

## Acceptance Criteria

- [ ] AC1：仅缺 artist 时只写 artist；仅缺 album 时只写 album。
- [ ] AC2：两字段皆有值时不发起匹配。
- [ ] AC3：写回 `muses:songs` 后列表/重进可看到；当前曲 UI/通知文本刷新。
- [ ] AC4：`title` 与已有非空 artist/album 不变。
- [ ] AC5：失败不影响播放与封面/歌词。
- [ ] AC6：测试 / lint / tsc / spec 通过。

## Out of Scope

- 改 title / 文件名启发式
- 平台歌词源、手选候选 UI、全库批量刮削
- 与封面强绑同一次 HTTP 搜索

## Task Type

Complex
