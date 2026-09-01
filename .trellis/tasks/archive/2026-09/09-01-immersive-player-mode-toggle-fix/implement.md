# 实施计划 — 沉浸式播放页底部播放模式和循环模式图标点击无响应

## 前置检查

- [ ] `prd.md` / `design.md` 已评审
- [ ] 分支基于 `main` 最新，无未提交脏改

## 步骤

### 1. 定界与复现

- [ ] 1.1 复现：MuMu/真机 打开沉浸式播放页，手机与平板分别点击四枚按钮（`mode-bar` 与 `bottom-bar` 的循环/随机），`adb logcat -s FullPlayer:V` 观察 `bindClick ok / btn click / -> toggle*` 是否出现，及 `repeatMode/shuffle` 是否变化
- [ ] 1.2 判定根因分支：
  - 无 `btn click` → 触摸被 Compose/OnTouchListener 拦截（§4.1/4.2）
  - 有 `btn click` 但无 `-> toggle*` → Bridge 未到达 Kotlin（§4.2 JS Bridge）
  - 有 `-> toggle*` 但状态不变 → Kotlin 闭包/Controller 未连接（§4.3）

### 2. 代码修改（按分支择一或组合，控制改动半径）

- [ ] 2.1 `PlayerScreen.kt`：将 `onToggleRepeat/onToggleShuffle` 的闭包捕获改为无参 `toggle*` 或基于最新 Flow 值的计算，避免陈旧值
  - 可选在 `PlayerViewModel` 新增 `toggleRepeat()` / `toggleShuffle()`（内部读 `repeatMode.value / shuffleModeEnabled.value` 再 `set*`）
- [ ] 2.2 `FullPlayerWebView.kt`：`OnTouchListener` 对纯点击（位移 < touchSlop）强制 `requestDisallow(true)` 放行；或对底部 `mode-bar/bottom-bar` 区域定向放行；确保 `isPageReady` 后才消费 `onAction`
- [ ] 2.3 `full-player.js`：`panels touchstart/touchmove` 中若 `target.closest('.mode-bar,.player-page__bottom-bar')` 则不进入横滑逻辑；为按钮加 `z-index:3; pointer-events:auto`；保持 `bindClick` 的 `click + touchstart/move stopPropagation`
- [ ] 2.4 可选 `full-player.css`：微调按钮层级与点击区域（若被 `panels` overflow 裁剪）

### 3. 联调与自测

- [ ] 3.1 日志验证：点击后 `FullPlayer` tag 出现 `btn click` 与 `-> toggle*`，且 `updateProgress repeatMode/shuffleEnabled` 回写与按钮 `active` 一致
- [ ] 3.2 功能回归：横滑切面板、垂直下滑关闭（顶部可、歌词中部跟手、底部按钮区不误关）、进度条、歌词 seek、播放/切歌、队列/更多
- [ ] 3.3 布局回归：手机竖屏/窄屏、平板横屏的封面、进度、控制区布局无错位

### 4. 本地验证

- [ ] 4.1 编译：`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug`
- [ ] 4.2  lint：`./gradlew :app:lintMusesDebug`
- [ ] 4.3 单测：`./gradlew :feature:player:testDebugUnitTest`（如受影响）

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug
```

## 回滚点

- 若手势改动导致下滑关闭过度灵敏，回退 `PlayerScreen` 与 `FullPlayerWebView` 的触摸分流改动，保留 ViewModel 的 `toggle*` 闭包修复

## 产出

- 代码：`PlayerScreen.kt` / `FullPlayerWebView.kt` / `full-player.js(/css)` / `PlayerViewModel.kt`（按需）
- 产物：`app-muses-debug.apk` 可装机验证
