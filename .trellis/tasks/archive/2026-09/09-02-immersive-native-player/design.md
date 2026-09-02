# 设计 — 沉浸式播放页原生重构（替换WebView）

## 1. 目标与约束
- 彻底下线 `WebView` 主路径（`FullPlayerWebView`/`LyricWebView` + `app/src/main/assets/amll/*` + `WebViewAssetLoader` + `evaluateJavascript 32ms` 轮询），回归纯 Compose。
- 布局 1:1 还原既有 WebView 版 BEM（但允许 Blob/字号等微调），数据契约完全沿用 `PlayerViewModel`。
- 歌词渲染**自研手搓**，不使用 `feature/player/src/main/kotlin/com/mocharealm/accompanist/lyrics/ui/**`（已 vendored 的 `lyrics-ui`），满足“不是官方、效果不符”诉求。

## 2. 架构与边界
```
app (Nav)
 └── feature:player
      ├── PlayerScreen.kt（容器：背景 + 头部 + Pager/双栏 + 手势 + VM 订阅）
      ├── lyric/
      │   ├── NativeLyricsPanel.kt（新：自研卡拉OK面板，LazyColumn）
      │   ├── NativeKaraokeLine.kt（新：单行逐词/逐字 Canvas 渲染）
      │   ├── LyricProgressor.kt（新：positionProvider 帧外推 + currentIndex 计算）
      │   ├── LyricsParser.kt（保留）
      │   ├── AmllMapper.kt（保留，兼容旧 JSON 但主路径不发）
      │   └── FlowingLightBackdrop.kt（复用）
      └── PlayerViewModel.kt（不变）
```
- `feature:player` 禁止直接依赖 Room/OkHttp，仍通过 `core:*` 接口。
- 不新增外部依赖；`androidx.webkit` 依赖保留但沉浸主路径不再调用（后续可清理）。
- Vendored `com.mocharealm.accompanist.lyrics.ui` 源码保留在仓（不删，避免误触第三方许可），但沉浸主路径**零引用**；后续可单独任务清理。

## 3. 数据流
```
SongEntity(lyrics, coverUri) --observeById--> PlayerViewModel
  ├─ stickyCover ──► FlowingLightBackdrop/CoverHero(AsyncImage)
  ├─ syncedLyrics ──► NativeLyricsPanel(syncedLyrics, positionProvider, translationEnabled, isPlaying)
  ├─ hasTranslation/translationEnabled ──► FAB 显隐
  ├─ position/duration/isPlaying/repeat/shuffle ──► ProgressSection/Controls
  └─ lyricPosition(100ms 钳制) ──► LyricClock 锚点 ──► 帧外推 ──► Karaoke 行染色（仅触发绘制，不触发重组）
```
- `rememberLyricPositionProvider(positionFlow, isPlaying)` 保留：`LyricClock.anchorPositionMs` 普通字段 + `withFrameMillis` 每帧 `animatedPosition = anchor + (frameTime - anchorTime)`；`isPlaying==false` 时冻结。
- `duration` 仍为 `combine(playerDuration, dbDuration)` 兜底，避免冷启动 0。
- Seek：`ProgressSection` 的 `onSeekStart/onSeekEnd` 暂停 `isSeeking`，VM 内同步 `_lyricPosition` 钳制到 `lastLineEndMs`。

## 4. 布局重构
### 4.1 容器 `PlayerScreen`
- 现状 `Box(offset drag) + FullPlayerWebView` 改为：
  ```
  Box(fillMaxSize) {
    FlowingLightBackdrop(stickyCover, hasLyric) // 最底层
    Box(offset drag + pointerInput) { // drag-layer
      Column {
        FixedSongHead(title, artist) // 手机显示，平板隐藏（或 isTablet 时不渲染）
        if (isTablet) TabletImmersiveLayout else PhoneImmersiveLayout
      }
    }
    Snackbar(error)
  }
  ```
- `isTabletLayout = screenWidthDp>=768 && screenHeightDp < screenWidthDp`（与 Web 版一致），`isNarrowHeight = screenHeightDp<=520`。
- drag-layer：`Modifier.offset { IntOffset(0, dragOffsetY.roundToInt()) }`（非 graphicsLayer，确保暴露区重绘），`pointerInput` 仅当 `!isLyricPanelActive || isLyricAtTop` 时启用；阈值 `dismissThresholdPx = (h*0.18).coerceIn(96dp,160dp)`，`bottomExclusion 180dp`；回弹 `Animatable + tween(220, CubicBezier(0,0,0.58,1))` 显式 `startRebound`，`clearDragImmediate` 用于 `onClose`。
- 顶部 `statusBarsPadding` + `navigationBarsPadding` 保留。

