# child2：三层入队入口——播放页标记 + 列表多选 + 筛选批量

## Goal

提供待刮削队列的全部入队入口（R4/R5），让用户从「播放中发现问题」到「浏览列表发现异常」再到「整库体检」都能把歌曲推入待刮削队列。

## Background / 依赖

- Parent: `08-18-library-tag-governance`（design.md §3）。
- 依赖 child1 的队列 API（`enqueueScrapeSongs` / 队列事件）。
- 现有 UI 锚点：`PlayerPage.vue` 歌曲操作菜单（`m-actions`，约 L360）；`SongsPage.vue` 多选条（L167 起）、长按操作菜单。

## Requirements

- R2-1 播放页：歌曲操作菜单新增「标记待刮削」；已标记时文案切「取消标记」，toast 反馈，不打断播放。
- R2-2 歌曲列表：长按项操作菜单新增「加入待刮削」；多选条新增「标记待刮削」按钮（selectedCount>0 可点）。
- R2-3 筛选批量：可疑歌曲判定函数（标题=文件名占位 / 缺 artist / 缺 album / 缺 cover / 缺 lyrics / 来源 cloud 且歌词 online 低可信）→ 筛选结果确认框 → 批量入队 → toast 数量 + 可跳转刮削页。
- R2-4 新路由 `/scrape`（刮削中心占位页，child3 实现完整流程）；入口从入队反馈/设置页/歌曲页顶部图标进入。

## Acceptance Criteria

- [ ] 播放页标记/取消标记可用，幂等（重复标记不重复入队），toast 反馈正常。
- [ ] 列表长按、多选条入队可用；多选 0 项时按钮禁用。
- [ ] 筛选批量：判定规则命中预期歌曲（单测覆盖判定函数），确认框 → 批量入队 → toast 数量正确。
- [ ] 队列状态在三个入口间实时同步（事件广播生效）。
- [ ] vue-tsc build 通过；判定函数单测 ≥ 6 例。

## Out of Scope

- 刮削流程/差异预览/写回（child3）、来源字段模型（child1 已做）、评分门槛（child4）。