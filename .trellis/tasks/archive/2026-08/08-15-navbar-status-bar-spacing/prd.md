# MNavbar 顶部增加状态栏呼吸间距

## 背景

Android 真机上，Capacitor 8 SystemBars 插件（`insetsHandling: 'css'`）把状态栏真实高度注入 `--safe-area-inset-top`（一般 24~40px）。当前 `MNavbar.vue` 的顶部内边距为：

```scss
padding-top: max(16px, var(--m-safe-area-top, 0px));
```

`max()` 恒取安全区值本身，导致 navbar 内容行紧贴状态栏下沿，没有呼吸间距，视觉上"距离状态栏太近"。

## 需求

在顶部安全区之外为 MNavbar 增加约 8px 的固定呼吸间距，使标题行与状态栏下沿保留视觉余量；同时统一全项目的 navbar 顶部避让公式。

## 技术方案（token 化）

### 1. 新增 token（`src/theme/index.scss`）

在 `.m-app` 安全区桥接处新增：

```scss
--m-navbar-pt: max(16px, calc(var(--m-safe-area-top, 0px) + 8px));
```

### 2. MNavbar.vue

`padding-top` 改为 `var(--m-navbar-pt, 16px)`（保留兜底）。

### 3. 内容避让公式全部改用 token

以下 13 处（grep `max(1*px, var(--m-safe-area-top` 全量确认）把 `max(Xpx, var(--m-safe-area-top, 0px))` 替换为 `var(--m-navbar-pt, 16px)`（SongsPage 保留其 +6px 差值）：

| 文件 | 位置 | 现值 | 新值 |
|---|---|---|---|
| AlbumsPage.vue:90 | content pt | `max(16px,…)+44px` | `var(--m-navbar-pt)+44px` |
| ArtistsPage.vue:98 | content pt | 同上 | 同上 |
| LibraryDetailPage.vue:359,367 | content pt ×2 | 同上 | 同上 |
| PlaylistDetailPage.vue:277,285 | content pt ×2 | 同上 | 同上 |
| PlaylistsPage.vue:256 | content pt | 同上 | 同上 |
| SettingsPage.vue:164 | content pt | `max(var(--m-spacing),…)+44px` | `var(--m-navbar-pt)+44px` |
| SongsPage.vue:838,845,857,934,1110 | content/toolbar ×5 | `max(22px,…)+44px(…) ` | `calc(var(--m-navbar-pt) + 6px) + 44px(…)` |
| SourcesPage.vue:944,951 | content pt ×2 | `max(16px,…)+44px(…+8px)` | `var(--m-navbar-pt)+44px(…+8px)` |

### 4. 顺带修复 SongsPage 覆盖隐患

`SongsPage.vue:790` 的 `:deep(.m-navbar) { padding-top: 22px; }` 会完全覆盖安全区感知（Android 状态栏会压住标题行）。改为：

```scss
padding-top: calc(var(--m-navbar-pt, 16px) + 6px); /* 16+6=22px 保持椒盐基准 */
```

与内容避让公式（+6px）保持一致。

### 5. spec 同步

`.trellis/spec/frontend/component-guidelines.md` 中 navbar 避让契约（`padding-top: calc(max(16px, var(--m-safe-area-top, 0px)) + 44px)`）更新为 token 写法，并记录 `--m-navbar-pt` 契约与 SongsPage +6px 差值。

## 验收标准

- [ ] Android 真机/模拟器上 MNavbar 标题行与状态栏下沿有约 8px 间距。
- [ ] 所有内容避让公式改用 `--m-navbar-pt`，滚动内容与吸顶 navbar 无错位/重叠/空带；SongsPage 工具条 sticky 位置同步正确。
- [ ] 浏览器（safe-area-top = 0）下 MNavbar 顶部仍为 16px，SongsPage 为 22px，布局不变。
- [ ] SongsPage 在 Android 上不再被状态栏压住标题行。
- [ ] `npm run build` 通过；`npm run lint` 无新增报错。
- [ ] `.trellis/spec/frontend/component-guidelines.md` 契约描述同步更新。

## 非目标

- 不改动底部安全区、MiniPlayer 定位等无关几何。
- 不调整 44px 内容行高度与水平安全区处理。
- 不处理 theme/index.scss 中 PlayerPage/弹层等非 navbar 的 `--safe-area-inset-top` 直接消费（它们语义不同）。
