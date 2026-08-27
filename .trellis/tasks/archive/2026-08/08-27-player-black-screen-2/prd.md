# 沉浸式播放页黑屏修复（二次回归）

## Goal

沉浸式播放页（`PlayerScreen` + `AmllWebView`）打开后不再黑屏，背景/封面/歌词/控制可见。

## 背景

- 0.4.5 已通过 `100vh` 修复 `frontend/amll-web` 高度塌陷（`abd9db6c`），但用户在 MuMu 模拟器上再次报告黑屏
- 需排查是否因近期 `MusesApp` 导航/迷你条 `overlayRoute` 逻辑、`PlaybackService` 懒扫描或 `AmllWebView` 初始化回归

## Requirements

### R1: 复现与定位
- 在 MuMu 模拟器上复现黑屏，抓取 `logcat` 中 `AmllWebView`/`chromium` 相关错误
- 检查 `frontend/amll-web/src/style.css` 的 `100vh` 是否仍生效，`androidAssets` 是否为最新构建

### R2: 修复
- 若为 `AmllWebView` 加载失败，修复 `WebViewAssetLoader` 或 `index.html` 资源路径
- 若为 `PlayerScreen` Compose 布局高度为 0，修复 `Modifier.fillMaxSize` / `inset` 逻辑
- 若为 `overlayRoute` 导致内容被遮挡，修复 `TabsLayout` 显隐逻辑

### R3: 验证
- MuMu 模拟器上打开播放页，非黑屏，封面/进度/歌词可见
- `assembleMusesDebug` 通过

## Acceptance Criteria

- [ ] MuMu 模拟器打开沉浸式播放页，5 秒内可见内容
- [ ] logcat 无 WebView 资源加载 404/ERR
- [ ] 构建产物与源码一致

## Notes

- 上次修复仅改 CSS 高度，若 WebView 本身未加载则仍黑屏，需区分 CSS 塌陷与 WebView 加载失败
