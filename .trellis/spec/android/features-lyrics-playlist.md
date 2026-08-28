# 特征·歌词 / 播放列表 / 响度均衡 — 开发规范（M2）

> 适用于 仓库根 Android 工程的 AMLL 歌词渲染、播放列表管理、响度均衡相关改动。Web 层对应契约见 `spec/frontend/features-player.md`，本文是其原生等价实现 + M2 新增事实。

---

## 范围 / 触发条件

- 改动 `feature/player/lyric/*`（AmllWebView、LyricsParser、AmllMapper）、`frontend/amll-web/`（AMLL 前端页面）
- 改动 `feature/playlist/*`、播放列表 Room 表
- 改动响度均衡链路（LoudnessCalculator/LoudnessController/replayGainTrackDb 列）

## 1. AMLL 渲染 = 原生 Compose（WebView 已废弃，归档保留）

- **现行方案（native，08-28 起）**：纯 Compose `PlayerScreen` 直接复刻 `MeloX-Android` 的 `MeloXFlowingLightBackdrop` + `MeloXIOSLyricsPanel`，**不再使用 WebView**。`PlayerViewModel` 经 `LyricsParser` + `AmllMapper` 解析为 `AmllLyricLine`（`words[{startTime,endTime,word}]`）→ `parsedLines` 供 `MeloXIOSLyricsPanel`（逐词）与 `MetaWindow`（五行小窗）同源消费；封面经 `stickyCover` 粘性传递给 `MeloXFlowingLightBackdrop`。
- **归档 WebView 方案（仅历史参考，勿复用）**：曾用 Vite 打包 `@applemusic-like-lyrics/core` 进 APK assets（`frontend/amll-web/` → `feature:player/src/main/androidAssets/amll/`）→ `AndroidView` 包 WebView → `WebViewAssetLoader` 以 `https://appassets.androidplatform.net/assets/amll/index.html` 加载 → `evaluateJavascript` 注入；已于 08-28 原生重构中删除（`AmllWebView.kt` / `frontend/amll-web/` 已移除），原因见 §2 末与 index.md 播放契约。
- **禁止**重新引入 WebView 歌词栈；新需求在 `MeloXIOSLyricsPanel` / `MeloXFlowingLightBackdrop` 上扩展。

### 就绪握手（0621054，P4.4 黑屏修复）

- **onPageFinished 触发早于 ES module 执行**：此时 `window.updatePlayerState/updateLyrics` 未定义，
  Kotlin 首轮 evaluateJavascript 注入静默丢失；无后续状态变化时播放页表现为纯底色黑屏。
- 契约：前端 module 尾部经 `nativeBridge.onAction('{"action":"ready"}')` 上报就绪；
  Kotlin 收到 ready 后全量重推当前 playerState 与歌词载荷（经 ref 取最新值，防闭包捕获过期）。

### 踩坑记录（92bf1a2，P4.3 歌词面板修复）

1. **androidAssets 目录必须显式注册为 assets 源**：`feature/player/build.gradle.kts` 加 `assets.srcDir("src/main/androidAssets")`——M1 起该目录从未注册，WebViewAssetLoader 找不到 index.html → ERR_INVALID_RESPONSE，且报错被 WebView 白屏吞掉难定位
2. **WebView 尺寸自适应用 ResizeObserver 不用 window.resize**：Android WebView 初始布局高度为 0，后续 AndroidView 获得真实尺寸不再派发 resize → 背景 canvas 高度 0 永不可见；amll-web 侧 `new ResizeObserver(resize).observe(document.body)` 兜底
3. **AndroidView 嵌入面板区域时 offset 位移要加在 AndroidView 自身而非父容器**（父容器位移会连带裁剪/命中区域错位）；背景歌词解耦 = 背景层与歌词层各自独立 AmllWebView 实例

### 桥接口签名（window 级，前端 `amll-web/src/main.ts` ↔ Kotlin `AmllWebView.kt`）

