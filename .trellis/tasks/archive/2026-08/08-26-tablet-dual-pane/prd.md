# M4 平板双栏布局完善

> 前置：TabsLayout 已有 TabletLayout 基础形态（≥768dp 左侧 260dp aside + 内容区，P1 交付）。Web 规格书 `src/views/TabsPage.vue`（isTablet = viewportWidth >= 768）+ 各页面 @media(min-width:768px) 覆盖段。本任务 = **逐页对照 Web 宽屏形态查缺补漏 + 播放页平板分支**。

## Goal

平板/横屏宽度下所有页面与 Web 版宽屏形态一致：导航 aside、内容区布局适配、播放页平板分支（无歌词 FAB 播放键等差异）、MiniPlayer 宽屏位置语义。

## 范围

1. **TabsLayout 宽屏核对**：aside 导航激活态/图标壳/标签样式对照 `__nav-link`；覆盖路由（播放/队列）全屏不受 aside 影响
2. **各列表页宽屏适配**：SongsPage 等页 `@media(min-width:768px)` 覆盖（如 content-pb-md 底部留白）逐项翻译
3. **PlayerPage 平板分支**：`isTabletLayout` 差异项（歌词 FAB 组不显示播放键等）；WebView 方案下经桥传 isTablet 标志由前端适配
4. **MiniPlayer 宽屏**：Web 版平板下 MiniPlayer 位置/宽度语义核对
5. **MuMu 平板分辨率实测**（如 1280×800）

## Out of Scope

- 手机形态改动（已验收冻结）
- 新功能

## Acceptance Criteria
- [ ] MuMu 平板分辨率下逐页与 Web 版宽屏观感一致（用户确认）
- [ ] 手机分辨率回归无变化
- [ ] 门禁 `lintDebug testDebugUnitTest :app:assembleMusesDebug` 全绿
