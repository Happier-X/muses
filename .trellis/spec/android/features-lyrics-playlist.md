# 特征·歌词 / 播放列表 / 响度均衡 — 开发规范（M2）

> 适用于 仓库根 Android 工程的 AMLL 歌词渲染、播放列表管理、响度均衡相关改动。Web 层对应契约见 `spec/frontend/features-player.md`，本文是其原生等价实现 + M2 新增事实。

---

## 范围 / 触发条件

- 改动 `feature/player/lyric/*`（AmllWebView、LyricsParser、AmllMapper）、`frontend/amll-web/`（AMLL 前端页面）
- 改动 `feature/playlist/*`、播放列表 Room 表
- 改动响度均衡链路（LoudnessCalculator/LoudnessController/replayGainTrackDb 列）

## 1. AMLL 渲染 = 原生自研（09-02 起，手搓，WebView 已下线）

- **现行方案（原生，09-02 起）**：`feature/player/lyric/NativeLyricsPanel`（`LazyColumn` 居中滚动 + `isAtTop` 回调）+ `NativeKaraokeLine`（`AnnotatedString` 逐词/逐字符 `lerp` 连续渐变 + 距离 `Blur/Scale/Alpha` + `FontStyle.Italic` 和声）→ `PlayerScreen` 透过 `FlowingLightBackdrop` 透底；`PlayerViewModel` 经 `LyricsParser` 解析为 `SyncedLyrics` 直传面板，无 `amll.bundle.js`/`WebViewAssetLoader`/`evaluateJavascript`；`rememberLyricPositionProvider` 帧外推保持 60fps（仅当前行重组）。
- **归档 WebView 方案（08-31，09-02 已删除）**：曾用 `app/src/main/assets/amll/` → `WebViewAssetLoader https://appassets.androidplatform.net` → `window.update*` 注入 + `BackgroundRender`，因手搓需求与包体积已彻底删除 `FullPlayerWebView.kt`/`LyricWebView.kt` 与 `app/src/main/assets/amll/*`（Git 可回溯）。
- **禁止**再引入 `WebView` 或 `lyrics-ui KaraokeLyricsView`（已 vendored 但沉浸主路径零引用）；新需求在 `NativeLyricsPanel`/`NativeKaraokeLine` 上扩展。

### 手搓渲染契约（09-02）

- 数据源 `SyncedLyrics.lines: List<ISyncedLine>`（`KaraokeLine.syllables` 逐词时轴 / `SyncedLine` 整行）→ `WordInfo(start,end,text)`；翻译 `translation/phonetic` 按 `translationEnabled` 显隐。
- 当前行判定 `computeCurrentIndexNative(lines, positionMs)` 线性扫描；`currentIndex` 100ms 轮询更新，仅索引变化触发滚动重组。
- 距离衰减：`alpha 1/0.45/0.28/0.18` + `scale 1.05/0.92` + `blur 6.dp (distance>=2)`；当前行逐词 `fraction=(pos-start)/(end-start)` lerp `White 0.35→White`，长词>6字符按字符拆分二次 lerp 实现字符级扫过。
- 弹簧（09-02 补齐）：滚动 `LazyColumn` 默认即 `spring`（`stiffness≈400, damping≈0.8`），行 `placement` 用 `Modifier.animateItem(placementSpec=spring(300,0.75))`，行焦点 `alpha/scale/blur` 经 `animate*AsState(spring(350/380/300))` 弹性过渡；`isScrollInProgress` 防抖 3s 回中同样弹簧。
- 滚动：`LazyColumn` `animateScrollToItem(currentIndex)` 居中，手势 `isScrollInProgress` 时 3s 防抖后恢复；`onLyricAtTopChange` 供外层下滑关闭分流。

## 2. 背景生命周期治理（纯 Compose）

- 背景不得因「无歌词」卸载：`FlowingLightBackdrop` 始终渲染；空态在前景 `NativeLyricsPanel` 显示占位，背景照常。
- **原生背景**：`FlowingLightBackdrop`（`clipToBounds + fallback 纵向渐变 + cover blur28/scale1.08/alpha0.75 + Canvas 3 Blob + 暗色 scrim + 顶部高光`，`flowSpeed=2 → 6000ms 周期`）。
- 粘性封面三段语义（PlayerViewModel.stickyCover）：新曲有封面即更新；无封面**沿用旧值**；仅无当前曲才清空。
- `stickyCover == null` 时沿用旧帧（`FlowingLightBackdrop` 不清背景），避免切歌闪黑。

## 3. 封面加载

