# 沉浸式播放页底部播放模式和循环模式图标点击无响应

## 目标

修复沉浸式播放页（单一 WebView 整页，`FullPlayerWebView + full-player.js/css`）底部播放模式（随机/顺序，shuffle）与循环模式（列表循环/单曲循环，repeat）图标点击无响应的缺陷，使其在手机与平板布局下均可正常切换并同步播放器状态。

## 背景

- 现行沉浸式页为单一 WebView 整页：`PlayerScreen` 容器 + `FullPlayerWebView`（`WebViewAssetLoader https://appassets.androidplatform.net/assets/amll/`，硬件加速 + 透明）+ 前端 `full-player.js/css`（`panels 200%`、平板双栏、SVG 乐观切换）。
- 底部交互：手机为 `info-panel` 内的 `mode-bar`（`#btn-repeat / #btn-shuffle`），平板为 `player-page__bottom-bar` 内的 `controls-row`（`#bottom-repeat / #bottom-shuffle`），两者在 `full-player.js` 中经 `bindClick` → `window.Android.onAction({action:'toggleRepeat'/'toggleShuffle'})` 桥接至 Kotlin `FullPlayerWebView.onAction` → `PlayerScreen onToggleRepeat/onToggleShuffle` → `PlayerViewModel.setRepeatMode/setShuffleModeEnabled` → `PlayerConnection -> MediaController`。
- 缺陷现象：点击上述四枚图标无视觉或状态变化，播放模式与循环模式保持原值。
- 关联契约：沉浸式布局、WebView 资产加载、手势分流（`isLyricPanelActive && !isLyricAtTop`）、`updateProgress` 32ms 轮询等见 `features-lyrics-playlist.md §7`。

## 需求

### 功能需求

1. **点击可响应**：手机 `mode-bar` 的循环/随机按钮与平板 `bottom-bar` 的同款按钮，单击均能触发对应切换。
   - 循环：`REPEAT_MODE_ALL (0)` ↔ `REPEAT_MODE_ONE (1)` 互切。
   - 播放模式：`shuffleEnabled true ↔ false` 互切。
2. **视觉与状态一致**：点击后按钮的 `active` 态（白色高亮）与图标（`Repeat / RepeatOne`、`Shuffle / FormatListBulleted`）立即乐观切换，并在 32ms 轮询经 `updateProgress` 的真实状态回写后保持一致，不闪回。
3. **跨布局一致**：手机与平板（`≥768dp && landscape`）均满足 1、2；手机横竖、窄屏（`≤520`）不回归。
4. **不破坏既有交互**：单击不触发面板横滑、拖拽关闭、进度条拖拽；歌词点击 seek、播放/上一首/下一首、队列/更多等其它 `onAction` 保持可用。
5. **日志可诊断**：点击链路保留 `Android.log` 与 `FullPlayer` tag 的关键日志（`bindClick ok` / `btn click` / `-> toggleRepeat/Shuffle`），便于 `adb logcat -s FullPlayer` 验证。

### 非功能 / 约束

- 不改变现有 `PlayerConnection` / `PlaybackService` 的 Media3 契约，仅修复桥接与事件拦截。
- 不引入新的 WebView 引擎或额外依赖；保持 `WebViewAssetLoader + HARDWARE + TRANSPARENT + isPageReady` 闸门。
- 兼容 `WebView.setOnTouchListener` 的手势分流（歌词顶部下滑放行、横滑由 WebView 消费）与外层 `PlayerScreen` 的 `detectVerticalDragGestures` 下滑关闭。

## 验收标准

- [ ] 手机竖屏：点击 `mode-bar` 的循环图标，循环模式在 `REPEAT_MODE_ALL ↔ REPEAT_MODE_ONE` 间切换，按钮 `active` 与图标同步；再次点击可回切。
- [ ] 手机竖屏：点击 `mode-bar` 的随机图标，随机模式在 `顺序 ↔ 随机` 间切换，`Shuffle`/`FormatListBulleted` 互切且 `active` 同步；再次点击可回切。
- [ ] 平板横屏（`≥768dp landscape`）：`bottom-bar` 的循环/随机按钮同上可切换。
- [ ] 切换后经 `PlayerViewModel.repeatMode / shuffleModeEnabled` 观测到的状态与 UI 一致，重启沉浸页后保持（由 `PlayerConnection` 持久化的 MediaController 状态决定）。
- [ ] 点击不误触发面板切换或页面关闭；横滑切面板、垂直下滑关闭、进度条拖拽仍正常。
- [ ] `adb logcat -s FullPlayer` 可见 `btn click btn-repeat toggleRepeat` 等点击日志与 `-> toggleRepeat` 分派日志。
- [ ] 门禁：`assembleMusesDebug`、`lintMusesDebug`、相关单测通过，无回归。

## 边界与例外

- 无播放队列时切换随机/循环：仅改模式，不抛异常，不自动开始播放。
- WebView 未就绪（`isPageReady=false`）期间的点击：静默忽略，不崩溃，待就绪后恢复。
- 快速连点：以最后一次状态为准，不出现竞态导致的模式错乱。
