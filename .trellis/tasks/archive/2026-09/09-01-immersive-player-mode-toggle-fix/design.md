# 设计 — 沉浸式播放页底部播放模式和循环模式图标点击无响应

## 1. 背景与现状

- 沉浸式页 `PlayerScreen`：外层 `Box(offset {dragOffsetY}) + pointerInput(detectVerticalDragGestures)` 负责下滑关闭；内层单一 `FullPlayerWebView` 承载 `full-player.js/css + amll.bundle.js`。
- `FullPlayerWebView.kt`：`AndroidView(WebView)` + `WebViewAssetLoader https://appassets.androidplatform.net/assets/amll/` + `HARDWARE + TRANSPARENT` + `isPageReady` 闸门 + `32ms` 轮询 `updateProgress/updateTime/setPaused`；`addJavascriptInterface("Android") onAction/onLineClick/onPanelChange/onLyricScroll/log` + `AndroidDirect`；`setOnTouchListener` 按 `dy/dx` 分流并 `requestDisallowInterceptTouchEvent`，`isUserScrolling` 时 `configureLyricMotion(enableBlur:false)`。
- 前端 `full-player.js`：`initDom` 构建 `panels 200%`、`info-panel/mode-bar(#btn-repeat/#btn-shuffle)`、`bottom-bar(#bottom-repeat/#bottom-shuffle)`；`bindClick` 绑四枚循环/随机按钮 → 乐观切 `state.repeatMode/shuffleEnabled` + `classList.toggle/active` → `window.Android.onAction({action:'toggleRepeat'/'toggleShuffle'})`；Kotlin `onAction` 分派至 `onToggleRepeatState/onToggleShuffleState` → `PlayerScreen` 闭包 `viewModel.setRepeatMode/setShuffleModeEnabled`。
- 缺陷：点击无响应。候选根因：
  1. 外层 Compose `pointerInput detectVerticalDragGestures` 拦截了落到底部按钮区的触摸（WebView 未收到 `click`）。
  2. `FullPlayerWebView` 的 `OnTouchListener` 对 `down→up` 纯点击误判为待拦截，`requestDisallowInterceptTouchEvent(false)` 使父容器消费；`click` 事件未冒泡至 JS 按钮。
  3. JS `bindClick` 的 `touchstart/touchmove stopPropagation` 与 `panels` 横滑监听冲突，按钮处于 `panels` 容器内被 `touchmove preventDefault` 误消。
  4. `repeatMode/shuffleEnabled` 的 Kotlin 闭包捕获陈旧值或 `MediaController` 未连接导致 `set*` 无效，JS 乐观态被 32ms 回写覆写回旧值（视觉闪回即视为无响应）。

## 2. 目标与非目标

- 目标：定位并修复底部四枚按钮的点击链路（触摸分发→JS 事件→Bridge→ViewModel→回写），手机/平板均可切换且视觉与状态一致。
- 非目标：不重构沉浸式整页架构（保持单一 WebView），不改 Media3 存储与限流契约，不引入新依赖。

## 3. 方案总览

```
[触摸] 用户点击 #btn-repeat/#btn-shuffle 或 #bottom-repeat/#bottom-shuffle
  ↓ 排查 1：Compose 外层下滑手势是否消费
[WebView] FullPlayerWebView.OnTouchListener 分流（dy/dx + isLyricAtTop）
  ↓ 排查 2：requestDisallowIntercept 是否对纯点击放行
[JS] full-player.js bindClick(click) → state 乐观 + Android.onAction
  ↓ 排查 3：JS 事件是否被 panels 横滑拦截
[Bridge] Android.onAction toggleRepeat/toggleShuffle → post → ViewModel.set* → PlayerConnection → MediaController
  ↓ 排查 4：闭包与控制器是否可用
[回写] 32ms updateProgress(repeatMode/shuffleEnabled) → JS updateProgress → 按钮 active/图标定格
```

修复策略：按 1→4 顺序隔离验证，最小改动闭环。

## 4. 详细设计

### 4.1 触摸与手势分流

- **PlayerScreen 外层**：`detectVerticalDragGestures` 仅在 `!isLyricPanelActive || isLyricAtTop` 时启用；阈值 `clamp(0.18*h,96,160)`，跟手仅当 `dy>dx && dy>touchSlop`。问题点：该 `pointerInput` 挂在包裹 WebView 的 `Box`，Compose 的 pointer 会在 WebView 之前预消费。
  - 修复：为底部按钮区预留 `click` 不拦截；将 `pointerInput` 的 `awaitPointerEventScope` 改为仅消费垂直拖拽已确认后的序列，未达阈值前不消费（或加 `pointerInteropFilter` 的 `requestDisallow` 协作）。备用：把下滑手势下沉至 WebView 内部或仅在顶部 `head` 区域响应，底部 `mode-bar/bottom-bar` 区域不参与下滑判定。