- **现行（纯 Compose）**：`stickyCover` 直喂 `FlowingLightBackdrop`/`CoverHero` 的 `AsyncImage`（coil 天然支持 `file://`/`content://`/`data:`/`https:`），无 `fetch->blob->dataURL` 转码与 `WebViewAssetLoader`。
- **归档 WebView 转码**：曾用前端 `fetch->blob->dataURL` 与 `coverUriToAppAssetsUrl` 映射，09-02 已随 WebView 删除。

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

## 7. 沉浸式播放页（纯原生 Compose，09-02 起自研）

### 7.1 职责划分

| 组件 | 文件 | 职责 |
|---|---|---|
| `PlayerScreen` | `feature/player/PlayerScreen.kt` | 容器：`drag-layer`（`offset {IntOffset(0,dragOffsetY)}` + `isLyricAtTop` 手势分流）+ `FlowingLightBackdrop` 透底 + `HorizontalPager` 双面板（手机）/ 左右双栏+`TabletBottomBar`（平板）；阈值 `clamp(0.18*h,96,160)`，回弹 `0.22s CubicBezier(0,0,0.58,1)`，底部 `180dp` 排除；`activePanel` 经 `onActivePanelChange` 同步外层 `isLyricPanelActive`，`onLyricAtTopChange` 供下滑分流 |
| `NativeLyricsPanel` | `feature/player/lyric/NativeLyricsPanel.kt` | 原生歌词面板：`LazyColumn` + `currentIndex` 100ms 轮询 + `animateScrollToItem` 居中 + `isAtTop` snapshotFlow + FAB 3s 显隐；空态占位 |
| `NativeKaraokeLine` | `feature/player/lyric/NativeKaraokeLine.kt` | 单行渲染：距离 `alpha/scale/blur` + 逐词/逐字符 `AnnotatedString` lerp（`fraction=(pos-start)/(end-start)`）+ 和声 `italic/End` + 翻译/音译二行；点击 `onSeek(line.start)` |
| `LyricsPanel` | `lyric/LyricsPanel.kt` | 兼容壳：直接委托 `NativeLyricsPanel`（保留旧签名，新增 `onLyricAtTopChange`） |
| `PlayerViewModel` | `PlayerViewModel.kt` | 粘性封面 `stickyCover` + `parsedLines`（`LyricsParser→AmllMapper`）+ `lyricPosition`（100ms 钳制 `min(pos,lastLineEnd)`）+ `translationEnabled/hasTranslation` |

> 归档：`FullPlayerWebView.kt`/`LyricWebView.kt` 与 `app/src/main/assets/amll/*` 已于 09-02 彻底删除（Git 可回溯）；`LyricWebView` 的 `WebViewAssetLoader/isPageReady/32ms` 契约已下线。

### 7.2 布局契约（复刻 Capacitor `PlayerPage.vue` BEM，09-02 原生增量）

- `.player-page__drag-layer`：`offset {IntOffset(0,dragOffsetY)}`（非 `graphicsLayer`，确保下滑暴露区重绘底表）+ `isLyricPanelActive && !isLyricAtTop ? Modifier : pointerInput` 分流；阈值 `clamp(0.18*h,96,160)`，回弹 `0.22s CubicBezier(0,0,0.58,1)`；底部 `180dp` 排除区内 `ignoreDrag=true` 不参与下滑判定，保证底部按钮点击直达 Compose。
- `.player-page__panels`：手机 `HorizontalPager(pageCount=2, 0.22s easeOut)` 替代 `width 200% translateX`，平板 `Row weight 1f + gap 24 + TabletBottomBar` 双栏；`info-panel/lyric-panel` 各 `weight 1f`；无 `isInNoSwipeZone` JS 逻辑，手势由 Compose `detectVerticalDragGestures` + `HorizontalPager` 原生分流。
- `.player-page__cover-hero`：`aspectRatio(1) max-height min(50vh,420px)`，圆角 `12dp`，信息页弹性居中；复用既有 `CoverHero`。
- `info-panel/progress/controls/mode-bar`：`ProgressSection` 自绘双轨 + `time-row 12px tabular 0.68`；`ControlsRow 48/56 + ModeBar 40 max320` 均用 `SaltIconButton`，无 `is-active`。
- `lyric-panel`：`NativeLyricsPanel` 纯 Compose，`LazyColumn vertical 120dp padding` + `mainFontSize clamp(22,6.5vw,32)`，手势由 `isUserScrolling 3s` + `onLyricAtTopChange` 协同。

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

- 歌词空时发空数组（非 null），`LyricsPanel` 空态占位，背景不卸载。
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
