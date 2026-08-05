# 云端强制搜与多候选编排 API

## Goal

为编辑页提供**强制搜索 + 多候选**编排（文本 / 封面 / 歌词），与播放静默补空 API 分离或可区分模式；**不**含 PlayerPage UI。

## Parent

`.trellis/tasks/08-05-player-edit-cloud-meta`（D1–D6）

## Ordering

本子任务应先于 `08-05-edit-cloud-meta-ui` 完成（UI 依赖本 API）。

## Requirements

1. 编辑查询输入：`songId` + 当前 title/artist/album（及 duration 若歌词需要）。
2. **忽略** `needsOnlineTextMeta` / 已有封面跳过 / userEdited 跳过请求；编辑路径始终尝试搜索。
3. 返回结构至少包含：
   - `textCandidates: TextMetaHit[]`（去重、排序，标 defaultIndex）
   - `coverCandidates: { remoteUrl, source }[]`
   - `lyricsCandidates: { text, format, source, translationText? }[]`
   - 各维 `status: 'ok' | 'no-match' | 'network' | ...`
4. 支持 `AbortSignal` 或 token 由调用方丢弃（API 层至少可中途 stop 串行源）。
5. 封面**不**在 API 内落盘；只返回 remote URL。落盘在 UI 应用时 `cacheRemoteCover`。
6. 不写 `playerState`、不 `upsertSong`。
7. 现有 `matchOnlineTextMeta` / `matchOnlineCoverRemote` / `matchOnlineLyrics` 播放行为回归保持。

## Out of Scope

- 任何 Vue UI
- 写库 / 写文件
- 新平台源

## Acceptance Criteria

- [x] AC-API1：字段已满时编辑搜索仍返回候选（若源有数据）— 不读 needsOnlineTextMeta / hitFillsMissing / 播放负缓存
- [x] AC-API2：文本/封面/歌词均可返回 >1 候选（源足够时）— 全源收集 + 去重 + MAX 8
- [x] AC-API3：播放用 match* 路径行为不变 — metadata/cover/lyrics match 文件无 diff
- [x] AC-API4：lint/build 相关模块通过 — `npm run lint` / `npm run build` 通过

## Notes

- 复杂：需本目录 `design.md` 片段或复用父 design + 本 `implement.md`。