- **FullPlayerWebView OnTouchListener**：
  - 现状：`ACTION_DOWN` 固定 `requestDisallow(false)`，`ACTION_MOVE` 按 `dy>dx` 决定是否 `true/false`，纯点击（无 MOVE）始终为 `false`（允许父拦截）。
  - 修复：`ACTION_DOWN` 不预设，`ACTION_UP` 前若累计位移 < touchSlop 则视为点击，`requestDisallow(true)` 保证这次序列由 WebView 消费，JS 的 `click` 得以触发；或直接在 `OnTouchListener` 中对按钮区域（通过 JS 坐标回传或固定底部 120dp）强制 `requestDisallow(true)`。

### 4.2 JS 事件链路

- **bindClick**：已对 `click` 绑 `stopPropagation` + `touchstart/touchmove stopPropagation` 以避 `panels` 横滑。需确保：
  - `click` 监听为 `passive:false` 且不被 `panels touchmove preventDefault` 覆盖；按钮的 `touchend` 不被 `panels touchend` 的 `setActivePanel` 误消。
  - 修复：为 `mode-bar` 与 `bottom-bar` 按钮加 `pointer-events:auto` 与 `z-index:3`，并在 `panels touchstart` 中若 `e.target.closest('.mode-bar,.player-page__bottom-bar')` 则直接 `return` 不进入横滑判定。
- **乐观与回写**：JS 乐观切 `state` 后，Kotlin 的 `updateProgress` 将在 32ms 内回写真实 `repeatMode/shuffleEnabled`。若 Kotlin 侧未更新，乐观态会被回写覆写。需保证：
  - Kotlin 闭包取最新值：`PlayerScreen` 的 `onToggleRepeat` 改为无参形式 `viewModel.toggleRepeat()`（内部读当前 Flow 值），避免捕获陈旧 `repeatMode`；同理 `toggleShuffle()`。
  - 或在 `FullPlayerWebView` 的 `onAction` 中直接基于 `repeatModeState.value.invoke()` 的现值计算下一个值再调 `set*`。

### 4.3 Bridge 与状态

- **PlayerViewModel**：`setRepeatMode(mode)` / `setShuffleModeEnabled(enabled)` 直通 `PlayerConnection`。需确认 `controller != null`，否则静默不生效。
  - 修复：若 controller 未就绪，暂缓或 `PlayerConnection` 内加 `pendingRepeat/pendingShuffle` 待连接后应用；并在 UI 侧对 `playbackState==IDLE` 仍允许切模式。
- **ViewModel 闭包**：将 `PlayerScreen` 的 `onToggleRepeat` 由 `if (repeatMode==ONE) ALL else ONE` 捕获值改为基于最新 `repeatModeState` 的计算，确保多击不因重组滞后而错。

### 4.4 日志与验证

- 保留 `Android.log` 的 `bindClick ok/bindClick miss/btn click/-> toggle*`；在 Kotlin `onAction` 分支也打 `Log.w("FullPlayer", ...)` 已有。
- 验证：`adb logcat -s FullPlayer:V` 观察 `btn click bottom-repeat toggleRepeat` → `-> toggleRepeat` → `PlayerConnection` 状态变更 → `updateProgress repeatMode=1`。

## 5. 兼容与回退

- 保持 `WebViewAssetLoader + HARDWARE + isPageReady` 闸门不变；仅调整触摸分流阈值与 JS 事件 `stopPropagation` 范围。
- 若修复后下滑关闭误触发率上升，回退手势改动，改为仅 JS 层对按钮区 `touchstart` 时 `requestDisallowInterceptTouchEvent(true)` 的定向放行。

## 6. 涉及文件

- `feature/player/PlayerScreen.kt`（手势分流、闭包写法）
- `feature/player/lyric/FullPlayerWebView.kt`（`OnTouchListener` 的点击放行、Bridge 分派、32ms 轮询）
- `app/src/main/assets/amll/full-player.js`（`bindClick` 的横滑互斥、乐观态）
- `app/src/main/assets/amll/full-player.css`（如需 `pointer-events / z-index` 微调）
- `feature/player/PlayerViewModel.kt`（可选 `toggle*` 便捷方法）

## 7. 异常与风险

- 点击与横滑同处 `panels` 容器，`preventDefault` 可能导致按钮点击被吞；风险通过 `closest` 判定规避。
- 底部区域 `requestDisallow(true)` 可能使下滑关闭在底部难以触发；权衡为按钮区优先，顶部/中部仍可下滑关闭。

## 8. 验证策略

- 编译：`assembleMusesDebug`。
- 单测：`feature:player` 保留单测；新增 `FullPlayerWebView` 桥接的轻量单测（如有）。
- 手工：MuMu/真机 手机与平板（横屏）分别验证四枚按钮的切换、视觉、回写、日志；回归横滑、垂直下滑、进度条、歌词 seek。
- 门禁：`lintMusesDebug`。

## 9. 回滚

- 回退 `PlayerScreen/FullPlayerWebView/full-player.js` 的手势与事件改动至当前 commit，模式切换回退为不可用状态，代价可接受。
