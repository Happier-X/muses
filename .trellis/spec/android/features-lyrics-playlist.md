# 特征·歌词 / 播放列表 / 响度均衡 — 开发规范（M2）

> 适用于 仓库根 Android 工程的 AMLL 歌词渲染、播放列表管理、响度均衡相关改动。Web 层对应契约见 `spec/frontend/features-player.md`，本文是其原生等价实现 + M2 新增事实。

---

## 范围 / 触发条件

- 改动 `feature/player/lyric/*`（AmllWebView、LyricsParser、AmllMapper）、`frontend/amll-web/`（AMLL 前端页面）
- 改动 `feature/playlist/*`、播放列表 Room 表
- 改动响度均衡链路（LoudnessCalculator/LoudnessController/replayGainTrackDb 列）

## 1. AMLL 渲染 = WebView 嵌入（DroidMate 1:1，08-31 起）

- **现行方案（WebView，08-31 起）**：`app/src/main/assets/amll/`（`vite lib es amll.bundle.js + cssInliner` 产自 `.workbuddy/tmp/amll-droidmate/frontend/src/main.tsx + styles.css + vite.config.js`）→ `AndroidView` 包 `WebView` → `WebViewAssetLoader` 以 `https://appassets.androidplatform.net/assets/amll/index.html` 加载 → `window.updateLyrics/updateTime/setPaused/updateAlbumArt/configure*` 注入。`PlayerViewModel` 经 `LyricsParser` + `AmllMapper` 解析为 `SyncedLyrics` → `LyricWebView` 透传；封面经 `FlowingLightBackdrop`（Compose 层）透底（WebView `LAYER_TYPE_HARDWARE + TRANSPARENT + mix-blend-mode:normal` 兼容 Chromium 110），WebView 内 `BackgroundRender(Mesh/Pixi)` 可选叠加但本任务以 Compose 层为主。
- **归档原生 Compose 方案（08-28，仅历史参考）**：曾用 `MeloXFlowingLightBackdrop + MeloXIOSLyricsPanel` 纯 Compose 复刻，因 LazyList 波浪 stagger 无法无闪复刻 Web 行弹簧，已归档。
- **禁止**在 WebView 栈外另起第二套歌词渲染；新需求在 `LyricWebView` / `amll.bundle.js` 上扩展。

### 就绪握手（0621054，P4.4 黑屏修复）

- **onPageFinished 触发早于 ES module 执行**：此时 `window.updatePlayerState/updateLyrics` 未定义，
  Kotlin 首轮 evaluateJavascript 注入静默丢失；无后续状态变化时播放页表现为纯底色黑屏。
- 契约：前端 module 尾部经 `nativeBridge.onAction('{"action":"ready"}')` 上报就绪；
  Kotlin 收到 ready 后全量重推当前 playerState 与歌词载荷（经 ref 取最新值，防闭包捕获过期）。

### 踩坑记录（92bf1a2，P4.3 歌词面板修复）

1. **androidAssets 目录必须显式注册为 assets 源**：`feature/player/build.gradle.kts` 加 `assets.srcDir("src/main/androidAssets")`——M1 起该目录从未注册，WebViewAssetLoader 找不到 index.html → ERR_INVALID_RESPONSE，且报错被 WebView 白屏吞掉难定位
2. **WebView 尺寸自适应用 ResizeObserver 不用 window.resize**：Android WebView 初始布局高度为 0，后续 AndroidView 获得真实尺寸不再派发 resize → 背景 canvas 高度 0 永不可见；amll-web 侧 `new ResizeObserver(resize).observe(document.body)` 兜底
3. **AndroidView 嵌入面板区域时 offset 位移要加在 AndroidView 自身而非父容器**（父容器位移会连带裁剪/命中区域错位）；背景歌词解耦 = 背景层与歌词层各自独立 AmllWebView 实例

### 桥接口签名（window 级，前端 `main.tsx` ↔ Kotlin `LyricWebView.kt`，1:1 DroidMate）

