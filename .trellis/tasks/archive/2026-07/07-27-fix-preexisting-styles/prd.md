# 修复预存的样式问题（song-item flex 缺失与 action-sheet-item 作用域问题）

## Goal
修复之前“脱离 Ionic”迁移和“样式迁移到 Tailwind v4”任务期间记录的两个预存样式遗留问题：
1. `.song-item` / `.queue-item` 的 flex 布局缺失问题。
2. `.action-sheet-item` 在跨页面的作用域丢失问题。

## Requirements
- **R1: 修复歌曲列表行布局**：`SongsPage.vue`、`QueuePage.vue` 和 `PlaylistDetailPage.vue` 等使用虚拟列表或 `.song-item`/`.queue-item` 的地方，原本可能因为失去 `ion-item` 的底层 flex 而缺少横向 flex 布局。需要为其补充 `flex items-center` 相关的 Tailwind 类，使左侧占位/封面、中间文字标题/信息以及右侧操作按钮能正常按 flex 对齐，且中间文本区域需正确收缩（`min-w-0`）以支持超长截断（`truncate` / `line-clamp`）。
- **R2: 修复 action sheet 按钮样式**：原 `SourcesPage` 中的 `.action-sheet-item` / `.action-sheet-cancel` 样式在迁移中被转换为了 JS 字符串常量（如 `actionShetItemClass`），而 `SongsPage` 与 `PlaylistsPage` 在底部的 `<h-bottom-sheet>` 弹窗中也使用了相应的 CSS class。需要将这些共用的 action-sheet 样式抽离（可以直接复用或提取统一的 Tailwind class 组合），保证各页面的上拉菜单按钮视觉统一、无样式丢失。
- **R3: 零回归**：保持纯 Tailwind v4 编写，不能引入任何新的 `<style scoped>` 块。

## Acceptance Criteria
- [ ] AC1：`SongsPage`、`QueuePage` 里的歌曲行具有 `flex items-center gap-x` 等正确布局。
- [ ] AC2：超长歌曲名/艺术家名能够正确截断（在中间块需要 `min-w-0` 支持）。
- [ ] AC3：`SongsPage` 和 `PlaylistsPage` 中的 `<h-bottom-sheet>` 内部按钮具有正确的样式（复用 `SourcesPage` 相同的基础 Tailwind class）。
- [ ] AC4：`npm run build` 和 `npm run lint` 验证全绿，测试（`npm run test:unit`）通过。

## Context
在 Tailwind 迁移期间发现，部分页面的 `.song-item` 的外壳仅有 `absolute`（对于虚拟列表的行），没有 `flex`，导致原本期望单行省略的子级因缺乏约束而无法正常截断；而 action sheet 按钮因原样式被隔绝在 `SourcesPage` 的 scoped 区域内，在转换为 Tailwind utilities 后其他组件未同步获得。
