# 沉浸式播放页 AMLL WebView 1:1 复刻 DroidMate — Design

## 边界

- 修改范围：`feature/player/lyric/*`（LyricWebView、LyricsPanel、AmllMapper、LyricsParser 可选）、`app/src/main/assets/amll/*`（index.html + amll.bundle.js 产线）、`feature/player/PlayerScreen.kt`（容器与 FAB 透传）、`feature/player/backdrop/FlowingLightBackdrop.kt`（是否与 WebView 背景二选一）、`feature/player/PlayerViewModel.kt`（粘性封面/翻译开关对齐）
- 只读对照：`.workbuddy/tmp/amll-droidmate/frontend/src/main.tsx`、`frontend/styles.css`、`frontend/vite.config.js`、`app/src/main/java/dev/amll/droidmate/components/AMLLLyricsView.kt`
- 不碰：`core:media` 播放恢复、`core:data` 刮削/持久化、`core:lyrics` TTML 在线链路

## 现状与差距

- 现状已抄 70%：`WebViewAssetLoader + cssInliner + onPageReady 重发 + player.element 手动挂载 + cursorMs 单调去重` 已落地，数据通路 59 行可达 JS，但渲染层卡在 `mix-blend-mode:plus-lighter + mask-image` 在 Chromium 110 不出字（`web13..web20` 红通道为 0）。
- DroidMate 完整链：`DomLyricPlayer({container:root}) + BackgroundRender(Mesh/Pixi) + tick: player.update(delta)每帧 + 刷新率节流 updateTime + LayerType HARDWARE + 字体/背景批量配置 + file->dataURL 封面`。Muses 缺：前端 `main.tsx` 未对齐（自写 bundle 缺 BackgroundRender 与乐观 seek）、Kotlin 侧 50ms 无节流全刷、缺硬件层、缺字体注入。

## 架构抉择

**选型：完全对齐 DroidMate 前端三件套（main.tsx + styles.css + vite.config.js），Kotlin 侧对齐 AMLLLyricsView.kt 的 update 策略。**

- 理由：零散修 mask 无法收敛；DroidMate 已验证的“透明 WebView + 虚拟域名 + 内联 CSS + 每帧 tick”链路在多机型通过，改动半径最小。
- 备选（回退到纯 Compose MeloXIOSLyricsPanel）不选：用户明确要 WebView 效果，且纯 Compose 在 LazyList 波浪 stagger 上已验证无法无闪复刻 Web 行弹簧。

## 数据契约

### Kotlin -> JS (window.*)

| 接口 | 载荷 | 触发 |
|---|---|---|
| `updateLyrics({lines:[{words:[{word,startTime,endTime,romanWord?}], text, translatedLyric, romanLyric, startTime,endTime,isBG,isDuet}]})` | 严格 DroidMate `buildLyricsJson` 字段名；`endTime>startTime` 否则 +1ms 防 NaN | `isPageReady && lyrics !== lastLyrics` 引用比较 |
| `updateTime(number)` | `Math.trunc(ms)`，暂停且已同步过则忽略 | `now - lastUpdate >= frameIntervalMs`（60Hz=16ms,120Hz=8ms），记录 `lastUpdate` |
| `setPaused(bool)` | `!isPlaying` | `lastIsPlaying !== currentIsPlaying` 去抖 |
| `updateAlbumArt(string)` | `file://` 先 `fetch->blob->dataURL`，http 加 `t=Date.now()` 防缓存，空串清空 | `lastAlbumArtUri !== albumArtUri` 且长度校验 |
| `configureLyricMotion({enableSpring,enableScale,enableBlur,springPosY,...})` | 来自 AMLLSettings | 引用比较 |
| `configureLyricBackground({renderer,cssProperty,fps,renderScale,staticMode})` | 同上 | 引用比较 |
| `applyFontSettings({effectiveFamily,files:[{familyName,uri}]})` | `uri=https://appassets.androidplatform.net/fonts/{id}` | 签名比较 |

### JS -> Kotlin (window.Android)

- `onPageReady()` → 置 `isPageReady=true` 并重推 `updateLyrics/updateTime/setPaused/背景/字体`
- `onLineClick(lineIndex,startTime)` → `post { evaluate(setIsSeeking true); evaluate(updateTime(seekTime)) }` 乐观更新 + 回调 `onSeek(startTime)`
- `log(msg,level)` → Timber/logcat

## 前端产线

- 复用 DroidMate `frontend/vite.config.js`：`wasm + topLevelAwait + cssInliner`，`lib entry src/main.tsx -> es amll.bundle.js`，`cssCodeSplit false, minify false, sourcemap true, inlineDynamicImports true`
- `src/main.tsx` 关键段落移植：`attachElementToRoot`、`createBackgroundRenderer`（Mesh/Pixi 双候选）、`initAMLL`（`DOMContentLoaded` + `onPageReady` 上报）、`tick`（每帧 `player.update(delta)`，不因 paused 跳过）、`updateLyrics`（`setLyricLines/setLyrics/updateLyrics` 兼容 + `calcLayout+update(0)`）+ `updateTime`（paused 忽略微波动）+ `updateAlbumArt`（file->dataURL）
- `styles.css` 原样引入：`:root --amll-*`、`html,body transparent`、`#app 100%`、`amll-lp-line translateZ(0) + will-change`、`--amll-lp-font-size max(12px,min(8vw,32px))`、`::-webkit-scrollbar 0`
- 产出：`app/src/main/assets/amll/amll.bundle.js`（~550KB）+ `app/src/main/assets/amll/index.html`（`base ./` + `import('./amll.bundle.js')`）

## Kotlin 侧重构

- `LyricWebView.kt` 重写为 `AMLLLyricsView.kt` 1:1 结构：`rememberUpdatedState` 防闭包过期、`isPageReady` 闸门、`lastLyrics/lastAlbumArtUri/lastIsPlaying/lastTimeUpdate` 去重、`LAYER_TYPE_HARDWARE`、`clearCache/clearAllCache`、`onRelease destroy()`、`GestureDetector + isInteractive 透传`
- `LyricsPanel.kt` 仅作 FAB/点击透传壳，逐词与滚动全由 WebView 内 AMLL 承担，不再做 `computeCurrentIndex` 自绘回退
- `AmllMapper.toLyricLinesJson` 对齐 DroidMate：`romanWord` 逐词映射（可选）、`cleanBackgroundText`、整行/逐词 `end>start` 防御、附加 `text` 字段

## 兼容与性能

- WebView 版本：目标 Chromium 110+ 可行；118+ 最佳；对 110 的 mask 兼容，已在前端加入探针分支（卡拉OK 不可见时回退到非 mask 渲染路径，具体由 core 侧 `wordFadeWidth` 控制）
- 性能：HARDWARE 层缓存静态帧 + 每帧仅 tick player，不每帧重绘背景；时间节流按刷新率避免 UI 线程阻塞；字体/背景配置签名比较避免 recompose 重建 JSON

## 回滚

- 保留当前 `amll.bundle.js` 为 `amll.bundle.js.bak`；Kotlin 侧 `LyricWebView` 旧分支以 `if (useDroidMate) ... else ...` 特性开关保留一版本，验证失败切回