| JS 接口 | 入参 | 调用时机 |
|---|---|---|
| `updateLyrics(payload: string)` | JSON 字符串 `{lines, coverUrl, songId}` | 页面 ready 后首次 + 每次切歌 |
| `updatePosition(positionMs: number)` | ms 数值 | VM 侧 ~100ms 轮询节流，仅 isPlaying 时发射 |
| `pauseRender()` / `resumeRender()` | 无 | Lifecycle ON_STOP / ON_START |
| `updatePlayerState(payload: string)`（P4.4） | JSON `{title, artist, coverUrl, isPlaying, positionMs, durationMs, buffering, repeatMode:'off'\|'one'\|'all', shuffleEnabled, hasTranslation, translationEnabled, insetTopPx?, insetBottomPx?}` | 页面 ready / 任一状态变化（title 空串 = 无播放歌曲，前端显空态；coverUrl=null 粘性沿用） |

JS→Native（P4.4）：前端经 `window.nativeBridge.onAction(json)` 发动作 `{action:'playPause'|'next'|'previous'|'seekTo'(positionMs)|'setRepeatMode'(mode)|'setShuffle'(enabled)|'toggleTranslation'|'openQueue'|'close'}`；Kotlin 侧 `NativeBridge` 回调在 JS 线程，AmllWebView 内部统一 post 主线程后再分派。seek 为一次性语义：拖动 preview 由前端本地完成，抬起才发。

- **payload 注入必须经 `AmllMapper.quote()` 包成 JS 字符串字面量**——前端内部做 `JSON.parse`，直接内插对象会被 ToString 成 `[object Object]`。
- `songId` token：前端校验过期注入丢弃。

## 2. 背景生命周期治理（原生 Compose）

- 背景不得因「无歌词」卸载：`hasLyric = parsedLines.isNotEmpty()` 仅作语义保留，`MeloXFlowingLightBackdrop` 始终渲染；空态在前景 `Column` 显示占位（"暂无播放歌曲"），背景照常。
- **原生 Compose 无需 pauseRender/resumeRender**：流体 Blob 由 `rememberInfiniteTransition` 自驱动，生命周期跟随 Composable；封面虚化由 `coil AsyncImage` + `blur(32.dp)` 直映，无 PIXI ticker。
- 粘性封面三段语义（PlayerViewModel.stickyCover）：新曲有封面即更新；无封面**沿用旧值**；仅无当前曲才清空。
- `stickyCover == null` 时沿用旧帧（`MeloXFlowingLightBackdrop` 不清背景），避免切歌闪黑。

> 归档：WebView 时代 ON_STOP→pauseRender / ON_START→resumeRender 已废弃。

## 3. 封面加载

- **原生 Compose**：`stickyCover`（已是 `coverUriToAppAssetsUrl` / `data:image` 转换后结果）直接喂 `AsyncImage(model = coverUri)` 与 `MeloXFlowingLightBackdrop`，无 WebView 混合内容限制；coil 原生支持 `file://` / `content://` / `data:` / `https:`。
- **归档 WebView 混合内容规避（已废弃）**：页面源是 https 时 `file://` 会被拦截，曾统一经 `coverUriToAppAssetsUrl(uri, cacheDirPath)` 映射为 `https://appassets.androidplatform.net/cache/...`。

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

## 7. 原生沉浸式播放页（MeloX 复刻，08-28）

### 7.1 职责划分

