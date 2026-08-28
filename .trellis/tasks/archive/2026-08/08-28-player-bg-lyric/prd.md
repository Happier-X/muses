# 沉浸式背景与歌词未渲染

## Goal

沉浸式播放页的流体背景与 AMLL 歌词在 MuMu 模拟器上可见，WebView 内容层正确渲染。

## 背景

- 当前原生回退标题可见，但 WebView 的 `#background-layer` 与 `#lyric-layer` 仍黑屏/空白
- 日志显示 `updatePlayerState` 与 `updateLyrics` 已注入且 `hidden=false`，但 `screencap` 仍黑，`window.innerHeight` 640 正常

## Requirements

### R1: 背景渲染
- `background-layer` 的 PIXI 流体背景在 WebView 中可见
- 封面经 `coverUriToAppAssetsUrl` 映射后，`CacheDirPathHandler` 能正确提供 `/cache/` 图片

### R2: 歌词渲染
- `updateLyrics` 载荷经 `AmllMapper.toJson` 序列化后，`AmllLyricLine` 在 `lyric-layer` 中可见
- 切歌时 `payloadJson` 与 `isPlaying` 联动，`updatePosition` 驱动高亮

### R3: 验证
- MuMu 上打开沉浸式页，背景有模糊/渐变，歌词可见且随进度高亮
- `logcat` 无 `updateLyrics` 解析失败，`onConsoleMessage` 无 JS 报错

## Acceptance Criteria

- [ ] 背景层可见（非纯黑）
- [ ] 歌词行可见
- [ ] 切歌时背景与歌词同步更新

## Notes

- 复用 `AmllWebView` 的 `jsReady` 与 `WebViewAssetLoader` 日志
