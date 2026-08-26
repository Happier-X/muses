# 技术设计 — 平板双栏完善

## 方法论

沿用「Web=规格书」：每页先读对应 .vue 的 `@media(min-width:768px)` 覆盖段与 TabsPage.vue 的 isTablet 分支，逐条翻译为 Compose（BoxWithConstraints maxWidth 判定或 Configuration.screenWidthDp）。

## 关键机制

- **isTablet 判定**：TabsLayout 已有 `maxWidth >= 768.dp`；页面级需要时用 `LocalConfiguration.screenWidthDp >= 768` 或 BoxWithConstraints 局部判定
- **WebView 播放页**：isTablet 经 updatePlayerState payload 加 `isTabletLayout: Boolean` 字段下发，前端 CSS 类切换（对齐 PlayerPage.vue isTabletLayout 分支）
- **MiniPlayer 宽屏**：对照 TabsPage.vue 宽屏段 MiniPlayer 的定位/宽度规则

## 风险

- 页面级覆盖散落在多个 .vue 文件的 media query 里，需逐文件盘点防遗漏
- WebView 播放页双端字段同步（Kotlin 判定 vs 前端自行判定不一致会导致两套布局混搭）
