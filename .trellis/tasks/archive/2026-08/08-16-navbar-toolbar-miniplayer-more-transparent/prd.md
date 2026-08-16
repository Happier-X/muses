# PRD：navbar / 工具条 / MiniPlayer 透明效果加重

## 背景

Salt 风格液态玻璃迭代（08-16 系列）后，navbar、歌曲页工具条（位于 navbar subnavbar 内）与底部 mini 播放条/跳转 FAB 的磨砂玻璃透明度仍然偏"实"。用户要求透明效果再重一些（内容透出更明显）。

## 需求

1. **Navbar**（含歌曲页工具条——工具条无独立背景，随 navbar 同一块 `--m-navbar-glass-bg` 表面）：浅色/深色主题下 alpha 均由 `0.8` 降低，透明更明显。
2. **Mini 播放条**（`MiniPlayer.vue`）与同质感的跳转 FAB（`SongsPage.vue` 的 `--m-glass-bg`）：浅色/深色主题下 alpha 均由 `0.6` 降低。
3. 保持既有视觉配方不变：基底色、blur(20px)、内高光、边框高光全部保留，只调 alpha。

## 改动范围

- `src/theme/index.scss` 中 4 个变量：
  - `:root` → `--m-navbar-glass-bg`（浅色）：`rgba(243, 243, 243, 0.8)` → `rgba(243, 243, 243, 0.65)`
  - `:root` → `--m-glass-bg`（浅色）：`rgba(255, 255, 255, 0.6)` → `rgba(255, 255, 255, 0.45)`
  - `.dark` → `--m-navbar-glass-bg`（深色）：`rgba(32, 32, 32, 0.8)` → `rgba(32, 32, 32, 0.65)`
  - `.dark` → `--m-glass-bg`（深色）：`rgba(30, 30, 30, 0.6)` → `rgba(30, 30, 30, 0.45)`

## 验收标准

1. `src/theme/index.scss` 中上述 4 个变量 alpha 已按要求更新（浅色 0.65/0.45，深色 0.65/0.45）。
2. 全局无其它引用处被破坏：`--m-navbar-glass-bg` 引用方（MNavbar、SongsPage 深色覆盖、PlayerPage 沉浸式除外）与 `--m-glass-bg` 引用方（MiniPlayer、SongsPage FAB）行为一致，仅透明度变化。
3. 构建通过（`npm run build` 或等价校验无错误）。
4. 提交信息遵循 `fix(ui): ...` 惯例。
