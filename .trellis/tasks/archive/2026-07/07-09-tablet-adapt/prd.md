# 平板适配：列表网格化 + Tab 侧栏 + 内容限位居中

## 目标

让 Ionic Vue + Capacitor 应用在 Android 平板（以及大屏浏览器窗口）下从"拉伸的手机布局"变为真正的大屏适配布局，覆盖列表多列、Tab 侧栏化、内容最大宽度限位三项核心体验。

## 背景与现状

- 当前项目无任何响应式断点代码，所有页面按手机单列布局设计
- 路由：`/tabs/*`（标签页 Songs/Albums/Artists/Playlists/Sources/Settings） + `/player`、`/queue`（全屏页）
- Tab 形态：`src/views/TabsPage.vue` 使用 `ion-tabs` + 底部 `ion-tab-bar`
- 主题文件 `src/theme/variables.css` 几乎为空（只有注释），是放全局断点/限位变量的合适位置
- AndroidManifest 未锁定方向，`configChanges` 已含 `orientation|screenSize|smallestScreenSize|screenLayout|uiMode`

## 范围与子任务映射

本轮 MVP 只交付三个独立可验证的子目标，全部归入单一子任务 `07-09-tablet-mvp-grid`（避免单 PRD 含义过宽，但仍作为一个连续交付一起验证）：

| 项 | 子任务 |
|----|-------|
| A 列表/网格列数自适应  | 07-09-tablet-mvp-grid（✅ 已完成） |
| B Tab Bar → 宽屏侧栏 | 07-09-tablet-mvp-grid（✅ 已完成） |
| D 内容最大宽度限位居中 | 07-09-tablet-mvp-grid（✅ 已完成） |
| C Player 全屏页宽屏双栏 | 07-09-tablet-player-split（✅ 已完成） |
| E Player 横屏视口校准：vh→dvh | 07-09-tablet-dvh（✅ 已完成） |
| F MiniPlayer 宽屏居中限宽 | 07-09-tablet-miniplayer（✅ 已完成） |

平板适配系列全部完成。

## 跨子任务验收标准（父级）

- [ ] 宽屏（≥768px 物理宽度）下，至少 Songs/Albums/Artists/Playlists 四个列表页呈现多列网格
- [ ] 宽屏下 Tab Bar 移至左侧侧栏，不再贴底
- [ ] 宽屏下主内容区有最大宽度限位且居中，不再被横向拉满
- [ ] 窄屏（手机宽度 <768px）下布局与当前一致，无回归
- [ ] `npm run build` 通过（lint + type-check）

## 超出范围（父级）

- 原生 Android 代码改动（不修改 MainActivity.kt / AndroidManifest.xml）
- Player / Queue 页结构改动（留作后续 C 子任务）
- 横屏专项校准（留作后续 E 子任务）