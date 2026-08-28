# 设计 — 沉浸式播放页一比一复刻 Capacitor

## 架构与边界

- **宿主**：`feature:player/PlayerScreen.kt` 为唯一渲染入口，`PlayerViewModel` 仅暴露 `StateFlow`，不依赖 `Context` 或 `Room/OkHttp` 直引（复用 `core:*` 接口）
- **分层**：`MeloXFlowingLightBackdrop`（背景）与 `MeloXIOSLyricsPanel`（歌词）为纯 UI 叶子组件，`PlayerScreen` 负责布局编排与手势；数据解析 `LyricsParser→AmllMapper` 在 `PlayerViewModel` 后台线程完成
- **边界**：编辑表单与歌曲操作弹窗不入本次边界，仅保留 `onOpenEditMeta / onOpenQueue` 回调；播放服务 `PlaybackService` 与 `PlayerConnection` 不改动

## 数据流与契约

```
SongEntity(lyrics, coverUri) --Room observeById--> PlayerViewModel
  ├─ stickyCover: String? (三段粘性：有即更新 / 无沿用 / 无曲清空) → MeloXFlowingLightBackdrop(coverUri) + CoverHero
  ├─ LyricsParser.parse(raw) on Dispatchers.Default → SyncedLyrics?
  ├─ AmllMapper.toAmllLines(synced) → List<AmllLyricLine> {words{startTime,endTime,word}, startTime,endTime, translatedLyric, romanLyric, isBG, isDuet}
  ├─ _hasTranslation / translationEnabled → _parsedLines 过滤后暴露 parsedLines
  └─ lastLineEndMs → lyricPosition 100ms 轮询 `coerceAtMost(lastLineEndMs)` 钳制

PlayerConnection.position (500ms) / lyricPosition (100ms) + isPlaying/isBuffering/repeat/shuffle → PlayerScreen
  ├─ PhoneImmersiveLayout: FixedSongHead + panels(200%) + InfoPanel(MetaWindow+Progress+Controls+ModeBar) + MeloXIOSLyricsPanel
  └─ TabletImmersiveLayout: 左栏(面板内头部+CoverHero) + 右栏(MeloXIOSLyricsPanel) + TabletBottomBar
```

- **粘性**：`stickyCover==null` 时 `MeloXFlowingLightBackdrop` 不清旧帧；`hasLyric` 仅语义保留，背景始终渲染
- **时间**：`effectiveSeekPosition` 本地 preview，`onValueChangeFinished` 才 `onSeekEnd`；`displayedWindow` 取 `currentIdx-2..+2` 五联

## 兼容性与迁移

- **旧 WebView 已删**：`AmllWebView` / `frontend/amll-web` / `androidAssets/amll` 不再存在，禁止回退；`AmllMapper.quote` 等注入逻辑已废弃，文档保留归档语义
- **数据库**：无 schema 变更；仅 `SongEntity` 读路径 `lyrics/coverUri` 复用
- **主题**：沿用 `SaltTheme` / `SaltIconButton`，深底白字 `rgba(255,255,255,0.9/0.92/0.8)` 与旧版一致；`GlassSurface` 不在播放页使用（全屏沉浸由背景承担）

## 权衡与取舍

- **逐词粒度**：`wordFadeWidth 0.5` 在 Compose 侧以词级二段近似（已唱/未唱/正在），不做字符级 shader，保性能且与 `lyrics-core` Syllable 边界一致
- **模糊与缩放**：`MetaWindow` 非当前行 `blur 0.6px` 以 `alpha 0.55` 近似（文本 blur 成本高），当前行 `scale 1.05` 经 `animateFloatAsState 260ms [0.32,0.72,0,1]` 近似旧版 `spring stiffness 240 damping 26`
- **指示器**：旧版无显式指示器，本次保留两点 `20x6 / 6x6` 轻量提示，不影响手势
- **安全区**：`statusBarsPadding` + `safe-area-inset-bottom` 经 `12px` padding 近似，未引入 `WindowInsets.navigationBars` 动态计算，后续可按需补

## 运行与回滚

- **运行**：纯 Compose，无额外权限；`rememberInfiniteTransition` 与 `AsyncImage` 生命周期跟随 Composable，无需 `pauseRender/resumeRender`
- **回滚**：若验收不通过，单文件回退 `PlayerScreen.kt` + 删除 `backdrop/` / `MeloXIOSLyricsPanel.kt` 增量即可；`PlayerViewModel` 与 `core` 无侵入，回滚成本低
