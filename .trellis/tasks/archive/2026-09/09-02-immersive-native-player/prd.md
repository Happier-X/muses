# 沉浸式播放页原生重构（替换WebView）

## Goal
将沉浸式播放页由当前的单一 WebView 整页方案（`FullPlayerWebView` + `app/src/main/assets/amll/*`）重构为纯原生 Compose 实现，消除 WebView 依赖，提升启动/滑动性能、可维护性与离线稳定性，同时保留沉浸式体验。用户价值：更流畅的下滑关闭、手势跟手、进度控制与歌词逐词渲染，无 WebView 内存/CORS/缓冲抖动问题。

## Background
- **现状（已验证）**：
  - `feature/player/PlayerScreen.kt` 当前为 `Box(offset drag) + FullPlayerWebView(full params)`，手势由 WebView 的 `onTouchListener` 分流（`isLyricPanelActive/isLyricAtTop/bottomExclusion 180dp`）并回调 `onDragOffsetUpdate/onDragDismiss` 驱动 Compose `dragOffsetY`；面板由 JS `activePanel` 控制（200% 宽度 + `translateX(-50%)`）。
  - `FullPlayerWebView.kt` / `LyricWebView.kt` 均采用 `WebViewAssetLoader https://appassets.androidplatform.net` + `LAYER_TYPE_HARDWARE/TRANSPARENT` + `isPageReady` 闸门 + `file://→dataURL` 封面 + 32ms `updateProgress/updateTime` 轮询；`LyricsPanel.kt` 内 `LyricWebView` 承载 AMLL `amll.bundle.js` + `BackgroundRender` 流体。
  - 同时仓库已存在一套未接线的原生 Compose 实现（`PhoneImmersiveLayout`/`TabletImmersiveLayout`/`InfoPanel`/`CoverHero`/`ProgressSection`/`ControlsRow`/`ModeBarRow`/`TabletBottomBar`/`FlowingLightBackdrop`），以及基于 `com.mocharealm.accompanist:lyrics-core/ui` 的 `KaraokeLyricsView` 卡拉OK渲染（逐词 `BlendMode.DstIn` 连续插值、Blur/Scale 发光），`PlayerViewModel` 已提供 `stickyCover/parsedLines/syncedLyrics/hasTranslation/translationEnabled/lyricPosition(100ms 钳制) / isBuffering / duration兜底` 等完整数据流。
  - `PlayerViewModel` 已支持队列循环/随机、粘性封面、冷启动进度兜底等契约；详见 `android/index.md` 沉浸式段落与 `features-lyrics-playlist.md`。
- **动机**：用户明确要求“不用 WebView”，回归原生以简化链路、降低排查成本、避免 Chromium 冻结/白屏与 JS Bridge 竞态。

## Requirements
- **R1 — 去 WebView**：移除 `FullPlayerWebView` / `LyricWebView` / `app/src/main/assets/amll/*` 对沉浸式主路径的依赖；沉浸式页不再创建 `WebView` 实例（可保留独立调试入口但默认不进入）。
- **R2 — 背景**：复用或重构 `FlowingLightBackdrop`（封面虚化 28dp+scale1.08+alpha0.75、 fallback 纵向渐变、3 Blob 流体 `flowSpeed=2` 12s 周期、上下 scrim 与顶部高光），`coverUri=stickyCover`，`hasLyric` 语义保留但不卸载背景。
- **R3 — 布局**：还原手机/平板双形态（`isTabletLayout = w>=768 && h<w` 横屏平板判定）：
  - 手机：固定头部 `FixedSongHead`（title/artist，顶部 `16+safe/24`）+ `HorizontalPager` 双面板（info/lyric 各 50%，0.22s easeOut `CubicBezier(0,0,0.58,1)`），`Panels width 100% pager` 替代旧 200% Row。
  - 平板：左右双栏各 50% + 底部全宽控制条 `TabletBottomBar`（进度全宽+三段式按钮），`info-panel` 内控制隐藏由底部条承担，封面居中 `CoverHero max min(50vh,420dp)`。
  - 窄高断点 `<=720/520` 的 gap/尺寸收紧保留。
