# 实施计划 — 沉浸式播放页原生重构（替换WebView）

## 0. 前置
- 分支：`main`（当前 clean）
- 验证基线：`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest`

## 1. 有序清单
### Step 1 — 容器重构（PlayerScreen）
- [ ] 将 `PlayerScreen` 的 `FullPlayerWebView` 整段替换为 `FlowingLightBackdrop + Column(头部+Pager/双栏)` 原生结构（复用 `PhoneImmersiveLayout`/`TabletImmersiveLayout` 现有代码，修正其 `activePanel` 与外层 `isLyricPanelActive/isLyricAtTop` 联动）
- [ ] 保留 `dragOffsetY/isDraggingVertically/dismissThreshold/bottomExclusion/startRebound/clearDragImmediate` 闭环逻辑，确保 `offset` 非 `graphicsLayer`
- [ ] 接线 `PlayerViewModel` 所有 StateFlow（`isPlaying/position/duration/.../isBuffering/playbackError`）与回调（`onSeek/onPlayPause/.../toggleTranslation`），验证 `stickyCover` 与冷启动 `duration` 兜底仍生效
- 风险文件：`feature/player/PlayerScreen.kt`
- 验证：`assembleMusesDebug` 编译通过，MuMu 上标题/封面/背景可见，无 WebView 日志

### Step 2 — 自研歌词面板（核心）
- [ ] 新建 `feature/player/lyric/NativeLyricsPanel.kt`：`LazyColumn + currentIndex + animateScrollToItem 居中 + isAtTop 回调 + FAB(翻译/播放) 3s 显隐`
- [ ] 新建 `feature/player/lyric/NativeKaraokeLine.kt`：行容器（scale/blur/alpha）+ `Canvas` 双层文本（`drawText` + `clipRect(progress)` + `BlendMode.Plus` 发光）+ 翻译/和声/间奏
- [ ] 新建 `feature/player/lyric/LyricProgressor.kt` 或内联 `rememberLyricPositionProvider`：`LyricClock` 锚点 + `withFrameMillis` 外推，`positionProvider: ()->Int` 仅 DrawScope 读取
- [ ] 保留 `LyricsPanel.kt` 的空态文案与对外签名，但内部不再包 `LyricWebView`，改为包 `NativeLyricsPanel`
- 风险文件：`feature/player/lyric/*`、`feature/player/PlayerScreen.kt` 的 `isLyricAtTop` 联动
- 验证：歌词逐词连续填充、当前行高亮、翻译开关、点击 seek、无词空态均正常；滚动与下滑关闭不冲突

### Step 3 — 进度/控制/平板条收口
- [ ] 确认 `ProgressSection` 的 `drawBehind` 双轨 + `detectTap/detectDrag` + `previewMs` + `isBuffering` 完整可用，平板/手机两处复用一致
- [ ] 确认 `ControlsRow`/`ModeBarRow`/`TabletBottomBar` 图标（`RepeatOne/Shuffle/List`）随 `repeatMode/shuffleEnabled` 正确切换（经真机日志验证）
- [ ] 窄高断点 `<=720/520` 的 gap/尺寸收紧回归（MuMu 旋转/分屏验证）
- 风险文件：`feature/player/PlayerScreen.kt` 内子组件
- 验证：seek/播放/上一首/下一曲/循环/随机/队列/更多全链路可用

### Step 4 — 资源清理与 Spec 同步
- [ ] 删除或移除引用：`feature/player/lyric/FullPlayerWebView.kt`, `LyricWebView.kt`（主路径零引用后可直接删除，Git 可回溯）
- [ ] 删除 `app/src/main/assets/amll/amll.bundle.js`, `full-player.js/css`, `index.html`（若存在 `feature/player/src/main/androidAssets` 需同步清理）
- [ ] 更新 `spec/android/index.md` 沉浸式段落：单一 WebView 整页 → 原生 Compose 自研卡拉OK（`NativeLyricsPanel` + `FlowingLightBackdrop` + `HorizontalPager`）
- [ ] 更新 `spec/android/features-lyrics-playlist.md` §1/§7：移除 WebView 契约，补充自研渲染契约（可选，M2 文档增量）
- 风险文件：`app/src/main/assets/**`, `spec/**`
- 验证：`grep -R "FullPlayerWebView\|LyricWebView\|appassets.androidplatform.net" --include="*.kt"` 零命中（主路径）

### Step 5 — 校验与交付
- [ ] 执行 `JAVA_HOME="..." ./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest` 全绿
- [ ] 安装至 MuMu：`adb install -r app/build/outputs/apk/muses/debug/app-muses-debug.apk`，用户手动验收 AC1-AC7
- [ ] 记录回滚点：本次提交前 `git stash` 或 `git commit -m "chore: backup before immersive native"`，回滚 `git revert`

## 2. 验证命令
```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest
# 快速编译
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug
# MuMu 安装（端口按实际 16384/7555）
adb connect 127.0.0.1:16384 && adb install -r app/build/outputs/apk/muses/debug/app-muses-debug.apk
```

## 3. 回滚预案
- 若自研歌词效果不及预期或手势回归失败：`git revert <commit>` 恢复 `FullPlayerWebView` 路径；资产在 Git 历史中可 `git checkout HEAD~1 -- app/src/main/assets/amll feature/player/lyric/FullPlayerWebView.kt`
- 若仅背景/布局回归：可保留歌词自研，临时回退 `PlayerScreen` 的 Pager 容器为旧 `Box`。

## 4. 人工复核门
- [ ] `task.py start` 前已完成 PRD 收敛（Open Questions=0）+ design/implement 已评审
- [ ] 自研歌词的首版“行级连续+词级透明”效果已与用户在 MuMu 上对齐，字符级动效可迭代