| 组件 | 文件 | 职责 |
|---|---|---|
| `PlayerScreen` | `feature/player/PlayerScreen.kt` | 沉浸式容器：固定头部 + 双面板滑动（`HorizontalPager` 0.22s easeOut 等价 `panels 200% → translateX(-activePanel*50%)`）+ 进度/控制 + 平板双栏；数据源 `PlayerViewModel` |
| `FlowingLightBackdrop` | `feature/player/backdrop/FlowingLightBackdrop.kt` | 流体 Blob（`Canvas` 3 径向渐变 + `infiniteRepeatable` `phase` 驱动）+ 封面虚化（`AsyncImage` + `blur(28.dp)` + `scale 1.08` + `alpha 0.75`）+ 暗色 scrim + 顶部高光；fallback 对齐 `.fallback-background`（`#171b2b→#0a0c14→#05070d` + 紫径向 50%/18%）；`flowSpeed=2` 约 12s 一圈 |
| `LyricPanel` | `PlayerScreen.kt` 内（`lyric/LyricsPanel.kt` 为同构副本，import 被遮蔽） | 完整歌词：逐词 `alpha`/`ExtraBold`（`wordFadeWidth 0.5` 二段近似）、翻译/音译显隐、和声 italic 标记、点击跳转 `onSeek(line.startTime)`、自动居中滚动 `animateScrollToItem(current-2)`、FAB 200ms fade + 3s idle 隐藏 |
| `MetaWindow` | `PlayerScreen.kt` 内 | 五行小窗预览：`79px` 视口 + `translateY -29.5` 居中、当前行 `scale 1.05 / alpha 1.0`（颜色 `0.92`）非当前 `0.92 / 0.55`（颜色 `0.6`）、`transform-origin left center`、相邻切行窗口整体上移 0.4s `cubic(0.32,0.72,0,1)`、行动画 `spring(0.84, 800)`，窄屏降为单行 |
| `PlayerViewModel` | `PlayerViewModel.kt` | 粘性封面 `stickyCover` + `parsedLines`（经 `LyricsParser`→`AmllMapper`）+ `lyricPosition`（100ms 轮询钳制 `min(pos, lastLineEnd)`）+ `translationEnabled/hasTranslation` |

### 7.2 布局契约（复刻 Capacitor `PlayerPage.vue` BEM，08-28 增量 1:1）

- `.player-page__drag-layer`：`graphicsLayer translationY = dragOffsetY`，垂直下滑 ≥阈值 `clamp(0.18*h, 96,160)` 触发 `onClose`，否则 0.22s `CubicBezier(0,0,0.58,1)` 回弹；**歌词面板激活（`activePanel==1`）时禁止下滑关闭**（对齐 `canStartVerticalDismiss → isLyricPanelTarget`）。
- `.player-page__panels`：手机 `HorizontalPager`（`beyondViewportPageCount 0`）+ `animateScrollToPage 220ms easeOut`，等价 200% → `translateX(-activePanel*50%)`；平板收缩为 `Row weight 1f` 双栏 + 底部 `TabletBottomBar`（渐变背景 `rgba(5,7,13,0)→0.55` + 进度全宽 + 三段式控制，padding `6 24 calc(8+safe)`）。
- `.player-page__cover-hero`：`aspectRatio(1)` 正方形，`max-height min(50vh,420px)` 内 contain，窄屏 `min(34vw,150dp)`，圆角 12dp。
- `info-panel`：padding `calc(16+safe) 24 16`；`info-inner` gap 14（`song-meta` 下边距 18 → 进度间距 32）；断点收紧 ≤720 gap 4 / ≤520 gap 2，mode-bar max 320/280/260。
- `progress-range`：白色填充 + `rgba(255,255,255,0.25)` 底轨（全局 `.player-overlay .progress-range` 覆盖 primary）、thumb 隐藏、时间行 12px tabular `rgba 0.68`、缓冲 11px `0.55`、`formatTime` 分钟补零（`03:45`）。
- `lyric-panel`：行字号 `clamp(22px,6.5vw,32px)`（平板 `clamp(20,2.4vw,30)`）统一、行水平边距 24、无当前行底色（AMLL 行无背景）、空态 `17px/600 + 13px/0.65` 无图标；`lyric-fabs` left/right 12、bottom `calc(8+safe)`、200ms fade。
- 空态：`placeholder-cover` 渐变圆角方块 + `♪ 48px`、标题 `20px/600`、描述 `14px/1.5/0.75`。
- `PhoneImmersiveLayout` 含固定头部 `FixedSongHead`（常驻，无指示器）、panels；`TabletImmersiveLayout` 头部移入左栏、右栏歌词无 play FAB。

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