| JS 接口 | 入参 | 调用时机 |
|---|---|---|
| `updateLyrics({lines:[{words:[{word,startTime,endTime,romanWord?}], text, translatedLyric, romanLyric, startTime,endTime,isBG,isDuet}]})` | 对象（非字符串） | `isPageReady && lyrics !== lastLyrics` 引用比较 |
| `updateTime(number)` | `Math.trunc(ms)` | `now - lastUpdate >= frameIntervalMs(32ms)` 节流，暂停且已同步过则忽略 |
| `setPaused(bool)` | `!isPlaying` | `lastIsPlaying !== curIsPlaying` 去抖 |
| `updateAlbumArt(string)` | `file://` 先 `fetch->blob->dataURL`，http 加 `t=Date.now()` | `lastAlbumArtUri !== cur` 时 |
| `configureLyricMotion / configureBackgroundEffect / configureLyricBackground / applyFontSettings` | 各自 JSON | 签名比较，仅变化时刷 |

JS→Native（P4.4）：前端经 `window.nativeBridge.onAction(json)` 发动作 `{action:'playPause'|'next'|'previous'|'seekTo'(positionMs)|'setRepeatMode'(mode)|'setShuffle'(enabled)|'toggleTranslation'|'openQueue'|'close'}`；Kotlin 侧 `NativeBridge` 回调在 JS 线程，AmllWebView 内部统一 post 主线程后再分派。seek 为一次性语义：拖动 preview 由前端本地完成，抬起才发。

- **payload 注入必须经 `AmllMapper.quote()` 包成 JS 字符串字面量**——前端内部做 `JSON.parse`，直接内插对象会被 ToString 成 `[object Object]`。
- `songId` token：前端校验过期注入丢弃。

## 2. 背景生命周期治理（WebView + Compose 混合）

- 背景不得因「无歌词」卸载：`FlowingLightBackdrop`（Compose）始终渲染；WebView 透明透出；空态在前景 `LyricPanel` 显示占位，背景照常。
- **WebView 背景（可选）**：`BackgroundRender.new(MeshGradientRenderer|PixiRenderer)`，`tick` 每帧 `player.update(delta)`（不因 paused 跳过），`updateAlbumArt` 时 `setAlbum + update(0)`；若 WebGL 不可用则仅用 Compose 层，避免 `plus-lighter` 无底色全透明（已全局改为 `normal` + 兼容补丁）。
- 粘性封面三段语义（PlayerViewModel.stickyCover）：新曲有封面即更新；无封面**沿用旧值**；仅无当前曲才清空。
- `stickyCover == null` 时沿用旧帧（`FlowingLightBackdrop` 不清背景），避免切歌闪黑。

## 3. 封面加载

- **现行（混合）**：`stickyCover` 喂 Compose `FlowingLightBackdrop`（coil 天然支持 `file://`/`content://`/`data:`/`https:`）；WebView 侧 `updateAlbumArt` 对 `file://` 执行 `fetch->blob->dataURL` 规避 https 混合内容拦截，http 则加 `t=Date.now()` 防缓存。
- **归档**：曾用 `coverUriToAppAssetsUrl` 映射 `https://appassets.androidplatform.net/cache/...`，现由前端 `fetch` 直转 `dataURL` 替代。

## 4. 歌词解析（lyrics-core 0.4.7 API 事实）

```kotlin
AutoParser()                    // 无 Builder；可传 PhoneticProvider
parse(raw): SyncedLyrics?       // 自动识别 TTML/LRC/YRC/KRC/LSY
```

- **0.4.7 对不可识别文本不抛异常而是返回空行集** → `LyricsParser.parse` 已归一化：失败或空行集一律返回 null。
- 0.4.7 **无 Android target**（JVM/iOS/JS/wasm），以 JVM 变体参与构建；其 TTML 解析为自实现（无 javax.xml 依赖），Android 可用。
- 行模型两态：`KaraokeLine`（syllables 逐词 + translation + phonetic；`KaraokeLine.AccompanimentKaraokeLine` 为背景行）与 `SyncedLine(content, translation, start, end)`（LRC 整行，**EnhancedLrcParser 已自动做同时间戳双语配对**）。
- 升级版本时重点核对：AutoParser 构造方式、SyncedLyrics.lines 元素类型、KaraokeLine.getTranslation/getPhonetic。