### 4.2 手机 `PhoneImmersiveLayout`
- `Column`：`FixedSongHead` + `HorizontalPager(pageCount=2, beyondViewport=0)`：
  - page0 `InfoPanel`：`CoverHero(weight=1f) + ProgressSection + ControlsRow + ModeBarRow`
  - page1 `NativeLyricsPanel`
- 切换：`LaunchedEffect(activePanel) → pagerState.animateScrollToPage(activePanel, tween 220 easeOut)`；反之 `pagerState.isScrollInProgress==false` 时回写 `activePanel` 并同步 `isLyricPanelActive` 给外层手势分流。
- 禁止手机额外小圆点指示器（对齐原版 1:1）。

### 4.3 平板 `TabletImmersiveLayout`
- `Column { Row(weight=1f) { Box(weight=1f, center){ CoverHero } ; Box(weight=1f){ NativeLyricsPanel(isTablet=true) } } ; TabletBottomBar }`
- 左栏不含进度/控制（由 `TabletBottomBar` 承担），右栏 `NativeLyricsPanel` 隐藏 header 翻译 FAB 的播放键（`showPlayFab=false`）。

### 4.4 `FlowingLightBackdrop` / `CoverHero` / `FixedSongHead` / `ProgressSection` / `ControlsRow` / `ModeBarRow` / `TabletBottomBar`
- 全部复用现有实现，仅做参数对齐：
  - `FlowingLightBackdrop`：`clipToBounds + fallback 纵向渐变 + cover blur28/scale1.08/alpha0.75 + Canvas 3 Blob + 暗色 scrim + 顶部高光`，`flowSpeed=2` → `duration 6000ms`。
  - `CoverHero`：`BoxWithConstraints + size(targetSize) + aspectRatio1 + RoundedCorner12 + AsyncImage(Crop)`，`maxHeroHeight min(50vh,420dp)`，窄高时 `34vw/150dp`。
  - `ProgressSection`：自绘 `drawBehind` 双圆角轨（4dp），`pointerInput detectTap+detectDrag`，`previewMs` 本地态，`time-row 12sp tabular 0.68 + 缓冲中 11sp 0.55`。
  - `ControlsRow`/`ModeBarRow`/`TabletBottomBar`：`SaltIconButton` 尺寸与 gap 断点保留。

## 5. 自研歌词渲染（核心）
### 5.1 数据模型
- 输入 `SyncedLyrics?`（`lines: List<ISyncedLine>`），分支：
  - `KaraokeLine`：`syllables: List<Syllable(content, start, end)> + translation/phonetic/alignment/isBG`
  - `SyncedLine`：`content/translation/start/end` 单词整行。
- 统一映射为内部 `LyricWord(start,end,text)` 与 `LyricLine(words, start,end, translation, roman, isBG) `（复用 `AmllMapper` 逻辑或新 `LyricUiMapper`，但不发 JS JSON）。

### 5.2 状态与计算
- `currentIndex = computeCurrentIndex(lines, positionMs)` 线性扫描（O(n) n<~200，可接受），`positionMs` 来自 `positionProvider()` 每帧。
- 距离衰减：`distance = abs(index - currentIndex)`；`alpha = when(distance){0->1f;1->0.45f;2->0.28f else->0.18f}`；`scale = if(distance==0)1.05f else 0.92f`；`blur = if(distance>=2) 6.dp else 0.dp`（仅远行模糊以省性能）。
- 逐词进度：对 `KaraokeLine` 的每个 `word`，`frac = (position - word.start)/(word.end-word.start) coerce 0..1`；对 `SyncedLine` 单词 `frac = (position - line.start)/(line.end-line.start)`。
- 长词字符级：若 `word.text.length>6`，将 `frac` 映射到字符：`chars = text.length; filledChars = (frac*chars).toInt()`，剩余字符做 alpha 渐变（首选线性，无需复杂 bounce，先达成连续感，后续可加 `CubicBezier` 缓动）。

### 5.3 渲染
- 行容器：`LyricsLineItem`（`Modifier.graphicsLayer(scaleX/scale, scaleY/scale) + blur + alpha + clickable{onSeek(line.start)}`），点击即 `viewModel.seekTo` + `onSeek`.
- 文本渲染：`KaraokeLineText` 采用 `Canvas` 双层绘制：
  1. 底层：`drawText(inactiveColor)` 全量；
  2. 上层：`with(saveLayer) { clipRect(right = width*overallProgressOrWordProgress) ; drawText(activeColor) }`，`BlendMode SrcOver` 即可实现连续扫过；发光效果对当前行叠 `BlendMode.Plus` 或 `shadow`。
  - 简化首版：按**行级 overallProgress**（`line` 进度）做整体填充 + 按词做透明度加权，已能实现“逐词连续”观感；后续迭代再按词独立 `clipRect`。
