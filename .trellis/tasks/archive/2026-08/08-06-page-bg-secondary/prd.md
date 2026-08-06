# 全局页面背景改用 surface-secondary

## Goal

页面级背景从 `--h-color-surface`（白）改为 `--h-color-surface-secondary`（#f4f4f5 灰），使 HCellGroup `variant="card"` 卡片（surface 白）在页面上凸显；SongsPage 顶部工具条同步改灰避免突兀白块。

## Background

- 组件库 HCellGroup `variant="card"` 设计：悬浮卡片无阴影，"靠留白/背景对比"凸显（playground 示例：`background: var(--h-color-bg-muted)` 灰底容器）。卡片 body 背景为 `--h-color-surface`（白）。
- 问题：Muses body 背景同为 `--h-color-surface`（白），卡片与背景同色不可见。
- 用户决策：方案 B——全局 body 背景统一灰底（非仅设置页）。
- 暗色模式：token 媒体查询自动切换（surface-secondary 暗色 #2c2c2e），无需额外代码。

## Requirements

- R1：body 背景改 `--h-color-surface-secondary`。
- R2：SongsPage 顶部工具条（48px）从 surface 改 secondary，与背景融合。
- R3：其余显式 surface 块（TabsPage 平板侧边栏、MiniPlayer、navbar、tab-bar）保持白底（导航/播放器独立表面，合理）。

## Acceptance Criteria

- [x] AC1：设置页卡片白 vs 页面灰对比可见（模拟器 computed style：body #f4f4f5、card #ffffff）。
- [x] AC2：歌曲页顶部条与 body 背景一致（#f4f4f5）。
- [x] AC3：`npm run lint` 与 `npm run build` 通过。

## Out of Scope

- 其他页面卡片化改造。
- 暗色模式定制。

## Open Questions

- 无。