### AmllMapper 输出契约

- 目标结构字段名必须与 AMLL core 0.5.2 一致：`words[{startTime,endTime,word}] / startTime / endTime / translatedLyric / romanLyric / isBG / isDuet`（ms 单位）。
- 手写 JSON 序列化（不引 kotlinx-serialization）；转义用 `quote()`。
- 播完钳制在 Kotlin 侧：发送 `min(positionMs, lastLine.endTime)`，规避「播完全行失活模糊」。
- 解析失败降级：空行数组 payload，**不是不发**。

## 5. 播放列表（Room v2+）

```
playlists(id PK, name, createdAt, updatedAt)
playlist_songs(playlistId FK→playlists CASCADE, songId FK→songs CASCADE, position)
               PK(playlistId, position) + INDEX(songId)
```

- 排序 = position 连续 0..n-1；删除歌曲后 `removeSongAndCompact` 紧凑重排；reorder 用**两阶段平移**（+100_000 再写回）避复合 PK 冲突，单事务。
- 整体入队：`PlaylistRepository.getSongs(id)` → `PlayerConnection.play(first.id, songs)`；空列表早退。
- 迁移只向前追加，不改既有表；新增列用 `ALTER TABLE ... ADD COLUMN ... DEFAULT NULL`。

## 6. 响度均衡（服务侧应用）

- **音量必须设在服务侧 ExoPlayer 上**（`PlaybackService` 的 player）；MediaController 无 volume 能力。`PlaybackService` 已 `@AndroidEntryPoint`，onCreate 组装 `LoudnessController(player, settingsRepository, songDao, serviceScope)`，onDestroy **先 stop controller/serviceScope 再 release player**。
- 计算语义（照搬 Web 层 loudness.ts）：`volume = clamp(10^((db+6)/20), 0.1, 1.0)`；关闭或 gain=null → 1.0；超 ±30dB 先走 Q7.8 ÷256 兜底换算，仍越界才丢弃。
- 切歌必重算（onMediaItemTransition），禁止串曲增益；开关变化即时对当前曲重设。写入 volume 必须**单飞**（取消上一个在途 applyJob 再启动，防快速切歌乱序覆盖）。
- 默认关（DataStore `loudness_enabled`）；设置页 UI 入口留 M3。
- RG 数据链路：TagReader 别名扫描/TXXX → normalize（÷256 + 校验）→ SongEntity.replayGainTrackDb → LoudnessController 按 mediaId 反查。

## 7. 沉浸式播放页（单一 WebView 整页，08-31 起 DroidMate 1:1）

### 7.1 职责划分