- 翻译/音译：行下方第二行 `Text(translation, 13sp, White 0.55, maxLines=1)`，`translationEnabled==false` 时不渲染。
- 和声/伴唱：`isBG==true` → `italic + alpha 0.6 + 偏右对齐`。
- 间奏：`KaraokeBreathingDots`（三点呼吸动画 `infiniteTransition`）当 `gap>4000ms` 时插入。
- 字体：`mainFontSize = if(isTablet) (w*0.024).coerceIn(20,30) else (w*0.075).coerceIn(26,32) sp`（与 Web 版 `clamp(22,6.5vw,32)` 对齐）。

### 5.4 滚动
- `LazyColumn(state=rememberLazyListState, contentPadding=PaddingValues(vertical=120.dp))`，`LaunchedEffect(currentIndex)` 中 `listState.animateScrollToItem(currentIndex, offset = -viewport/2)` 使当前行居中（`alignPosition 0.5` 等价）。
- 用户手势滚动时不自动抢回：`isUserScrolling` 标记（`LazyListState.isScrollInProgress`），`delay(3000)` 后恢复自动居中；滚动期间仍更新 `positionProvider` 的染色进度（不暂停染色，仅暂停自动滚动）。
- 下滑关闭分流：`NativeLyricsPanel` 通过 `listState.firstVisibleItemIndex==0 && firstVisibleItemScrollOffset<=1` 判定 `isAtTop`，回调给 `PlayerScreen` 的 `isLyricAtTop`，用于外层 `pointerInput` 的启用判断。

### 5.5 性能
- `positionProvider` 的 `mutableLongState` 仅在 `Canvas DrawScope` 读取，避免触发重组；行级 `alpha/scale` 仅 `currentIndex` 变化时重组。
- `Blur` 仅远行应用，且 `enableBlur` 可由 `isUserScrolling` 动态关闭（与 Web 版 `configureLyricMotion` 对齐，但首版可常开）。
- 无 `evaluateJavascript`，无 `file→dataURL` 转码，无 `WebViewAssetLoader`。

### 5.6 FAB
- 浮动区 `Row(BottomCenter)` 含翻译键（`hasTranslation` 时）与播放/暂停（`!isTablet` 时），`chromeVisible` 默认 `false`，点击/滚动 `revealChrome()` → `delay 3000` 隐藏，`animateFloatAsState` 淡入淡出。

## 6. 资源清理
- 删除 `feature/player/lyric/FullPlayerWebView.kt` 与 `LyricWebView.kt` 的**主路径引用**，删除 `app/src/main/assets/amll/` 下 `amll.bundle.js`, `full-player.js/css`, `index.html`；`PlayerScreen` 不再导入 `AndroidView`/`WebView`。
- `feature/player/build.gradle.kts` 的 `androidx.webkit` 依赖保留（或后续任务清理，避免本次 diff 过大）。
- Git 历史保留，回滚 `git revert` 即可。

## 7. 兼容与回归
- `PlayerViewModel` 零改动，`PlayerConnection`/`PlaybackService`/`core:data`/`core:lyrics` 不动。
- 导航：`MusesApp`/`NavDestination` 的 `PlayerScreen(onClose)` 回调不变。
- Spec 更新：`spec/android/index.md` 与 `spec/android/features-lyrics-playlist.md` 的“沉浸式为单一 WebView 整页”段落重写为原生方案描述。

## 8. 取舍
- **自研 vs Vendored**：自研成本更高（需自实现测量/裁剪/Blur/滚动），但满足用户“效果不一样”诉求，且摆脱 `lyrics-ui` 的弹簧/波浪实现与包体积；首版先实现行级连续+词级透明，字符级 bounce 可迭代。
- **Canvas 双层 vs AnnotatedString**：Canvas 裁剪最接近 AMLL 的位图遮罩扫过，`AnnotatedString` 仅能二段变色，放弃。
- **彻底删除 vs 保留预览**：彻底删除最干净，符合“不用 WebView”决心；保留预览会延续 WebView 依赖与体积。

## 9. 风险与对策
- 逐词测量在 Compose `Text` 下需 `TextLayoutResult`，长行换行测量复杂 → 首版固定单行省略或 `maxLines=2`，避免换行；后续再支持换行测量。
- `blur` 在低端机 `RenderEffect` 可能掉帧 → 远行才模糊，近行无模糊；提供开关。
- 自动滚动与用户手势冲突 → `isUserScrolling` 3s 防抖，与 Web 版一致。
