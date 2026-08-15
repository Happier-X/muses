# 播放条/FAB 玻璃改为椒盐式实心胶囊

## Goal

MiniPlayer 与 SongsPage 跳转 FAB 从「白色 72% 半透明玻璃（blur 20px）」改为椒盐音乐实测的**实心胶囊**：浅色纯白 `#fff` / 深色 `#1e1e1e`，移除 blur 与白边。

## Background

- **用户反馈**：玻璃的透明效果有点浅（太透发灰），跟椒盐音乐不一致。
- **椒盐实测**（MuMu 12.2.0，浅色/深色双主题像素测量）：
  - 浅色：MiniPlayer 胶囊内部**纯白 #ffffff 实心**（无半透明、无边框线、无 blur 痕迹），仅边缘极轻过渡；
  - 深色（`cmd uimode night yes`）：胶囊 **#1e1e1e 实心**（比页面背景 #181818 亮一档），图标/文字浅灰（#D6D9DC 左右）；
  - 列表页顶部工具条为平铺背景色（非玻璃）。
- **muses 现状**：MiniPlayer `rgba(255,255,255,0.72)` + `blur(20px)` + `1px 白边` + 阴影；SongsPage jump-fab 同款玻璃。项目仅这两处实际玻璃（index.scss 的 `.m-glass-*` 为历史兼容工具类未使用）。
- **改动**：
  - MiniPlayer：背景 `#fff`，移除 blur/白边，阴影调轻（0.08）；`__btn` 图标色 `#211715` 写死改为 `var(--m-text)`（深色自动变浅）；新增 `:global(.dark)` 覆盖 `#1e1e1e`（+深色阴影 0.35）。
  - SongsPage jump-fab：同款实心化；图标色 `#666` → `var(--m-text-2)`；新增深色覆盖 `#1e1e1e`。

## Requirements

- 浅色主题：播放条与 FAB 为纯白实心胶囊，无模糊、无白边；阴影轻。
- 深色主题（真机 .dark）：播放条与 FAB 为 `#1e1e1e`，图标色随主题变量。
- 不改布局几何（尺寸/圆角/定位）与交互；其余页面不受影响。

## Acceptance Criteria

- [ ] MuMu 浅色：MiniPlayer 纯白实心（像素 #ffffff），底部列表透不过胶囊；FAB 同款。
- [ ] 深色 CSS 覆盖存在且逻辑正确（模拟器 WebView 110 系统主题不传递属已知限制，真机验证）。
- [ ] `vue-tsc` / ESLint / 构建通过。

## Notes

- 轻量任务，PRD-only。改动：`src/components/MiniPlayer.vue` + `src/views/SongsPage.vue`。
- 已实测：浅色 MiniPlayer 区域主色 #ffffff（8283 样本）✓。
- 需同步更新 `.trellis/spec/frontend/component-guidelines.md` 的 MiniPlayer 底部几何/玻璃契约描述（「液态玻璃 72%」过时）。