- **R4 — 封面与信息**：`CoverHero` 1:1 正方形、圆角 12dp、占位 `White 0.06` + MusicNote；标题/艺术家单行省略。
- **R5 — 进度与缓冲**：自绘进度条（底轨 `White 0.25` 4dp + 填充 `White`，无 Material thumb），支持 tap/drag seek（`canSeek=duration>0`），显示 `formatTime(displayPos) / formatTime(duration)` 与 `isBuffering` “缓冲中” 提示；`onSeekStart/onSeekEnd` 暂停 `lyricPosition` 轮询钳制。
- **R6 — 控制**：`ControlsRow` 三键 lg（Prev/PlayPause/Next，gap `clamp(24,10vw,44)` 矮屏收紧）+ `ModeBarRow` 四键（RepeatOne/Repeat/Shuffle/List + Queue + More，max320/280/260，`spaceBetween`），图标准确，无 `is-active` 样式。
- **R7 — 歌词（手搓原生，不依赖 lyrics-ui）**：自研 Compose 卡拉OK渲染，还原 AMLL 逐词/逐字连续填充（`BlendMode`/`saveLayer` 按 `word.start→end` 进度扫过，长词字符级动效）、当前行发光（`BlendMode.Plus`）、非当前行按距离的 Blur/Scale/Alpha 衰减、和声/间奏样式、自动滚动聚焦当前行；数据源 `syncedLyrics`（`KaraokeLine.syllables` 逐词时间轴），`positionProvider` 仍为 VM 100ms 锚点 + UI 帧线性外推以达 60fps，无重组风暴；支持翻译开关（有译文时 FAB，3s 显隐）。
- **R8 — 手势**：下滑关闭（阈值 `0.18*h clamp 96-160dp`，底部 180dp 排除；信息页直接下滑，歌词页仅顶部时下滑，否则滚动；跟手 `offset` 非 `graphicsLayer` 以触发重绘）；左右横滑切面板；拖动时 `isDraggingVertically` 与回弹 `0.22s easeOut` 闭环（`startRebound` 显式，不依赖 watch 隐式）。
- **R9 — 数据契约不变**：沿用 `PlayerViewModel` 现有 StateFlow（`isPlaying/position/duration/repeatMode/shuffleModeEnabled/stickyCover/parsedLines/syncedLyrics/hasTranslation/translationEnabled/lyricPosition/isBuffering/playbackError`）与 `onSeek/playPause/skip/toggleRepeat/toggleShuffle/toggleTranslation/onSeekStart/onSeekEnd` 方法；不改动播放内核与队列持久化。
- **R10 — 资源清理**：彻底删除主路径 `FullPlayerWebView.kt` / `LyricWebView.kt` 引用与 `app/src/main/assets/amll/amll.bundle.js/full-player.{js,css}/index.html` 首屏依赖（Git 可回溯），不保留调试预览入口；更新 `spec/android/index.md` 沉浸式段落。

## Acceptance Criteria
- [ ] AC1 — 打开沉浸式页不创建 WebView 实例（通过 `WebView.setWebContentsDebuggingEnabled` 日志/内存检查验证），背景、封面、标题/艺术家正常显示（有/无封面 fallback 均正确）。
- [ ] AC2 — 手机/平板布局 1:1 还原：手机固定头部不随面板移动、双面板 `HorizontalPager` 横滑 0.22s easeOut 切换；平板横屏下左右双栏 + 底部全宽条，竖屏平板回退手机形态（`h>w` 时）。
- [ ] AC3 — 进度条可 tap/drag seek，显示正确分秒与缓冲提示；拖拽时预览值跟手、松手回写 `PlayerViewModel`；冷启动暂停态进度不为 0（`duration` 兜底 + `lyricPosition` 立即同步）。
- [ ] AC4 — 播放/上一曲/下一曲/循环/随机/队列/更多 均响应且图标随状态切换（RepeatOne/Shuffle/List）；平板底部条与手机 `ModeBar` 同步。
- [ ] AC5 — 歌词逐词渐变连续（长词字符级动画）、当前行高亮发光、非当前行模糊缩放、翻译开关生效、无词空态文案显示；滚动不与下滑手势冲突（歌词未在顶部时下滑不关闭）。
- [ ] AC6 — 下滑关闭：阈值 96-160dp，信息页任意位置可下滑关闭，歌词页仅顶部可下滑关闭；跟手位移实时、松手回弹 220ms easeOut 或关闭；底部 180dp 内不触发关闭（交给进度/按钮）；关闭后 `dragOffsetY` 归零，无半屏残留。
- [ ] AC7 — 性能与稳定性：沉浸式页滑动/滚动帧率达标，无 WebView 频刷 `evaluateJavascript 32ms` 的 UI 线程抖动；无 `file://→dataURL` 转码链路；内存不因 WebView 泄漏。
- [ ] AC8 — 回归：`./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest` 通过；`spec/android/index.md` 沉浸式契约已更新。

## Out of Scope
- 播放内核（`PlaybackService`/`PlayerConnection`）、队列持久化、WebDAV 缓存/限流、刮削/在线歌词搜索链路（`core:lyrics/core:scrape`）的改动。
- 迷你播放条、队列页、编辑页（仅 `onOpenQueue/onOpenEditMeta/onClose` 回调保留）。
- 在线封面/文本元信息自动补全（已下线，仅刮削页手动）。
- 主题/Salt 组件体系新设计（沿用现有 `SaltIconButton` 等）。

## Open Questions
（无）

## Decisions
- D1 — 还原度：采用推荐方案——以现有未接线原生组件为基线做 1:1 还原，允许 Blob 颜色/字号 clamp 等非核心视觉做原生合理微调（2026-09-02 确认）。
- D2 — 歌词渲染：不使用 `lyrics-ui KaraokeLyricsView`，自研 Compose 手搓 AMLL 动效（2026-09-02 确认，原因：非官方且效果不符预期）。
- D3 — 资源清理：按推荐彻底删除主路径 `FullPlayerWebView/LyricWebView` 引用与 `amll.bundle.js/full-player.*` 首屏依赖，Git 历史可回溯，不保留调试预览入口（2026-09-02 确认）。
- D4 — 验证方式：不新增截图对比自动化，开发自测通过后安装至 MuMu 模拟器由用户手动验收（2026-09-02 确认）。

## Notes
- 现有未接线原生组件已可用，建议以此为基线重构而非从零搭建（见 `PlayerScreen.kt: PhoneImmersiveLayout/TabletImmersiveLayout/FlowingLightBackdrop`）。
- `rememberLyricPositionProvider` 的锚点+帧外推模式必须保留，避免重组风暴。
