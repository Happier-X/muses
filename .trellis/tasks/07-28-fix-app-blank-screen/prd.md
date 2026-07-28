# 修复应用启动白屏问题

## Goal

应用打开后出现白屏（界面不渲染）。需定位根因并修复，使应用正常显示首屏。

## 背景（已确认事实）

- 当前仓库 `main` 分支，最近一次实质改动是 `6043084 refactor: drop tests and adopt Vue 3 declarative DOM for player interactions`：
  - 移除全部 `tests/unit` 与 `tests/e2e`（cypress）及 vitest/@vue/test-utils/jsdom 依赖
  - MiniPlayer / PlaylistDetailPage / QueuePage：用 `@click.stop` 替代 composedPath + classList.contains
  - SongsPage FAB：用 `songRowRefs` Map（collectRowRef）替代 querySelectorAll('[data-song-id]')
  - PlayerPage 手势：用 `lyricPanelRef` / `lyricPlayerRef` / `progressRangeRef` + Node.contains 替代 closest('.lyric-panel, .lyric-player') / .progress-range
  - 删除仅测试/命令式 DOM 用的标记类：player-actions, more-button, remove-button, amll-background{,-render}, immersive-shell, mini-player, app-mini-player, app-player-page, m-cover
- 技术栈：Vue 3.5 + vite 8 + vue-router 4 + Tailwind v4 + Capacitor 8（Android）。入口 `src/main.ts` → `router.isReady().then(() => app.mount('#app'))`。
- `npx vue-tsc --noEmit` 通过，无类型错误。
- 首屏路由：`/` → `/tabs/songs`，TabsPage 同步引入，SongsPage 异步引入。

## Requirements

- 定位白屏根因 ✅（已实测：`amll-pixi` chunk 顶层 `TypeError: t is not a function`，modulepreload 早期求值失败）。
- 选定修复方向并实施，使应用在 MuMu/Capacitor 环境正常显示首屏并能交互。
- 不引入回归（不扩大改动到与白屏无关的逻辑）。

## Acceptance Criteria

- [x] 目标运行环境（MuMu 模拟器 / Capacitor Android）打开应用，首屏（`/tabs/songs`）正常渲染，无白屏。
- [x] 浏览器控制台无 `TypeError: t is not a function` 等阻断性异常。
- [x] `npm run build` 通过（vue-tsc + vite build）。
- [x] AMLL/PIXI 仍保留在异步 `PlayerPage` chunk，未禁用动态背景；MuMu 仅报告 `EXT_color_buffer_float not supported` GPU 能力降级警告，未阻断初始化。

## Out of Scope

- 恢复被删除的测试框架（本任务是修白屏，不重建 vitest/cypress）。
- 其他与白屏无关的重构或样式美化。

## 根因（已实测定位）

通过 adb 连接 MuMu 模拟器（`emulator-5556`），用 Chrome DevTools Protocol 远程调试 Capacitor WebView（当前 URL `https://localhost/`），reload 后捕获到阻断性 JS 报错：

```
TypeError: t is not a function
  at https://localhost/assets/amll-pixi-D2OhIyWr.js:3:53818
  at cap.handleWindowError
```

- 报错位于独立 chunk `amll-pixi`（由 `@applemusic-like-lyrics/*` + `@pixi/*` 组成，`vite.config.ts` 里 `manualChunks` 合并）。
- 该 chunk 被 `index.html` 以 `<link rel="modulepreload">` 预加载，模块顶层立即执行即抛错，阻断后续脚本 → 整个应用挂载失败 → **白屏**。
- 报错代码段（amll-pixi-D2OhIyWr.js 第3行 ~53818 列）是 PIXI 设备检测（`@pixi/utils` 的 `ua-parser` 风格逻辑）后的 `var np=t(((e,t)=>{...})`，即 esbuild 的 `__toESM`/CommonJS 桥接辅助函数 `t` 在 modulepreload 早期求值时尚未定义。
- 次要非致命报错：`Error injecting safe area CSS: Cannot read properties of null (reading 'style')`（Capacitor 早期注入脚本的 safe-area CSS 注入，未阻断）。

### 与最近重构的关系（已确认）

`6043084` 对 `vite.config.ts` 仅删了 vitest 的 `/// <reference>` 与 `test` 配置块，**`manualChunks` 完全没变**。`amll-pixi` 的 chunk 策略在重构前即存在，故白屏非本次回归。

## 最终根因（精确定位）

Chunk 级循环依赖：
- `amll-pixi-D2OhIyWr.js` 开头：`import{... S as t ...}from"./storage-DUepHSwK.js"`，随后顶层 `np = t(((e,t)=>{...eventemitter3 CJS...})` 立即调用 `t`（`__commonJS` 风格包裹 helper）。
- `storage-DUepHSwK.js` 开头：`import{... ut as S ...}from"./amll-pixi-D2OhIyWr.js"`。
- 即 `amll-pixi` 与 `storage` 互相 import：`t`（=storage 的 `S`）实际是 storage 再从 amll-pixi 导入的 `ut` 的 re-export。模块求值时必有一方先跑，先跑方用对方导出时对方尚未初始化 → `t === undefined` → `TypeError: t is not a function` → 阻断挂载 → 白屏。

根因是 `vite.config.ts` 的 `manualChunks` 把 `@applemusic-like-lyrics/*` + `@pixi/*` 强制单独成 `amll-pixi` chunk，而 Rolldown/Vite 8 把 `__commonJS` interop helper（eventemitter3 是 CJS）放进了被多个模块共用的 `storage` chunk，形成跨 chunk 循环。与具体某某 xxx 绑定无关，与最近重构无关。

## 解决方向决策（已定方向 B 及落实）

用户选择方向 B（通过更新解决）。调研结论：
- `@applemusic-like-lyrics/core|vue` 已是 npm 最新 0.5.2，无法升级。
- `@pixi/app|core|display|sprite|filter-*` 子包最新即 7.4.3，无法升级；升 pixi v8 单包会破坏 amll 的 peerDeps（要求 `@pixi/*` 子包 ^7.4.3），不可行。
- Vite 8.1.5 已最新；`@vitejs/plugin-legacy` 可从 8.2.1 升到 8.2.2，但其 8.2.2 修复的是 Safari < 16 legacy chunk 不加载问题（`vitejs/vite#22008`），与我们的 modern chunk 循环依赖无关。

**因此纯升级 npm 包不能解决此问题。** 最终通过更新构建配置解决：移除 `@applemusic-like-lyrics` / `@pixi` 的 `manualChunks` 强制分块，让 Rolldown 自动处理依赖边界。产物中 AMLL/PIXI 保留在异步 `PlayerPage` chunk，首屏体积边界未被破坏；34 个 modern chunk 的静态 import 图无环。

## Notes

- 已覆盖勘察：入口 index.html / main.ts、App.vue、router、四个重构文件 diff、controller.ts、vite.config.ts、pixi/amll 依赖版本、dist 产物结构、logcat、CDP 实时 console。
- MuMu 环境关键属性：Android 15、x86_64、Chromium WebView 110.0.5481.154；WebView 被判为 modern 浏览器（`import.meta.resolve` 支持），走 modern chunk 而非 legacy。
- Capacitor `loggingBehavior: 'none'` 导致 JS console 不进 logcat，必须用 CDP 抓取。
