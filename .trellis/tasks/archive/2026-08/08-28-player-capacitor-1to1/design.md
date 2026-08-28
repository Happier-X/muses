# 设计 — 沉浸式播放页1:1复刻Capacitor

## 边界

- 输入：`PlayerViewModel` 暴露的 `stickyCover/parsedLines/position/duration/isPlaying/hasTranslation/translationEnabled/lyricPosition` 等 Flow
- 输出：Compose UI `PlayerScreen` 及其子 Composable（`FixedSongHead/CoverHero/MetaWindow/ProgressSection/ControlsRow/ModeBarRow/IOSLyricsPanel/FlowingLightBackdrop`）
- 不跨模块：不改 `core:model/core:data` 数据层，仅 UI 层对齐

## 对照契约

| Capacitor (Vue) | Native (Compose) | 对齐策略 |
|---|---|---|
| `.player-page__drag-layer` `translateY(dragOffsetY)` + `0.22s easeOut` | `Box.graphicsLayer translationY` + `Animatable tween 220 CubicBezier(0,0,0.58,1)` | 已对齐，保持 |
| `.panels` `width:200% translateX(-active*50%)` `0.22s easeOut` | `HorizontalPager beyondViewportPageCount 0` + `animateScrollToPage` | 已改 Pager 避免半屏，视觉等价即达标 |
| `.player-page__cover-hero` `max-height min(50vh,420)` `aspect 1 contain` | `CoverHero` 固定 dp | 改为 `BoxWithConstraints` 取 `maxHeight*0.5` 与 420 取 min，`Modifier.aspectRatio(1).fillMaxWidth().heightIn(max=computed)` |
| `.song-meta` 79 / `meta-window` -29.5 / `meta-line` scale/opacity/blur | `MetaWindow` 已有 79/-29.5/scale | 微调 tween 时长为 260 + `CubicBezier(0.32,0.72,0,1)`，验证矮屏 19.5 逻辑 |
| `m-range color primary thumb hidden` + `time-row 12 tabular` | `Slider white 0.22` + chip | 将轨改为 `MaterialTheme.colorScheme.primary` 或保持白（以设计令牌为准），thumb `alpha 0` 保留隐藏，时间行 `fontFeature tabular` |
| `controls gap clamp` `icon-lg 28 fill+stroke` `mode-bar max320` | `ControlsRow gap 28` `SaltIconButton LG` | 保留 28/20 两档，无 is-active，mode-bar 加 `widthIn(max=320).fillMaxWidth` |
| `lyric-fabs clear text-white/80 is-active仅翻译` | `FilledTonal/Filled` | 改为 `SaltIconButton` variant clear，`tint White alpha 0.8`，翻译激活时 `White` |
| `bg opacity 0.75 blur 28 scale1.08 radial 0.07` | `blur32 alpha0.68 / canvas 0.075` | 改为 blur28 alpha0.75，Canvas 高光 alpha 0.07，对齐 scss |

## 数据流

- Capacitor：`playerState.currentSong/position/duration/status` → `computed lyricArtist/displayCoverSrc/displayedWindow/effectiveSeekPosition` → `LyricPlayer/BackgroundRender/m-range`
- Native：`ViewModel StateFlow` → `collectAsStateWithLifecycle` → `IOSLyricsPanel/FlowingLightBackdrop/Slider`；保持单向，无双向同步

## 兼容与回滚

- 兼容已安装用户无数据迁移，仅 UI 像素调整
- 回滚：单文件 revert `PlayerScreen.kt` 即可，首轮不拆多提交

## 验证

- `./gradlew :feature:player:assembleDebug :app:assembleMusesDebug`
- 真机/模拟器对比：手机 360x800 与窄高 360x520 封面是否正方形、五行小窗是否 79 居中、进度条是否隐藏 thumb、Fab 是否 clear 风格