| 组件 | 文件 | 职责 |
|---|---|---|
| `PlayerScreen` | `feature/player/PlayerScreen.kt` | 容器：`drag-layer`（`offset {IntOffset(0,dragOffsetY)}` + `isLyricAtTop` 手势分流）+ `FlowingLightBackdrop` 透底 + 单一 `FullPlayerWebView`；`isLyricPanelActive && !isLyricAtTop` 时禁下滑（歌词未在顶部时让位跟手），否则 `pointerInput detectVerticalDragGestures` 跟手下滑≥`clamp(0.18*h,96,160)` 关闭 / 0.22s `CubicBezier(0,0,0.58,1)` 回弹；底部约 `180dp` 控制区排除下滑判定（`bottomExclusionPx` + `ignoreDrag`），避免与 WebView 底部按钮点击冲突；`onToggleRepeat/onToggleShuffle` 直调 `viewModel.toggleRepeat/toggleShuffle` 无参版以避闭包陈旧值 |
| `FullPlayerWebView` | `feature/player/lyric/FullPlayerWebView.kt` | 单一 `WebView`：`WebViewAssetLoader https://appassets.androidplatform.net/assets/amll/` + `LAYER_TYPE_HARDWARE + TRANSPARENT` + `isPageReady` 闸门 + `32ms` 轮询 `updateProgress/updateTime/setPaused` + `file://→dataURL` 封面 + `onAction/onLineClick/onPanelChange/onLyricScroll`；`OnTouchListener` 在 `ACTION_DOWN` 默认 `requestDisallowInterceptTouchEvent(true)` 保活点击（纯点击不再被外层下滑手势拦截），`MOVE` 时按 `dy/dx` 分流（`dy>dx` 信息页放行/歌词页顶部放行其余 WebView，`dx>dy` 横滑）+ `isUserScrolling` 时 `configureLyricMotion(enableBlur:false)`；`onToggleRepeat/onToggleShuffle` 经 `rememberUpdatedState` 取最新闭包 |
| `full-player.{js,css}` | `app/src/main/assets/amll/full-player.{js,css}` | 前端整页：`panels 200%→translateX(-activePanel*50%) 0.22s` / `alignPosition 0.5` 居中高亮行 / `SVG` 图标（`play/prev/next` + `repeat/svgRepeatOne` + `shuffle/svgOrder(FormatListBulleted 顺序)` + `queue/more`）经 `setRepeatIcon(mode)/setShuffleIcon(enabled)` 统一替换 `innerHTML` 与 `active`（循环 `ALL:Repeat ↔ ONE:RepeatOne`，随机 `true:Shuffle ↔ false:FormatListBulleted`），乐观 `playPause/toggleRepeat/toggleShuffle` 瞬切与 32ms `updateProgress({repeatMode,shuffleEnabled})` 真值回写均经同一 setter 幂等；`meta-window 79px` 小窗 + 平板 `--tablet` 双栏；`panels` 横滑对 `mode-bar/controls/progress-range/bottom-bar` 非滑动区 `isInNoSwipeZone` 直接跳过（`closest` 判定），按钮 `touchstart/move stopPropagation` 保留点击 |
| `LyricWebView` | `feature/player/lyric/LyricWebView.kt` | 兼容旧入口：同 `FullPlayerWebView` 契约的独立歌词 `WebView`（现仅 `LyricsPanel` 内部或单测使用），保持 `WebViewAssetLoader + isPageReady + 32ms` 去重 |
| `LyricPanel` | `lyric/LyricsPanel.kt` | 兼容壳：空态占位/`SaltIconButton` 翻译/播放 FAB 透传，歌词渲染已迁至 `FullPlayerWebView` |
| `PlayerViewModel` | `PlayerViewModel.kt` | 粘性封面 `stickyCover` + `parsedLines`（`LyricsParser→AmllMapper`）+ `lyricPosition`（100ms 钳制 `min(pos,lastLineEnd)`）+ `translationEnabled/hasTranslation` |

### 7.2 布局契约（复刻 Capacitor `PlayerPage.vue` BEM，08-31 单一 WebView 增量）

- `.player-page__drag-layer`：`offset {IntOffset(0,dragOffsetY)}`（非 `graphicsLayer`，确保下滑暴露区重绘底表）+ `isLyricPanelActive && !isLyricAtTop ? Modifier : pointerInput` 分流；阈值 `clamp(0.18*h,96,160)`，回弹 `0.22s CubicBezier(0,0,0.58,1)`；底部 `180dp` 排除区（`bottomExclusionPx`）内 `ignoreDrag=true` 不参与下滑判定，保证底部按钮点击直达 WebView。
- `.player-page__panels/full-player.js panels`：`width 200% + transform translateX(-activePanel*50%) 0.22s`（手机），平板 `width 100% transform:none + gap 24 + Row weight 1f` 双栏 + 底部 `TabletBottomBar`；`info-panel/lyric-panel` 各 `width 50% height 100% overflow hidden`；`full-player.js` 对 `mode-bar/controls/progress-range/bottom-bar` 命中 `isInNoSwipeZone` 时 `touchstart/move/end` 直接跳过横滑逻辑，按钮 `click` 仍经 `bindClick` → `Android.onAction toggleRepeat/toggleShuffle`；图标经 `setRepeatIcon/setShuffleIcon` 在 `bindClick` 乐观与 `updateProgress` 回写两处同步切换（`Repeat ↔ RepeatOne`、`Shuffle ↔ svgOrder`）。
- `.player-page__cover-hero`：`aspectRatio(1) max-height min(50vh,420px) / clamp(160px,62vw,300px) max-height min(38dvh,300px)`，圆角 `12dp`，信息页 `info-panel` 弹性居中。
- `info-panel/meta-window/progress/controls/mode-bar`：`meta-window 79px`（窄屏 `19.5px` 单行）`translateY -29.5` 居中 `scale 1.05/0.92 opacity 1/0.55`；`progress-range` 白填充+`0.25` 底轨，`time-row 12px tabular 0.68`；`controls 48/56 mode-bar 40 max320`，`SVG` `currentColor` + `active` 白。
- `lyric-panel`：`#lyric-player-container 100% relative` 承载 `amll-lyric-player`（`position absolute inset 0` 迁入），`alignPosition 0.5` 高亮行始终居中，行字号 `clamp(22px,6.5vw,32px)`，手势由 `isUserScrolling + onLyricScroll(cur<=1)` 协同。

