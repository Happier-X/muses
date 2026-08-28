# 沉浸式播放页黑屏三修

## Goal

沉浸式播放页在 MuMu 模拟器上打开后不再黑屏，完整显示背景、封面、歌词、控制条。

## 背景

- 已两次修复：`100vh` 高度、`NativeBridge` 递归、`join` 换行，但用户仍报告黑屏
- 最新构建在 `screencap` 中仍为纯黑，但 JS 日志显示 `updatePlayerState` 已成功设置 `hidden=false` 与标题，说明 DOM 已更新但 WebView 未渲染或被遮挡

## Requirements

### R1: 根因定位
- 抓取 `chrome://inspect` 或 `WebView` 控制台，确认 `updatePlayerState` 后 DOM 状态与计算样式
- 检查 `WebViewAssetLoader` 是否成功拦截 `/assets/` 与 `/cache/`，无 404
- 验证 `100vh` 在 WebView 视口（`window.innerHeight` 640）下是否生效，`#player-ui` 是否有高度

### R2: 修复
- 若 `screencap` 不捕获硬件加速 WebView，改用软件层或 `enableSlowWholeDocumentDraw` 使截屏与用户可见一致，或改用 `PixelCopy`
- 若 `resumeRender` 未定义导致 JS 抛错阻断渲染，改为 `if(window.resumeRender)` 保护
- 若 `updatePlayerState` 注入时序仍过早，确保仅在 `jsReady` 后注入

### R3: 验证
- MuMu 模拟器上打开沉浸式页，5 秒内可见封面/标题/控制，非纯黑
- `logcat` 无 `StackOverflow`、`SyntaxError`、`resumeRender is not a function`
- `screencap` 或 `PixelCopy` 能捕获到 WebView 内容

## Acceptance Criteria

- [ ] 真机/模拟器打开播放页，非黑屏
- [ ] `window.innerHeight` 640 时 `#player-ui` 高度 640
- [ ] `updatePlayerState` 后 `pp-title` 显示当前歌曲标题

## Notes

- 复用 `AmllWebView` 的 `jsReady` 握手与 `WebChromeClient` 日志
- 保持 `AudioTagReader` 与 `MiniPlayerBar` 修复不回归
