# 沉浸式播放页 AMLL WebView 1:1 复刻 DroidMate — Implement

## 执行顺序

### 1) 前端产线对齐（产出 amll.bundle.js）

- [ ] 将 `.workbuddy/tmp/amll-droidmate/frontend/{src/main.tsx,styles.css,vite.config.js,index.html}` 复制为本仓库 `app/src/main/assets/amll/` 的构建源（或在 `app/src/main/assets/amll-frontend/` 新建 vite 工程），安装 `@applemusic-like-lyrics/core + @applemusic-like-lyrics/lyric + @pixi/core/@pixi/utils + vite-plugin-wasm/top-level-await`
- [ ] 按 `vite.config.js` 构建：`lib es amll.bundle.js + cssInliner`，验证产物 `amll.bundle.js` 首行含 `style.innerText` 内联 CSS、`app/src/main/assets/amll/index.html` 含 `import('./amll.bundle.js')` 且 `base ./`
- [ ] 旧 `amll.bundle.js` 备份为 `amll.bundle.js.bak`，产出后用 `adb shell dumpsys` 确认装包生效

### 2) Kotlin 侧 WebView 重构

- [ ] 重写 `feature/player/src/main/kotlin/com/muses/player/feature/player/lyric/LyricWebView.kt` 为 `AMLLLyricsView` 1:1 结构：
  - [ ] `WebViewAssetLoader` 加 `/fonts/` handler 可选、`allowFileAccess false`、`LayerType HARDWARE`、`setBackgroundColor TRANSPARENT`、`clearAllCache`
  - [ ] `rememberUpdatedState` 包 `onLineSeek/onLyricsClick/isPlaying`
  - [ ] `isPageReady` + `onPageReady` 回调驱动 `LaunchedEffect(lyrics,isPageReady)` 重推；`onPageStarted` 重置 `lastLyrics`
  - [ ] `update` 块：`isPageReady` 闸门 + 三项去重（`lastLyrics` 引用、`lastIsPlaying`、`frameIntervalMs` 节流 `updateTime`）+ `buildLyricsJson` 调用
  - [ ] 乐观 seek：`onSeekRequested -> post { evaluate(setIsSeeking true); evaluate(updateTime(seekTime)) }`
  - [ ] `onRelease { stopLoading/clearHistory/clearCache/removeJavascriptInterface/destroy }`
- [ ] 对齐 `AmllMapper`：补 `text`、`romanWord` 逐词映射可选、`cleanBackgroundText`、`endTime>startTime` 防御
- [ ] `LyricsPanel.kt` 瘦身：仅 FAB 3s idle 隐藏 + 透传 `onSeek/onToggleTranslation/isPlaying/showPlayFab`，移除自绘回退
- [ ] `PlayerScreen.kt` 检查：`FlowingLightBackdrop` 与 WebView 背景二选一（WebView 背景 `BackgroundRender` 存在时 Compose 层仅 Scrim，或保留 Compose 流体二者不叠加）；`drag-layer offset` 不套在 WebView 父容器外

### 3) ViewModel 透传

- [ ] `PlayerViewModel` 保持 `stickyCover` 粘性与 `lyricsJson` 载荷不变，确认 `lastLineEndMs` 钳制语义与 Web 侧去抖不冲突；翻译开关仅过滤 `translatedLyric/romanLyric` 不重解析

### 4) 验证

- [ ] 本地验证：`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug` 通过；`adb install -r app/build/outputs/apk/muses/debug/*.apk` 后 `dumpsys package com.muses.player | grep versionCode` 回读确认
- [ ] 真机验证：含逐词 TTML 曲目（YRC/QRC/KRC）三类 + 纯 LRC各一，检查：可见性、逐词发光、行弹簧波浪、点击跳转、暂停/恢复、翻译开关、封面 file://、平板横竖
- [ ] 回归：切歌快速连点、旋转、切后台、熄屏恢复、空歌词空态

### 5) Spec 与收尾

- [ ] 更新 `.trellis/spec/android/features-lyrics-playlist.md`：删除“禁止 WebView”条款，新增 DroidMate 1:1 契约与桥接口表
- [ ] 更新 `.trellis/spec/android/index.md` 播放契约（如有）
- [ ] `implement.jsonl/check.jsonl` 已策展（见任务目录），`task.py validate` 通过后 `task.py start --allow-empty-context` 或正常 start

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest
adb install -r app/build/outputs/apk/muses/debug/app-muses-debug.apk
adb shell dumpsys package com.muses.player | grep -E "versionCode|lastUpdateTime"
adb logcat -s LyricWeb:*,AMLLLyrics:*,WebView:*
```

## 回滚点

- 前端产线失败：恢复 `amll.bundle.js.bak`
- Kotlin 侧黑屏回退：`LyricWebView` 特性开关切旧分支，保留一版可编译
- 验证不通过：`git checkout HEAD -- app/src/main/assets/amll/ feature/player/src/main/kotlin/com/muses/player/feature/player/lyric/`