### 7.3 数据流

```
SongEntity(lyrics, coverUri) --observeById--> PlayerViewModel.refreshLyricsWithEntity()
  ├─ stickyCover (三段粘性)
  ├─ LyricsParser.parse(raw) -> SyncedLyrics? (Dispatchers.Default)
  ├─ AmllMapper.toAmllLines(synced) -> List<AmllLyricLine>
  ├─ _hasTranslation / _parsedLines (translationEnabled 过滤)
  └─ lastLineEndMs (钳制)
position (500ms) / lyricPosition (100ms, coerceAtMost lastLineEnd) -> PlayerScreen
```

- 歌词空时发空数组（非 null），`MeloXIOSLyricsPanel` / `MetaWindow` 显空态，背景不卸载。
- 逐词染色：非当前行 `0.35`，当前行已唱 `1.0` / 未唱 `0.42` / 正在唱 `1.0 ExtraBold`（对齐 AMLL 非活动行统一暗淡）。

## 测试要点

- LyricsParserTest：TTML 样本解析、双语 LRC 配对、非法输入（含乱文本返回 null 不抛）
- AmllMapperTest：逐词映射窗口一致性、translation 直挂、JSON 字段名与转义、coverUrl=null 字面量
- MeloXIOSLyricsPanelTest（新增）：逐词 alpha/weight、和声 italic、翻译显隐、点击 seek、空态
- MeloXFlowingLightBackdropTest（新增）：无封面 fallback 纵向渐变、有封面 blur+scrim、flowSpeed 周期
- PlaylistDaoTest / PlaylistRepositoryTest：CRUD、去重追加、紧凑重排、双 CASCADE
- LoudnessCalculatorTest：Q7.8 换算、边界兜底、clamp 双端、开关/无标签恒 1.0

## 错误行为矩阵

| 场景 | 正确行为 |
|---|---|
| 歌词解析失败/空 | null → 空 payload，背景照常渲染 |
| payload JSON 含引号/换行 | quote() 转义后嵌入 JS 字符串字面量 |
| 封面为 file:// 非 cacheDir | 映射返回 null → 前端粘性沿用 |
| RG 标签非法（换算后仍超 ±30） | 丢弃不入库，播放按无标签处理 |
| 快速连点切歌 | 单飞 applyJob 取消旧查询，最终一致为新曲增益 |

15. **MuMu 的 screencap 截不到 WebView 硬件合成层**（截图纯黑但实际屏幕正常）——排查 WebView 页面"黑屏"必须用 uiautomator dump --compressed 读 accessibility 树或 CDP，勿信截图

16. **上一曲/下一曲改为纯队列首尾循环（09-01 沉浸式排障）**：`PlayerConnection.skipToPrevious/Next` 改为无视 `position/repeat/hasPrevious` 的环形索引 `(idx-1+count)%count / (idx+1)%count`（`count<=1` 回零），顺序即原始队列顺序、随机即洗牌后时间线顺序，均与进度分钟数无关；`count>1` 时队首上一曲必到队尾、队尾下一曲必到队首。
