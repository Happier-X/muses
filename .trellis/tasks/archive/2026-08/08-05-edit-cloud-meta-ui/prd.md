# 编辑 sheet 云端预览勾选应用 UI

## Goal

在 `PlayerPage`「编辑歌曲信息」sheet 内接入云端获取 UI：手动获取、多维预览与换候选、分字段勾选、应用到表单；保存沿用现有逻辑。

## Parent

`.trellis/tasks/08-05-player-edit-cloud-meta`（D1–D6）

## Ordering

依赖 `08-05-edit-cloud-meta-api` 的编排 API 就绪（已归档）。

## Requirements

1. 「从云端获取」按钮；打开编辑不自动搜。
2. Loading / 错误 / 部分成功状态。
3. 文本、封面、歌词预览 + 展开其它候选（只改预览选中项）。
4. 字段勾选 title/artist/album/cover/lyrics；默认有值全勾；「应用到表单」。
5. 应用封面：`cacheRemoteCover` → 安全 URI + `editCoverDirty`。
6. 应用歌词：写入 form lyrics（+ format 侧状态）。
7. 关 sheet / 切歌作废 token；不阻塞播放。
8. 不改 RG 云端；不改保存 D4 契约。

## Out of Scope

- 编排 API 内部实现（属 api 子任务）
- 列表页编辑入口

## Acceptance Criteria

- [x] AC1：有「从云端获取」；打开编辑不自动搜
- [x] AC2：预览 + 换候选不改表单
- [x] AC3：分字段勾选 + 应用到表单
- [x] AC4：有值默认全勾
- [x] AC5：封面经 cacheRemoteCover
- [x] AC6：歌词写入 + lyricsFormat（手改重置 lrc）
- [x] AC7：失败/无匹配反馈
- [x] AC8：关 sheet / 切歌 abort 且不串曲
- [x] AC9：保存仍 dirty-only + D4
- [x] AC11：lint/build 通过

## Notes

- 复杂 UI：见 `design.md` / `implement.md`。
