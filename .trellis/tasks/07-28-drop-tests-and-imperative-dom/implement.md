# 执行计划：删除测试并改用 Vue 3 声明式 DOM 操作

## 验证命令

- 构建 + 类型检查：`npm run build`（= `vue-tsc && vite build`）
- Lint：`npm run lint`
- 手动验证（用户执行，本文档不列）：MiniPlayer 按钮、队列删除、更多菜单、歌词手势、FAB 跳转

## 执行原则

- 命令式 DOM 改造在每一步完成后立即 build/lint，避免一处改坏连环出错。
- 标记类删除一次性收尾扫一遍。
- 测试删除独立批次，避免和代码改写同时动。

## 第一批：删除测试与测试基础设施

- [ ] 删除 `tests/unit/` 整目录
- [ ] 删除 `tests/e2e/` 整目录
- [ ] 删除 `cypress.config.ts`
- [ ] `package.json`：移除 `test:unit` 和 `test:e2e` 脚本；移除依赖 `vitest`、`@vue/test-utils`、`jsdom`、`cypress`
- [ ] `vite.config.ts`：移除 `<reference types="vitest" />` 与 `test:` 块
- [ ] `tsconfig` 如有 vitest types 残留一并清理
- [ ] Gate：`npm run build` + `npm run lint`

## 第二批：MiniPlayer / PlaylistDetailPage / QueuePage 三处父代理解析

- [ ] `MiniPlayer.vue`：删 `isPlayerActionEvent`；`openPlayerPage` 删 `composedPath` 调用；删 `player-actions` class
- [ ] `PlaylistDetailPage.vue`：`onPlaySong` 删 `composedPath` 检查；删 `more-button` class
- [ ] `QueuePage.vue`：`onSelectSong` 删 `composedPath` 检查；删 `remove-button` class
- [ ] Gate：`npm run build` + `npm run lint`

## 第三批：SongsPage FAB 跳转 ref 化

- [ ] 给虚拟行 `<div>` 内层（`data-song-id` 所在的 `role="button"`）或外层 `virtualRow` div 挂 `:ref="el => songRowRefs.set(...)"`
- [ ] `findSongRow` 改为 `songRowRefs.get(songId)`
- [ ] 移除 `querySelectorAll('[data-song-id]')` 调用
- [ ] Gate：`npm run build` + `npm run lint`

## 第四批：PlayerPage 手势 ref 化

- [ ] 给 `<section class="lyric-panel">`、`<LyricPlayer class="lyric-player">`、`<h-range class="progress-range">` 分别加 `ref="lyricPanelRef"`、`ref="lyricPlayerRef"`、`ref="progressRangeRef"`
- [ ] `INTERACTIVE_SELECTOR` 去掉 `.progress-range`，新增 `progressRangeRef.value?.contains(el)` 判断
- [ ] `isLyricPanelTarget` 改成 `lyricPanelRef.value?.contains(...) || lyricPlayerRef.value?.contains(...)`，删 `closest('.lyric-panel, .lyric-player')` + `composedPath` 兜底
- [ ] `canStartVerticalDismiss` 保留（无声明式替代）
- [ ] Gate：`npm run build` + `npm run lint`

## 第五批：清理剩余测试依赖标记类

- [ ] `PlayerPage.vue`：删 `amll-background`、`amll-background-render`、`immersive-shell` class
- [ ] `MiniPlayer.vue`：删 `mini-player` class
- [ ] `App.vue`：删 `app-mini-player`、`app-player-page` class
- [ ] `MCover.vue`：删 `m-cover` class（零锚点、零 JS 引用）
- [ ] Gate：`npm run build` + `npm run lint`

## 收尾

- [ ] 全局 grep 确认无残留：`composedPath`、`classList.contains`（保留 `canStartVerticalDismiss` 除外）、`querySelector`
- [ ] spec 同步：`.trellis/spec/frontend/component-guidelines.md` 的「标记类共存原则」更新——加入"测试已删，标记类不分保留/移除两套列表"表述；删 `player.spec` 引用相关的示例
- [ ] 最终 `npm run build` + `npm run lint` 全绿
- [ ] PRD 标记 AC 完成

## 风险与回滚点

- **删除测试**：不可逆。若未来需要再测，重新生成。回滚点：本地 git。
- **MiniPlayer 按钮 `@click.stop` 不足**：极低风险——标准 DOM 冒泡模型，`@click.stop` 等价 `event.stopPropagation()`，比 `composedPath` 检查更直接、更可靠。
- **SongsPage ref 化**：`@tanstack/vue-virtual` 的 `measureElement` 是行 ref 的正规集成方式，文档支持；但 ref 数组回收时机要注意（`onUnmounted` 清空 map）。
- **PlayerPage 手势**：歌词面板 ref 失效时，`contains` 返回 false → `isLyricPanelTarget` 返回 false → 触屏可在歌词区内误触发下滑关闭。需要在手势初始化前确保 `lyricPanelRef` 已挂载（onMounted 后才启用手势）。
- **PlayerPage `.lyric-fab .h-icon`**：保留 CSS 锚点 `lyric-fab`，不删。
