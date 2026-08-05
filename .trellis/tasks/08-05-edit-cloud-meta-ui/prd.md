# 编辑 sheet 云端预览勾选应用 UI

## Goal

在 `PlayerPage`「编辑歌曲信息」sheet 内接入云端获取 UI：手动获取、多维预览与换候选、分字段勾选、应用到表单；保存沿用现有逻辑。

## Parent

`.trellis/tasks/08-05-player-edit-cloud-meta`（D1–D6）

## Ordering

依赖 `08-05-edit-cloud-meta-api` 的编排 API 就绪（或约定 mock 接口形状一致）。

## Requirements

1. 「从云端获取」按钮；打开编辑不自动搜。
2. Loading / 错误 / 部分成功状态。
3. 文本、封面、歌词预览 + 展开其它候选（只改预览选中项）。
4. 字段勾选 title/artist/album/cover/lyrics；默认有值全勾；「应用到表单」。
5. 应用封面：`cacheRemoteCover` → 安全 URI + `editCoverDirty`。
6. 应用歌词：写入 form lyrics（+ format 侧状态若保存需要）。
7. 关 sheet / 切歌作废 token；不阻塞播放。
8. 不改 RG 云端；不改保存 D4 契约。

## Out of Scope

- 编排 API 内部实现（属 api 子任务）
- 列表页编辑入口

## Acceptance Criteria

- [ ] 对齐父 PRD AC1–AC9、AC11–AC12 中 UI 相关项
- [ ] 翻译 FAB / mode-bar 等无关回归

## Notes

- 复杂 UI：建议 `implement.md`；可复用父 design 交互一节。
