# 应用秒开优化

## Goal（目标与用户价值）

应用启动后“白屏几秒”才显示首屏，目标是让首屏（TabBar + 歌曲 / 曲库列表）在应用打开后**秒级可见**，把播放器全屏页（AML / Pixi 渲染）甩到首屏按需加载链路上。

用户价值：打开应用即看到内容，不再有空白等待；点击全屏播放器时才承担其 WebGL 库的加载成本。

## Background（确认事实 / 代码证据）

| 事实 | 证据 |
|---|---|
| 主 JS bundle 巨大 | `dist/assets/index-Cmh3Xe0W.js` = 1.5MB；总产物 3.5MB（含 legacy 1.8MB） |
| 播放器全屏页被静态打进主 bundle | `src/App.vue:23` `import PlayerPage from '@/views/PlayerPage.vue'`（非异步） |
| PlayerPage 依赖重量级 WebGL 库 | `src/views/PlayerPage.vue:136-140` 引入 `@applemusic-like-lyrics/core` + `@applemusic-like-lyrics/vue` + `@applemusic-like-lyrics/lyric`（内含整套 `@pixi/*`） |
| Pixi 实际进了主 bundle | 主 bundle 内 `pixi` 关键字出现 25+ 次、`PIXI` 2 次、`BackgroundRender`/`LyricPlayer` 各 1 次 |
| 首屏只渲染 TabBar + 当前 Tab | `src/router/index.ts` 各 Tab 已 `() => import(...)` 懒加载，但被主 bundle 里的 Pixi 拖累，懒加载收益被抵消 |
| HTML 容器纯空 | `index.html` `<div id="app"></div>` 无骨架 / 无主题底色，白屏期间用户看到空白色块 |
| 无 splash 配置 | `capacitor.config.ts` 未配置 splash；`package.json` 无 `@capacitor/splash-screen` |
| legacy 插件产出双份产物 | `vite.config.ts` 启用 `@vitejs/plugin-legacy`，targets 门槛为 `Chrome >= 67` 等老浏览器；安卓 Capacitor 走系统 WebView，多为现代内核 |

## Requirements（需求）

- **R1 首屏不加载播放器全屏页的重量级库**
  `@applemusic-like-lyrics/*`、`@pixi/*` 不得进入首屏必须同步下载的主 bundle；仅在用户首次打开全屏播放器时按需加载。
  锚点：`src/App.vue:23-24`、`src/views/PlayerPage.vue:136-140`。

- **R3 大库显式分包，利于长期缓存**
  `vite.config.ts` 配置 `build.rollupOptions.output.manualChunks`，将 `@applemusic-like-lyrics`、`@pixi`、`@ionic/vue`、`vue`+`vue-router` 拆为独立 chunk。

- **R4 功能不 Regression**
  播放器全屏页（背景动画、歌词、播放控制、队列）、TabBar 路由、回退按钮逻辑、MediaSession 等全部保持现有行为。

## Acceptance Criteria（验收标准）

- [ ] **AC1 主 bundle 体积下降**：构建后首屏主入口 JS（不含异步 chunk）gzip 前体积 ≤ 700KB（目前 1.5MB）；`grep -c "pixi" <主chunk>` 为 0。
- [ ] **AC2 Pixi/AMLL 被拆进独立 chunk**：构建产物中出现独立 chunk 文件包含 `@applemusic-like-lyrics` / `@pixi` 内容，且不在首屏主入口同步加载链路上（通过 `build.rollupOptions.output.manualChunks` + 异步 import 实现）。
- [ ] **AC3 播放器全屏页可按需加载并正常工作**：点击 MiniPlayer 打开 `PlayerPage` 后，背景动画、歌词、播放/暂停/上下首/拖动进度/队列/回退逻辑全部正常（手测通过）。
- [ ] **AC4 构建/类型检查通过**：`npm run build`（含 `vue-tsc`）通过，无新增类型错误；`npm run lint` 通过。
- [ ] **AC5 无新增运行时报错**：在 WebView 中无 console error 与 white screen（手测冷启动）。

## Out of Scope（不在本次范围）

- **首屏骨架 / 主题底色（R2 暂缓）**：本轮用户明确“先不弄这个”，`index.html` 不加内联骨架/logo，白屏体感改进留待后续。
- 引入原生 splash screen 插件（`@capacitor/splash-screen`）——原生 splash 后续单列。
- 移除 `@vitejs/plugin-legacy` / 调整 targets（按推荐保持现状，作为后续可选优化）。
- 首屏空闲 prefetch 其余 Tab chunk（可后续优化）。
- 后端 / 数据加载性能（库扫描、WebDAV 拉取）——属另一链路。

## Open Questions（仍待确认）

无。R2 已按用户本轮意愿降级为 out of scope；legacy 保持现状（Q2 按推荐默认）。
