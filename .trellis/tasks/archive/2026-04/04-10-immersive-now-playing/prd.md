# brainstorm: immersive-now-playing

## Goal

为 Muses 音乐播放器新增一个沉浸式"正在播放"全屏页面：点击底部 PlayerBar 时从底部滑出占满全屏，包含大封面、播放控制、进度条等。

## What I already know

- PlayerBar.kt: 底部迷你播放器，40dp 封面，播放/暂停、队列按钮，2dp 进度条
- MainActivity.kt: 用 ModalNavigationDrawer 管理主导航，PlayerBar 固定在底部，无全屏播放器
- PlayerViewModel: 已暴露所有 playback 控制 API（seekTo、togglePlayPause、skipToNext/Previous、toggleShuffle、cycleRepeatMode）
- PlayerState: isPlaying, title, artist, album, albumArtUri, positionMs, durationMs, queue, currentIndex, shuffleModeEnabled, repeatMode
- Theme: Material3 + dynamic colors，dark/light mode 支持
- QueueSheet: ModalBottomSheet，已用于队列弹窗
- Album art: rememberAlbumArt() 使用 BitmapFactory RGB_565 下采样，Composable 状态

## Assumptions (temporary)

- 交互：点击 PlayerBar 整体区域打开全屏，滑下或点击返回按钮关闭
- 动画：底部滑出的半透明/模糊背景 + 页面内容，内容区用 AnimatedVisibility + slideInFromBottom / slideOutToBottom
- MVP 只包含核心播放 UI，音量控制、歌词等暂不包含

## Open Questions

~~**MVP scope**: 除了封面+控制+进度条，是否包含 shuffle/repeat 切换按钮？~~ ✅ **已确认：包含 shuffle/repeat 按钮**

## Requirements (evolving)

**已确认 MVP 范围：**

1. **触发方式**：点击 PlayerBar 整体区域打开全屏；下滑关闭 + 点击返回按钮关闭
2. **全屏布局（从上到下）**：
   - 顶部栏：返回按钮 + 占位（可后续扩展）
   - 封面区：大尺寸专辑封面（宽度尽量占满，左右留 padding 16-24dp），无封面时显示 MusicNote 占位图标
   - 曲目信息：歌曲名（大字） + 艺术家（中字），居中或左对齐
   - 进度条：可拖动 Slider，显示「当前时间 / 总时长」
   - 控制栏（从左到右）：Shuffle → 上一首 → 播放/暂停 → 下一首 → Repeat（循环按钮）
   - shuffle/repeat 图标根据 PlayerState 状态高亮或灰显
3. **动画**：底部滑入/滑出，300ms ease-in-out
4. **主题**：跟随 MusesTheme（深色/浅色 + dynamic colors）

## Acceptance Criteria (evolving)

- [ ] 点击 PlayerBar 触发全屏播放页滑出动画
- [ ] 全屏页显示大封面（占满宽度，留 padding）
- [ ] 全屏页显示曲目名 + 艺术家
- [ ] 进度条可拖动 seek
- [ ] 上一首 / 播放暂停 / 下一首 按钮正常工作
- [ ] Shuffle 按钮可切换 shuffle 模式
- [ ] Repeat 按钮可在 off/all 之间切换
- [ ] 下滑或点击返回按钮关闭全屏页
- [ ] 现有 PlayerBar 行为不变
- [ ] 深色/浅色主题均正常显示

## Acceptance Criteria (evolving)

- [ ] 点击 PlayerBar 触发全屏播放页滑出
- [ ] 全屏页显示大封面（尽可能占满宽度，留边距）
- [ ] 全屏页显示曲目名 + 艺术家
- [ ] 全屏页有上一首 / 播放暂停 / 下一首 按钮
- [ ] 全屏页有可拖动的进度条，显示当前时间 / 总时长
- [ ] 下滑或点击返回按钮关闭全屏页
- [ ] 现有 PlayerBar 行为不变（播放控制仍然可用）

## Definition of Done

- 新增 NowPlayingScreen.kt composable
- MainActivity 添加 showNowPlaying 状态，点击 PlayerBar 打开全屏
- 所有 PlayerViewModel API 接入（播放控制、进度、shuffle/repeat）
- 进度条支持拖动 seek
- 动画流畅（slide up/down，300ms）
- 深色/浅色主题均正常显示

## Out of Scope (explicit)

- 音量控制滑块
- 歌词显示
- 播放列表管理（已有 QueueSheet）
- 专辑/艺术家详情页

## Technical Notes

- 现有文件: PlayerBar.kt, PlayerViewModel.kt, MainActivity.kt, QueueSheet.kt, AlbumArtLoader.kt
- 需要新增: NowPlayingScreen.kt
- 不使用 NavHost/Compose Navigation，直接用状态变量控制显示/隐藏
- 动画: AnimatedVisibility + slideInVertically/slideOutVertically from bottom
- 需要获取屏幕高度用于动态布局
