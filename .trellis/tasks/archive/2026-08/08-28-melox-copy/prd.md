# 抄 MeloX 歌词与背景效果

## Goal

直接复刻 `MeloX-Android` 的沉浸式背景与歌词效果，替换当前 `PlayerScreen` 的对应实现。

## 背景

- 当前原生重构的沉浸式页与 `MeloX` 效果差距大
- 用户要求直接抄 `MeloX-Android` 的 `MeloXFlowingLightBackdrop` 与 `MeloXIOSLyricsPanel` 的逐词高亮、翻译、背景虚化

## Requirements

### R1: 背景
- 移植 `MeloXFlowingLightBackdrop` 的流体渐变与封面虚化

### R2: 歌词
- 移植 `MeloXIOSLyricsPanel` 的逐词 `alpha`/`ExtraBold`、翻译/音译、和声、点击跳转

### R3: 集成
- 在 `PlayerScreen` 中替换当前 `AmllWebView` 的背景与 `LazyColumn` 歌词为 `MeloX` 的 `Compose` 实现
- 复用 `PlayerViewModel` 的 `lyricsJson` 与 `coverUrl` 数据

## Acceptance Criteria

- [x] 背景与 `MeloX` 一致 — MeloXFlowingLightBackdrop 三 Blob 流体 + 封面 blur 32dp + scrim，MuMu 截图验证通过
- [x] 歌词逐词高亮与 `MeloX` 一致 — MeloXIOSLyricsPanel 逐词 alpha/ExtraBold、翻译/音译、和声 italic、点击跳转、自动居中滚动
- [x] MuMu 上验证通过 — 1080x1920 emulator-5556 横滑切面板、逐词高亮、流体背景均正常（截图 mumu_melox.png / mumu_melox2.png）

## Notes

- 直接参考 `MeloX-Android` 的 `MeloXFlowingLightBackdrop.kt` 与 `MeloXIOSLyricsPanel.kt`
- 实现文件：`feature/player/backdrop/MeloXFlowingLightBackdrop.kt`、`feature/player/lyric/MeloXIOSLyricsPanel.kt`、重构 `PlayerScreen.kt`
- 规范同步：`.trellis/spec/android/index.md` 与 `features-lyrics-playlist.md §7` 已更新，WebView 标记为归档废弃
