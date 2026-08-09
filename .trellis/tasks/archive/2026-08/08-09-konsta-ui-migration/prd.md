# 组件库迁移 Konsta UI（iOS 主题，完整替换）

## Goal

将 Muses（Vue 3 + Tailwind v4 + Capacitor 移动端音乐播放器）的 UI 组件体系从自研 `happier-ui` 完整迁移到 **Konsta UI v5（iOS 主题）**，获得原生 iOS 视觉质感；迁移后不再依赖 happier-ui 包，保留/收编必要的自建组件。

## 已确认事实（代码调查）

### 现状组件使用
- 11 个页面（TabsPage 为 tab 容器）：SongsPage、AlbumsPage、ArtistsPage、LibraryDetailPage、PlaylistsPage、PlaylistDetailPage、SettingsPage、SourcesPage、QueuePage、PlayerPage
- 自建壳组件：`MPage` / `MContent` / `MCover`（src/components/ui/）、`MiniPlayer.vue`
- 页面实际使用的 happier-ui 组件（经 `src/components/ui/index.ts` 转出）：
  - HNavBar、HCell、HCellGroup、HSwitch、HToast、HButton、HIcon、HPopup、HEmpty、HFloatingBubble、HBottomSheet、HDialog、HInput、HCard、HCheckbox、HProgress、HTabBar
  - index.ts 还转出了 HBadge、HImage、HPagination、HRange、HSelect、HSidebar、HTable、HTag、HTextarea（页面未直接用，仅转出）
- PlayerPage 全自绘（歌词 @applemusic-like-lyrics、PIXI 背景、自定义控制栏/进度条/action sheet 类样式），不依赖 ui 组件
- 页面大量结构用原生 div + Tailwind class 自绘，仅按钮/弹层/列表等复用 H* 组件

### 主题体系（当前全部来自 happier-ui 包）
- `--h-*` token：颜色（--h-color-*）、间距、圆角、字号、组件级 token（--h-cell-*、--h-button-* 等）
- `--muses-*` 语义别名：包内定义（如 `--muses-color-ink: var(--h-color-ink)`），项目页面直接使用 ~20 个
- 暗色模式：`@media (prefers-color-scheme: dark)` 自动切换（包内实现）
- 入口：src/theme/tokens.css（@import happier-ui/tokens.css）、src/theme/tailwind.css（@import happier-ui/styles）

### Konsta UI v5.3.0（已核实）
- npm `konsta`@5.3.0，MIT，依赖仅 tailwind-merge；Vue 3 官方支持（konsta/vue）
- 组件（65 个）覆盖现有需求：Navbar、List/ListItem/ListGroup/ListInput、Toggle、Button、Popup、Sheet、Actions(ActionSheet)、Dialog、Toast、Card、Checkbox、Progressbar、Tabbar、Segmented、Range、Table 系列、Page、Block 等
- **缺失**：Empty（空状态）、FloatingBubble（悬浮球）、MCover（音乐封面）、MiniPlayer（自绘，保留）
- 主题：iOS 默认（系统字体 + --color-ios-* 变量：primary=#007AFF 等），Material 需 Roboto；CSS 变量 `--k-*` 体系
- 集成方式：独立模式 `<k-app theme="ios">` 或 Provider 模式（konstaProvider + 根元素 `k-ios` class）
- 依赖 Tailwind（v4 兼容，入口 `@import 'konsta/vue/theme.css'`）
- 图标：Konsta 自带 Icon，项目已有 @lucide/vue 可继续使用

## Requirements

- [ ] R1 项目所有页面组件从 H* 组件迁移到 Konsta k* 组件，保持现有功能与交互行为不变
- [ ] R2 移除对 happier-ui 包的依赖（npm 卸载 + 样式/主题入口替换为 Konsta）
- [ ] R3 主题迁移到 Konsta iOS 主题体系；暗色模式行为保持一致（跟随系统）
- [ ] R4 Konsta 缺失组件（空状态、悬浮球等）以自建方式补齐，视觉与 Konsta 体系一致
- [ ] R5 自绘内容（PlayerPage 歌词/控制、MiniPlayer、MCover）视觉与 Konsta iOS 风格协调（按钮、action sheet、进度条等尽量复用 k* 组件或 k 样式类）
- [ ] R6 迁移过程中保持移动端（Android Capacitor）体验不回归：触控目标、弹层动画、滚动行为
- [ ] R7 迁移后构建、lint、类型检查全部通过

## Acceptance Criteria

- [ ] A1 全部页面（含弹层、action sheet、对话框、toast）可在 iOS 主题下正常渲染与交互，与迁移前功能等价
- [ ] A2 package.json 中不再包含 happier-ui；src 中无 `happier-ui` 与 `@/components/ui` 的 H* 组件 import
- [ ] A3 主题变量全部来自 Konsta 体系（--k-* / --color-ios-*），无 --h-* 残留（或已在设计文档中明确保留的别名层）
- [ ] A4 暗色模式与亮色模式均正常，跟随系统切换行为与迁移前一致
- [ ] A5 空状态、悬浮球等自建组件在各使用页面表现一致且视觉统一
- [ ] A6 Android 真机/模拟器验证：滚动、弹层、触摸交互无回归（含 MiniPlayer 常驻与播放队列）
- [ ] A7 `npm run build`（vue-tsc + vite build）与 `npm run lint` 通过

## Out of Scope

- 不做框架级重构（保留 vue-router 现有路由结构与页面组织）
- 不引入 Ionic / Framework7 等框架壳
- 桌面端（PC 浏览器/Electron）适配不纳入本次范围（保留现状，仅确保移动端体验不回归）
- 不改动业务逻辑、数据层、音频播放、歌词引擎

## Key Decisions

- [x] Q1 **主题策略（用户拍板：方案 A）**：完全采用 Konsta 默认 iOS 主题体系（系统蓝 #007AFF、iOS 灰阶、分组列表）。不再保留 `--h-*` / `--muses-*` 自定义语义层；页面自绘结构中引用的旧变量全部改写为 Konsta 变量（`--k-*` / `--color-ios-*`）或 k* 组件 class。
- [x] Q2 **迁移节奏**：分阶段推进——基础设施（阶段 0）→ 壳与导航（阶段 1）→ 逐页迁移由简到难（阶段 2）→ 清理收尾（阶段 3）。每阶段独立验证与 commit，可单批回滚。详见 implement.md。
- [x] Q3 **暗色模式（已调研）**：Konsta 暗色由 `.dark` class 驱动（Tailwind v4 `@custom-variant dark`），非 prefers-color-scheme 媒体查询。项目新增 `useSystemDark` composable（matchMedia 监听 → 根元素切换 `.dark` class），保持现状“跟随系统”行为。
- [x] Q4 **集成方式（用户拍板）**：按 Konsta 官方最佳实践，用 `k-app` 独立模式（`<k-app theme="ios">`）包裹整个应用。k-app = konsta-provider（全局主题上下文）+ 根 div 自动挂 k-ios class；App.vue 根 div 替换为 k-app，保留现有高度链。详见 design.md §1。
