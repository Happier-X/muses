# Salt UI 一比一复刻（父任务 08-24-native-compose-rewrite 的子任务）

> 背景：M1 交付的 UI 是占位级「基础形态」，与 Web 层实际观感差距巨大（Web ~10.4k 行 Vue vs 原生 ~1.5k 行）。父任务 D4 定的是「一比一复刻 Salt Player 风格」，本任务补齐这笔欠账。

## Goal

以 **Web 层 Vue 源码为规格书**，用 Compose 一比一复刻全部页面观感与交互：布局结构、间距、字号字重、颜色令牌、明暗主题、交互动效逐项对齐；每页完成后在模拟器与旧版并排对比验收。

## 核心方法论（本任务的第一原则）

1. **Vue 源码 = 唯一规格书**：每个页面实现前先读对应 `.vue` 文件 + `src/theme/index.scss` 设计令牌，逐段翻译成 Compose，不自由发挥、不"合理化重构"
2. **组件级映射**：Web 层 28 个 `m-*` 自研组件（Konsta 风格）逐一建立 Compose 对应物，页面只组装组件
3. **像素对照验收**：每页完成 → MuMu 安装 → 与旧版并排对比 → 用户确认过 → 下一页
4. **功能不动**：本任务只动观感与交互层；ViewModel/数据链路保持 M1/M2 已交付状态（缺功能的页面除外，如多选）

## 复刻范围（按优先级分批）

| 批次 | 页面 | Web 基准文件 |
|---|---|---|
| P0 共享层 | m-* 组件库映射 + 设计令牌 + MiniPlayer | `src/components/ui/*`（28 个）、`theme/index.scss`（797 行）、`MiniPlayer.vue` |
| P1 主框架 | TabsPage（侧边栏/抽屉双形态导航） | `TabsPage.vue`（684 行） |
| P2 歌曲域 | SongsPage、AlbumsPage、ArtistsPage、LibraryDetailPage | 1375/156/165/442 行 |
| P3 播放域 | QueuePage、PlaylistsPage、PlaylistDetailPage | 289/323/345 行 |
| P4 播放页 | PlayerPage（含 AMLL 层整合、拖拽手势、固定头部） | 3430 行（压轴） |
| P5 音源域 | SourcesPage、SourceWebDavPage、SourceWebDavBrowsePage、SettingsPage | 随 M3 功能一起或独立批次 |

## Acceptance Criteria

- [ ] 每个 P 批次：MuMu 上新旧版并排对比，布局结构/间距/字号/颜色/明暗主题肉眼无差异（用户逐批确认）
- [ ] 明暗主题跟随系统切换正确（SCSS 双套 CSS 变量 → Compose 双套 ColorScheme）
- [ ] m-* 组件映射覆盖页面所需全集，页面不再直接使用 Material 默认观感组件（Material 仅作行为基座）
- [ ] AMLL 歌词层（M2 已交付）在复刻后的播放页中正常工作
- [ ] `cd native && ./gradlew lint testDebugUnitTest :app:assembleDebug` 全绿

## Out of Scope

- 刮削相关页面（ScrapePage 属 M3）
- 平板双栏深度优化（Web 层有断点逻辑，先保证手机形态一比一，平板形态对齐布局规则即可）

## 关键决策

- **D1 Vue=规格书**（2026-08-25）：不复刻"意思"，逐段翻译源码；SCSS 变量直接换算为 Compose 令牌
- **D2 组件先行**（2026-08-25）：P0 先把 m-* 映射层建好再拼页面，避免每页重复造控件
