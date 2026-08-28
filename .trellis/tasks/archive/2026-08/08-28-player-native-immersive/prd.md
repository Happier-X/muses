# 沉浸式播放页原生重构

## Goal

废弃 `WebView` + `amll-web` 方案，改用原生 `Compose` 实现沉浸式播放页，复刻之前 `Capacitor` 版本的布局，并参考 `MeloX-Android` 的歌词渲染。

## 背景

- `WebView` 方案在 MuMu 上持续黑屏（`StackOverflow`、`SyntaxError`、`jsReady` 时序、`WebViewAssetLoader` 封面映射、`screencap` 硬件层等问题叠加）
- 用户决策：放弃 `WebView`，歌词改用 `MeloX-Android` 的 `MeloXIOSLyricsPanel` / `AMLL-DroidMate` 的 `AMLLLyricsView`（`accompanist-lyrics` + 逐词高亮），布局复刻 `Capacitor` 的 `PlayerPage.vue`

## Requirements

### R1: 移除 WebView
- 删除 `AmllWebView`、`frontend/amll-web` 的 `vite build` 及 `androidAssets` 依赖
- `PlayerScreen` 改为纯 `Compose`（`Box` + `Column` + `LazyColumn` 歌词）

### R2: 原生歌词
- 集成 `accompanist-lyrics` 或移植 `MeloXIOSLyricsPanel` 的逐词高亮、翻译/音译、背景虚化
- 复用 `PlayerViewModel` 的 `lyricsJson` / `AMLL` 行集与 `lyricPosition` 驱动

### R3: 布局复刻
- 复刻 `Capacitor` 的沉浸式布局：顶部标题/艺术家、封面 hero、五行小窗、进度条、三键控制、mode-bar
- 平板 `isTabletLayout` 分支保留

### R4: 背景
- 封面经 `SaltCover` 加载，背景用 `ArtworkDynamicPalette` 或 `MeloXFlowingLightBackdrop` 的模糊/渐变

## Acceptance Criteria

- [ ] MuMu 上打开沉浸式页，非黑屏，封面/标题/控制可见
- [ ] 歌词可见且随进度高亮，支持翻译开关
- [ ] 背景有模糊/渐变，非纯黑
- [ ] 切歌时封面/歌词/背景同步更新

## Notes

- 参考 `MeloX-Android` 的 `MeloXClassicNowPlayingScene` 与 `MeloXIOSLyricsPanel`
- 保留 `AudioTagReader` 与 `MiniPlayerBar` 修复
