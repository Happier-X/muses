# 特征·播放器 — 开发规范

> 本项目的音频播放、系统媒体通知、媒体会话体系在 `src/features/player/` 内统一管理。

---

## 范围/触发条件

涉及音频播放（本地、WebDAV）、通知栏媒体卡片、系统媒体会话（MediaSession / 媒体按键）的任何改动，都应在本规范约束下进行。

---

## 基础架构（战后记录）

### 前端播放器封装

- 业务层通过 `src/features/player/controller.ts` 调用统一的 `AudioPlayerNative` 抽象接口（`native.ts` 导出）。
- **永远不要**在 controller 层直接引入或调用原生播放器插件；播放器插件只存在于 `native.ts` 的封装层。
- `AudioPlayerNative` 现在底层实际使用 ExoPlayer Media3（播放引擎）和 `@capgo/capacitor-media-session`（系统媒体通知与按键映射）。
- ExoPlayer 通过自定义 `AudioPlayer` Capacitor 插件封装，JS 层通过 `AudioPlayerBridge` 接口调用。

### 通知架构（整理版）

以下是在本任务中加密的事实，未来任何媒体通知改动都不能绕开：

1. **不再使用 media3 `MediaSessionService`**  
   该项目历史上曾尝试使用 media3 的 `MediaSessionService + DefaultMediaNotificationProvider` 机制，但在自定义 `ACTION_PLAY` Intent 通道下，`DefaultMediaNotificationProvider` 的 `shouldShowNotification` 条件一直未完全满足，导致通知无法稳定显示官方媒体卡片。  
   **因此**：不再使用 media3 的 MediaSessionService 作为媒体通知创建者。

2. **ExoPlayer Media3 负责播放**  
   通过自定义 `AudioPlayer` Capacitor 插件封装 ExoPlayer。  
   音频焦点默认开启（`handleAudioFocus=true`），可通过 `setAudioFocus` 方法关闭。

3. **`@capgo/capacitor-media-session` 负责通知**  
   - 通知栏使用 `MediaStyle + MediaSessionCompat`，官方模板 `Notification$MediaStyle`。
   - 必须注册以下 action handler：
     - `play` → `resumePlayback`
     - `pause` → `pausePlayback`
     - `stop` → `stopPlayback`
     - `previoustrack` → `playPreviousFromQueue`
     - `nexttrack` → `playNextFromQueue`
   - 每次播放状态、进度变化后，必须同步到 `setPlaybackState` / `setPositionState`。
   - **进度推进不得只信插件 `currentTime` 事件**：`native.ts` 在 `playing` 期间须以 `getCurrentTime` 轮询兜底（约 **500ms**，#50 从 250ms 降频）；`play`/`resume`/`seek`(仍 playing) 启动，`pause`/`stop`/`unload` 停止。低于 0.05s 的轮询变化不 emit。不改 `node_modules/@capgo/*`（#47/#50）。
   - **MediaSession position 不得随每个 UI tick 跨 bridge**：position 至多约 1s 同步一次；播放状态变化仍立即同步（#50）。
   - native-audio 诊断日志默认静默；仅 `muses:debug-native-audio=1` 时输出（#50）。
   - song 切换时，同步 `setMetadata`（title/artist/album/cover）。
   - **`loading`/`finished` 不得映射为 `none`**：应保持 active（当前实现映射为 `playing`），否则插件会 stop 前台服务再重建，造成通知延迟/闪断（含队列自动下一首窗口）。
   - **metadata 两段式更新**：先推 title/artist/album + **占位清空 artwork**（1×1 中性 JPEG `data:`），封面经 `prepareArtworkDataUrl` 转 `data:` 后二次 `setMetadata`；用 token 丢弃过期封面回调。
   - **空 `artwork: []` 不能清封面**：capgo Android 插件仅在 `artwork[].src` 非空时才 `urlToBitmap`；传入空数组会**保留上一首 Bitmap**。无封面 / prepare 失败 / clear 时必须用显式 `data:` 占位图强制覆盖。
   - **懒扫描补全封面后必须 re-sync**：`scanSongMetadata` → `syncDisplayStateFromSong` 在当前曲 `coverUri`/title/artist/album 变化时须再调 `syncMediaSessionSong`，不能只更新 UI。
   - **返回键退出界面 = `App.minimizeApp()`**（`moveTaskToBack`），禁止用 `App.exitApp()` 作为 Tab 层返回默认行为；`exitApp` 会 destroy Activity → media-session unbind → 前台服务与通知一并消失。

4. **封面兼容（file:// 无法直接使用）**  
   当前 `@capgo/capacitor-media-session` 只接受 `http://` 或 `data:` URI 作为 artwork。所以遇到 `file://` 封面时，需要通过原生桥接（`AudioPlayerPlugin.prepareArtworkDataUrl`）转换为 `data:image/jpeg;base64,...` 再传给 `setMetadata.artwork`。  
   `prepareArtworkDataUrl` 对 `file://` 优先 `FileInputStream`，`content://` 走 `ContentResolver`；失败 resolve `dataUrl=null`，由前端占位图清空旧封面。

5. **Android 13+ 运行时权限**  
   首次播放前仍然需要通过 `AudioPlayerBridge.ensureNotificationPermission()` 请求 `POST_NOTIFICATIONS`，并在授权失败时静默忽略，不阻塞播放。

6. **前台服务/通知 ID 冲突**  
   capacitor-media-session 使用自己的通知 ID（`id=1, channel=playback`），但我们的旧 `AudioPlaybackService`（当前已清理为空服务）不再产生第二个媒体通知，避免前台服务通知冲突。

7. **finished 自动切歌：complete 即自然播完**（`controller.ts`，08-18-carwith-bg-ctrl-fix 语义变更）  
   - 进度条 / 歌词点击 / 媒体会话 `seekto` 均走 `seekPlayback`；成功后记录 `lastSeekAt`（保护窗约 1500ms）。
   - **complete/STATE_ENDED 的唯一合法来源是播放器真正播完**，不再依赖 position 的「接近结尾」判定：JS 冻结期间 `state.position` 滞后，会误判为伪 finished 而不切歌（CarWith/锁屏播放完暂停的直接原因）。窗口外（`isWithinSeekGuard() === false`）的 finished **无条件** `handlePlaybackFinished` → `advanceToNext`；`duration=0` 不例外。
   - **仅保留 seek 保护窗**：`isWithinSeekGuard()` 内的 finished 视为 seek 到未缓冲区触发的伪结束，恢复 `statusBeforeSeek`、保留 seek 目标进度，不 advance；seek 保护窗优先于一切。
   - 展示位置取 `max(nativePosition, state.position)`，避免 complete 事件 position 回 0 时进度条闪回。
   - `playSong` / `stopPlayback` 必须清理 seek guard，避免新歌首帧误吞真实 finished 或卡住队列。
   - 不修改 capgo 插件源码；远程/未缓冲 seek 触发的 `STATE_ENDED`/`complete` 由 seek 保护窗 + `seekPlayback` 的缓冲上限双保险在前端边界消化。

8. **播放失败自动恢复与有界跳过**
   - `controller.ts` 在 `AudioPlayerNative.play` 失败且 generation 仍匹配时，沿当前 active order 自动尝试未尝试过的下一首；active order 使用 `shuffleOrder ?? items`。
   - 失败恢复链用内部 `Set<songId>` 记录已尝试歌曲，最多检查一轮，禁止列表循环或单曲循环造成无限重试。
   - 单曲循环仅在失败恢复链中临时忽略自身重试；用户 `repeatMode` 配置保持不变，成功后自然结束仍遵循单曲循环。
   - 过期 generation 的失败必须直接丢弃，不推进队列、不覆盖当前歌曲状态；恢复中不要清媒体会话，只有链终止时才清理。
   - 全部候选失败后保留白名单过滤后的安全错误文案，清理缓冲和媒体会话；原生错误、URL、认证头和密码不得进入状态、日志或持久化数据。
   - 失败恢复 helper 保持播放器公开 API 最小化，仅供 controller 内部使用（队列层可提供独立可测试 helper）。

9. **已缓冲进度与 seek 限制**（`bufferedPosition`）
   - `PlayerState.bufferedPosition: number | null`：秒；`null` = 缓冲未知（不画假缓冲条，seek 退化为 duration clamp）。
   - **本地就绪**：`prepareLocalAudioFile` 完成后原生上报 `fullyBuffered`，前端 `bufferedPosition = duration`，缓冲条铺满，可全长 seek。
   - **WebDAV 完整缓存优先**：`native.ts` 播放 WebDAV 时先调 `getCachedWebDavAudioFile({ url })`；仅当返回**完整**文件 URI 时用 `file://` 播放并 `bufferedPosition = duration`（full buffer）。未命中 / partial / 失败则远程 URL + Basic `Authorization` 直链，`bufferedPosition = null`，seek 仅按 duration clamp。
   - **禁止 progressive 播放路径**：原生 `prepareWebDavAudioFile` / 增长中的 `.partial` 不得作为播放路径（可保留兼容代码与 `cancelBufferSession`，但切歌时仍可调用 cancel 清理旧会话）。
   - **seek 上限**：缓冲已知时 `min(duration, bufferedPosition)`；目标越界时 **拒绝 seek**（返回 `false`），不发起原生跳转。歌词点击共用 `seekPlayback`。
   - **切歌 / stop / 播放失败** 必须 `resetBufferState()`，禁止串曲缓冲。
   - 不改 `node_modules/@capgo/*`；缓冲由项目自有 `AudioPlayer` 插件上报，经 `native.ts` 合并进 `stateChange`。
   - 保留 seek 保护窗 + 自然结尾判定作为第二道防线。

9. **下一首 WebDAV 完整预取**（`peekNext` + 音频 + 元信息）  
   - `queue.ts` 提供无副作用 `peekNext()`：按当前 repeat/shuffle 规则解析下一首，**不**改 `currentIndex`。
   - `playSong` 成功进入 `playing` 后调度 `prefetchNextTrack()`：`next = peekNext()`；仅当 next 为 WebDAV 且 `next.id !== current.id` 时：
     1. **元信息写库预取**（`prefetchMetadata.ts`）：并行在线歌词 / 封面 / 文本补缺，成功则 `upsertSong`；**禁止**写当前 `playerState`，**禁止**动 `lyricsMatchToken` / `onlineCoverToken` / `onlineTextToken`。使用独立 `metadataPrefetchToken`，过期丢弃写库。
     2. **音频完整预取**：解析密码并调用 `prefetchWebDavAudioFile`（经 `native.ts` → `AudioPlayerBridge`）。缺密码时仍可跑元信息，不得因无密码整段跳过。
   - 写库策略对齐当前曲：歌词 `shouldPersistOnlineLyrics`；封面仅无安全 `coverUri` 时 `cacheRemoteCover` 后写安全 URI；文本 `needsOnlineTextMeta` + `mergeTextMetaFillEmpty`。
   - 队列变更 / `setRepeatMode` / `toggleShuffle` 后，若当前仍在 `playing`/`paused`，controller 包装的 queue API 会重新 `peekNext` 并调度预取；旧音频下载不取消；元信息以新 token 作废旧写库。
   - 跳过：空队列、单曲循环自身、本地下一首、非 WebDAV（本地下一首**不**做元信息预取，MVP 范围 C）。
   - **禁止**下一首 `scanSongMetadata` 预扫；内嵌标签仍等真正切歌后扫描。
   - 原生音频：`getCachedFile` 命中完整缓存 → `{ cached: true, started: false }` no-op；否则 `downloadInBackground` 完整下载 → `{ cached: false, started: true }`。同 URL 复用进行中会话，不重复写。
   - 旧音频预取不取消；新下一首可并行启动。预取失败静默，不得阻塞当前播放或切歌。
   - 密码只在 controller 解析后传入 bridge，不进 reactive state / localStorage / 日志。
   - 音频预取遵守 `WebDavAudioCache` 缓存上限与淘汰；仅完整目标文件可命中，`.partial`/`.tmp` 不得当缓存。

---

## 沉浸式播放页背景

- AMLL `BackgroundRender` **不得**仅因「当前无歌词」而卸载；有当前曲且有可展示封面（含粘性封面）即渲染。
- 切歌若新曲暂无 `coverUri`，UI 可短时**粘性**使用上一首可用封面作背景与封面槽，待新封面写入后再更新；无当前曲时清空粘性封面。避免闪回默认 `fallback-background`（#20）。
- 封面晚到时 `BackgroundRender` 须用 `key` 绑定封面 URI 以重建；`syncDisplayStateFromSong` **禁止**用库内空歌词清空运行时已有词，仅库内质量严格更优时才替换（#21）。
- `PlayerPage` 用 `k-popup :opened="playerOverlayVisible"`（iOS 全屏底部滑入，关闭态 `translate-y-full` 移出屏幕；无 keep-alive 概念，关闭时内容仍在 DOM 由自绘手势隐藏，AMLL 背景不重建）。App.vue 常驻挂载 `<PlayerPage />`（无 `keepPlayerPageMounted` / translate-y）。打开/关闭必须 `resetDragState()`，下滑关闭前也要清零 `dragOffsetY`，避免再打开半屏（#25）。
- **拖拽/回弹必须闭环，禁止残留半屏**（#08-16-fix-player-drag-stuck）：`dragOffsetY` 归零回弹**不得**只依赖 `watch(dragOffsetY)` 隐式触发（回弹动画进行中再松手会被早退吞掉；motion `stop()` = commitStyles + cancel 会把中间值写进 style 且不触发 `onComplete`，残留无人纠正）。约定：松手回弹用**显式** `startRebound(from)`（stopRebound → 锁回起点 `translateY(from)` → `animate` 0.22s easeOut 回顶 → `onComplete` 写死 `translateY(0px)`）；`watch` 仅兜底（`from>0` 归零时跳过「播放页已关闭」与「新触摸会话已开始」两种场景，其余一律 `startRebound`）。`clearDragOffsetImmediate()`（stopRebound + DOM 写 0 + `dragOffsetY=0`）用于进度条手势、歌词点击、`onTouchStart` 残留兜底、`resetDragState`。**回弹动画与拖拽 `:style` 必须作用同一元素**（`.player-page__drag-layer` 内层，`ref="dragLayerRef"`），禁止外层 `player-page__overlay` 动画 + 内层绑定分离（松手瞬间会内层先归 0、外层再动画，产生抖动）。真机触摸序列可能被系统打断（通知栏下拉/多指/低端机丢事件）导致 `touchend/touchcancel` 不至：`window blur` + `visibilitychange` 时 `clearDragOnWindowHide` 兜底清零（onMounted/onUnmounted 成对注册/移除）。"}]
- **手机固定头部（08-16 椒盐式，player-page-fixed-header-swipe）**：手机（<768px）下歌名/艺术家渲染为 `.player-page__song-head--fixed`——`drag-layer` 内 `panels` 滑动容器之外的第一个子元素（drag-layer 为 flex column），左右切面板时头部不移动；顶部避让与 panel 一致 `calc(16px + safe-area-top) 24px 0`。info-panel 内 `.player-page__song-head--in-panel` 与 lyric-panel 的 `.player-page__lyric-header` 手机下 `display:none`（由固定头部承担，避免重复），横屏平板（≥768px 且宽>高）恢复 `display:block`、固定头部隐藏。`panels` 手机下 `height:auto + flex:1 1 auto + min-height:0` 占剩余空间（横屏平板 `width:100% + flex:1 1 auto` 占头部/底部条之间空间）。
- **断点机制（08-17-tablet-immersive-player 重构）**：平板判定**不再用 media query**，统一收敛到 JS `isTabletLayout = viewportWidth >= 768 && viewportHeight < viewportWidth`（横屏平板；竖屏平板 800x1280 保持手机式全屏）。容器挂双 class：`.player-page--tablet`（scoped）与 `.player-overlay--tablet`（全局）。所有平板样式用 `.player-page--tablet &`（scoped）/ `.player-overlay--tablet`（全局）后代选择器驱动，`@media (max-width: 767.98px)` 手机微调与 `@media (max-height: 720/520px)` 矮屏断点保留。竖屏平板宽度 ≥768 但 `height > width` → 手机式全屏（固定头部/五行歌词/内嵌控制/歌词 FAB 播放键全部恢复）。
- **横屏平板播放页布局（08-17-tablet-immersive-player）**：左右双栏（左 info-panel + 右 lyric-panel 各 50%）+ **底部全宽控制条 `.player-page__bottom-bar`**（`v-if="isTabletLayout"`，drag-layer 内 panels 之后，`flex: none`）：进度条+时间一行（全宽，padding 24px），按钮三段式（左组 repeat/shuffle + 中组 controls prev/play/next + 右组 queue/more，`justify-content: space-between`，中组居中于屏幕中心）。info-panel 内 progress/controls/mode-bar 包进 `.player-page__info-controls`（手机 `display: contents` 保持子项展开，平板 `display: none`）。info-inner 平板 `justify-content: center`（控制下移后封面垂直居中）。右栏 lyric-header 平板保持隐藏（左栏头部承担，全局 `.player-overlay--tablet .lyric-header` 规则）。
- **下滑关闭露顶不得白底**（#08-03-player-drag-white-bg）：宿主自定义下滑只 `translateY` `.player-overlay` **内层**（带 void/AMLL 背景），`k-popup` 面板本身不动（k-popup 无背景色默认透明，无旧 `.h-popup__panel` 白底问题）；露顶透过 backdrop（约 36% 黑）看底层页面。**禁止**给 `.player-overlay` 外层铺死沉浸 void 色（会挡住透底）。`QueuePage` 保持表面底，不得误伤。
- 歌词页浮动：左下翻译开关（**仅当行数据或 `lyricsTranslation` 有译文/音译时渲染**；默认开，隐藏时清空 `translatedLyric`/`romanLyric`，并用 `key` 重建 `LyricPlayer` 保证 AMLL 立即刷新）；右下播放/暂停仅 **非平板**（`<768px`）显示；浮动按钮需小尺寸、低视觉权重。沉浸页 **主控 / mode-bar / 歌词 FAB** 用 `k-button clear` + 白色字 class（默认 `text-white/80`、激活 `text-white`），按下态由 Konsta 自身 active 反馈（半透明白底）承担；语义 `.is-active` **仅**用于翻译 FAB 开态，**禁止** mode-bar 循环/随机绑 `.is-active`（模式只靠图标 + aria-label）。禁止只改字色、禁止 Ionic `--color`/`--background`（`08-03-lyric-fab-style-translation`）。竖屏/横屏交互后 chrome 同一套显隐；宽屏不隐藏整区，只藏播放键。
- **歌词解析 vs 翻译适配（AMLL 边界）**：
  - 主词格式解析**只**用 `@applemusic-like-lyrics/lyric`（`parseLrc` / `parseYrc` / `parseQrc` / `parseTTML` 等），禁止自研平行格式解析器。
  - `parseLrc` **不会**把同时间戳双行 plain LRC 自动收成主行+`translatedLyric`；独立 tlyric 也需业务挂载。
  - 展示前 `prepareLyricLinesForDisplay`（`mergeTranslation.ts`）仅做：挂 `lyricsTranslation`（tlyric）→ 合并仍无译的双语主行 → 供翻译开关使用；**不是**第二套 LRC 引擎。
  - 已有非空 `translatedLyric`（TTML/库结果或已 attach）禁止再双行合并或覆盖。
- **双语主译判定**：合并双语主行时不得只靠文件顺序。若一对中一行是 Han、另一行是非 Han（Latin / 假名 / Hangul 等），**非 Han 为主行**、Han 为 `translatedLyric`。合并结果 `endTime = max(两行 endTime)`，避免活跃窗口过短导致高亮只闪一下。关翻译后主行须仍为原文。
- **双语合并的两条路径（08-17-fix-lyric-translation-offset 后）**：
  - **同时间戳相邻配对**（既有）：译文与原文同一时间戳（或 ≤50ms）时按相邻行合并；仅文字体系明显不同才合并，禁止吞同时间的两句独立歌词（同脚本相邻行绝不合并）。
  - **交替结构感知配对**（新增，修复译文整体后移一行）：当主词 LRC 中译文行时间戳打在**下一句原文时间戳**上（`C_i.startTime ≈ E_{i+1}.startTime`）时，按**文件顺序固定窗口**（0,1)(2,3)... 把译文并回前一行原文（修复后每句译文落到自己的原文行，结尾不再多出孤立中文行）。
  - 交替结构判定必须**整歌成立**才激活：配对数 ≥2、覆盖率 ≥60%（`SHIFTED_PAIR_MIN_RATIO`）、且满足**结构一致性**（所有配对的主行同属一种脚本、译文侧为互补脚本）+ **时间定义属性**（译文行时间戳贴近下一窗口原文行时间戳，`SHIFTED_CROSS_WINDOW_TOLERANCE_MS`）；任何一条不满足（如垫词行打断交替、零星中文行、错位窗口）则整体回退到「同时间戳相邻配对」，保持不误吞。
  - shifted 合并后会再跑一遍同时间戳相邻配对（收敛混合形态文件中残留的对齐对），两端译文守卫保证不重复合并。
- **tlyric 挂载（网易 yrc 场景，08-17-fix-lyric-translation-offset）**：先按 80ms 容差逐行就近挂载；挂载率不足且呈系统性时间偏移（yrc 行时间与 lrc/tlyric 普遍偏差数百 ms）时，退化为序列感知顺序对齐（双指针按行序匹配，宽容差 ≤2000ms，匹配率 ≥60%）。**边界防误判**：若「首条待挂行无可用 stamp」且「末 stamp 无主行承接」同时成立（tlyric 自身整句错移一段），判定为结构错移，放弃回退，避免在 tlyric 层复现同类错位。第一遍已消费的 stamp 从回退池精确剔除，避免同一译文重复挂两条主行。
- 快速切歌：`playSong` 用 generation 丢弃被 supersede 的 play 回写；native 状态有当前曲时必须 `currentSongId` 精确匹配；loading 期间忽略无关 paused/stopped（#28/#29）。
- **运行时缓存必须有界**：AMLL TTML 命中缓存、AMLL/在线封面/在线文本负缓存使用共享轻量 LRU helper，默认最多保留 256 个近期条目；命中刷新近期顺序，负缓存原有 TTL 与 reset 语义保持。不得新增无限增长的按 songId Map。
- **App 根级监听器必须卸载**：`App.vue` 注册 Capacitor `backButton` listener 时必须保存异步返回的 handle，并在组件卸载时调用 `remove()`；若 handle 在卸载后才 resolve，应立即移除，避免重复回调。

## 在线歌词匹配（多源回退）

- `playSong(song)` 无论歌曲是否已有本地歌词，均异步调用 `src/features/lyrics` 的 `matchOnlineLyrics`；匹配不得阻塞音频播放。
- 在线串行优先级：**amll TTML** → 平台歌词（kw→tx→wy→kg→mg）→ **LRCLIB** LRC → 本地内嵌/同名 `.lrc` → 空态。匹配期间已有本地词先展示本地 LRC。
- 平台源内 **逐字优先**（AMLL 原生仅 yrc/qrc；krc/mrc/lyricx 不做）：
  - **QQ**：`GetPlayLyricInfo` 加密串 → `decryptQrcHex` → 提取 `LyricContent` → `format: 'qrc'`；失败降级 `fcg_query_lyric_new` LRC。
  - **网易**：eapi `/api/song/lyric/v1` 优先 `yrc`，否则公开 API / LRC；UI 用 `parseYrc` / `parseQrc` / `parseLrc` / `parseTTML`。
- LRCLIB：`/api/get`（含 duration）→ `/api/search`；**仅** `syncedLyrics`；合规 `User-Agent`；MVP 不用 plainLyrics。
- `lyricsFormat`：`ttml | lrc | yrc | qrc | null`（运行时）；库内 `SongItem.lyricsFormat` 可选同枚举。
- **按质量写回曲库**：在线命中后若优于库内则 `upsertSong` 写 `lyrics` + `lyricsSource: 'online'` + `lyricsFormat`。质量序 `ttml|yrc|qrc` > `lrc` > 空；同级不覆盖。旧数据无 format 有词视为 `lrc`。
- 播放初始化用库内 `lyrics`+`lyricsFormat`；有词仍跑在线匹配以便升级。
- `httpGetText`：CapacitorHttp 返回的 4xx/5xx 直接抛出，**不**再回退 fetch（避免双请求与 404 误走实网）。
- `PlayerState.lyricsFormat` 为 `'lrc' | 'ttml' | 'yrc' | 'qrc' | null`，决定 `PlayerPage` 解析器；`onlineLyricsStatus` 为 `'idle' | 'matching' | 'ready' | 'miss' | 'error'`。
- amll 索引与 TTML 正文请求缓存仍可在进程内存；**匹配成功的歌词正文**可按质量持久化到 `SongItem`（见上）。不得把整库 amll 索引打包进 APK。
- 切歌递增歌词匹配 token；异步结果仅在 token 与 `currentSong.id` 同时匹配时写入，禁止上一首歌词串到当前歌曲。
- CDN/解析失败静默回退，不弹播放错误，也不得改变音频播放状态。

## 在线封面匹配（仅补缺）

- 触发：`playSong` 成功后 `scanSongMetadata` 结束（或本地已扫描仍无封面）时，若当前曲仍无安全 `coverUri`，异步调用 `src/features/cover` 匹配；不得阻塞播放。
- 源顺序：iTunes Search → kw 酷我 → tx QQ → wy 网易云 → kg 酷狗 → mg 咪咕（国内段对齐 any-listen sources；结构预留更多源）；任一源返回可用 HTTP(S) 图 URL 即停止。
- 落盘：`AudioPlayerPlugin.cacheRemoteCover` 下载到 `cache/covers/{sha}.jpg`，返回 `file://`；`upsertSong` 仅写安全 URI。
- **禁止**把 `data:` / base64 / 裸远程 URL 写入 `muses:songs`。
- **禁止**覆盖已有安全 `coverUri`（本地内嵌/扫描结果优先）。
- 切歌递增 `onlineCoverToken`；结果仅在 token 与 `currentSong.id` 同时匹配时写回并 `syncDisplayStateFromSong` + 媒体会话封面。
- 失败/无命中/超时静默；进程内负缓存避免同曲短时间反复请求；不影响歌词与播放状态机。

## 在线文本元信息（artist / album 仅补缺；弱 title 可写）

- 触发：与封面并列，`scanSongMetadata` 结束后若 `artist`/`album` 仍空，或 `title` 为弱标签，异步调用 `src/features/metadata`；不得阻塞播放。
- **弱 title**：`normalizeText(title) === normalizeText(getTitleFromPath(path))`（扫描无内嵌标题时的文件名兜底）。
- 字段：artist/album **仅补空**；弱 title 且 hit.title 与当前 title **相关**（normalize 相等或互相包含）时可写 title；**强 title 禁止覆盖**。
- 写回：`upsertSong` 顶层 title + tags；已有非空 artist/album 不覆盖。
- 源顺序：kw → tx → wy → kg → mg（对齐 any-listen 国内段；不含 iTunes）；与封面并行、独立 token（`onlineTextToken`）与负缓存。
- 切歌递增 token；结果仅在 token 与当前曲 id 匹配时写回并 `syncDisplayStateFromSong` + 媒体会话文本。
- 失败静默；不影响封面、歌词与播放状态机。

## 编辑页云端强制搜（`searchEditCloudMeta`，`08-05-edit-cloud-meta-api`）

- 入口模块：`src/features/editMeta`（`searchEditCloudMeta` + types）；**仅**服务编辑 sheet 主动获取，与播放静默补空 **分离**。
- **强制搜**：忽略 `needsOnlineTextMeta` / 已有封面跳过 / `userEditedFields` 门闸；不读播放负缓存；不写 `playerState` / `upsertSong` / 封面落盘。
- **返回**：`text` / `cover` / `lyrics` 三维，各含 `status`（`ok | no-match | network | aborted`）、`items[]`、`defaultIndex`（排序后最优为 0）。
- **多候选 MVP**：文本 = 各 text provider 现有 `search`（每源 1 条）合并去重 + `scoreTextHit` 排序；封面 = 各 cover provider **不** first-stop，URL 去重；歌词 = amll + fallback **全收集**（不因首命中停止）；每维上限默认 8。
- **封面**：API 只返回 `remoteUrl`；落盘 `cacheRemoteCover` 属 UI 应用阶段。
- **取消**：可选 `AbortSignal`；循环检查 `aborted`。
- **禁止**把编辑路径语义并入 `matchOnlineTextMeta` / `matchOnlineCoverRemote` / `matchOnlineLyrics`（播放 first-hit / 仅补空保持不变）。
- UI 勾选应用 / 表单写入见 `08-05-edit-cloud-meta-ui`：
  - 编辑 sheet **仅手动**「从云端获取」；打开不自动搜。
  - 结果先预览 + 可换候选；**分字段勾选**后「应用到表单」才写 `editForm` / 封面。
  - 封面应用必须 `cacheRemoteCover` → 安全 URI；禁止 http/data 入库。
  - 歌词应用主词 text + `editLyricsFormat`；保存 dirty 时写入 `lyricsFormat`（手改歌词输入则重置为 `lrc`）。
  - 关 sheet / 切歌：`AbortController` + 清空云端预览状态；封面异步回写须再检当前曲 id。

## 用户手改字段保护（`userEditedFields`，`08-04-player-more-edit-song`）

- `SongItem.userEditedFields?: Array<'title'|'artist'|'album'|'cover'|'lyrics'|'replayGain'>`；旧数据缺省 `[]`。
- **写库**：`updateSongUserEdit(songId, patch)` 更新字段并 **union** 进保护集；清空歌词/封面/RG 仍标记保护，避免扫描写回旧值。
- **自动 upsert**：`upsertSong` 在更新已存在曲时调用 `applyTagsRespectingUserEdits`，受保护字段保留库内用户值；`userEditedFields` 不被自动路径清除。
- **额外门闸**：`shouldPersistOnlineLyrics` 若 `lyrics` 手改则 false；`matchOnlineLyricsForSong` 手改时**不请求**在线且不覆盖运行时；在线封面/预取封面若 `cover` 手改则跳过（网络返回与下载后须再检）；`needsOnlineTextMeta` / `mergeTextMetaFillEmpty` 尊重 title/artist/album 保护。
- **保存顺序（D4）**：`saveCurrentSongUserEdit` → 写库 → `syncDisplayStateFromSong`（歌词可 `forceLyrics`）→ 按 patch 递增对应 `lyricsMatchToken` / `onlineCoverToken` / `onlineTextToken` 作废在途补缺 → RG 变化时 `setVolume` → **再** 尽力 `writeMetadata`（local SAF / WebDAV PUT）。文件失败**不回滚库**，Toast 区分「已保存」与「已更新曲库，写入音频文件失败」。
- **封面选图**：`LocalLibrary.cacheCoverBytes` 落 `cache/covers` 安全 `file://`；**禁止** data/http 入 `muses:songs`。
- **原生写标签**：`AudioMetadataWriter` + `LocalLibraryPlugin.writeMetadata` / `WebDavPlugin.writeMetadata`；错误码 `not_writable` / `unsupported_format` / `put_failed` / `missingUri` 等。

## 响度均衡（ReplayGain 轻量，#46）

- **仅标签**：扫描/懒读元数据时解析 track ReplayGain（`REPLAYGAIN_TRACK_GAIN` 等）写入 `SongItem.replayGainTrackDb`（dB）；可选次级 `R128_TRACK_GAIN`（Q7.8 整数按 ÷256 换算，无法落入合理 dB 区间则丢弃）。**禁止**全曲库 EBU R128 / ffmpeg 测响度，也禁止无标签时写假增益（0 或臆造值）。用户在编辑页显式输入的 dB（含 0）视为有效手改并写入 `userEditedFields`。
- **播放应用**：`controller` 根据 `loudnessNormalizeEnabled`（`muses:player-config`，**默认 true**）与 `replayGainTrackDb` 计算 `volume = clamp(10^((db + LOUDNESS_PREAMP_DB)/20), 0.1, 1.0)`，其中 **`LOUDNESS_PREAMP_DB = 6`**（#51 听感补偿；纯 RG 目标偏安静）。经 `PlayOptions.volume` 传入 `AudioPlayerNative.play`；`native.ts` 在 preload/play 后调用 `NativeAudio.setVolume`。
- **能力边界**：插件 volume 上限 1.0，**无法**把过静曲放大超过系统满幅；关开关或无标签 → volume 1.0。
- **切歌 / stop**：每首重算 volume；禁止串曲增益。懒扫补到 RG 后若仍在 playing/paused，须对当前曲 `setVolume`。
- **设置**：`SettingsPage`「音量均衡」toggle；`setLoudnessNormalizeEnabled` 持久化并立即对当前曲重设 volume。
- 纯函数：`src/features/player/loudness.ts`（`parseReplayGainDb` / `dbToPlaybackVolume`）。

## 约束与禁止模式

- **队列解析必须线性化**：从 ID 队列解析歌曲时，单次解析只加载一次 `loadSongs()`，构建 `songId -> SongItem` Map 后按队列顺序读取；禁止对每个队列项调用 `Array.find`。缺失歌曲跳过，重复 songId 保持曲库首条记录语义；不得引入跨窗口曲库缓存。
- **禁止**在除 `native.ts` 之外的任何文件直接调用 `NativeAudio.*` 或 `MediaSession.*`。
- **禁止**全库离线 loudness 扫描 / 无标签时写假 ReplayGain。
- **禁止**同时使用多个 notification provider（native-audio 的 showNotification 和 media-session 的通知只能开一个；当前我们只使用 media-session）。
- **禁止**在 `native.ts` 中搞双向依赖（目前 `mediaSession.ts` 和 `native.ts` 是解耦的；`mediaSession.ts` 仅 import `AudioPlayerBridge` 用于封面转换桥接）。
- **禁止**修改 `node_modules/@capgo/*` 源码（我们只修复了 manifest 中 `MediaButtonReceiver` 的缺失，这是 app 侧修正，不是插件修改）。
- **禁止**对任意 `finished`/`complete` 无条件 `advanceToNext`；必须经过 seek 保护窗 + 接近自然结尾校验。
- **禁止**在缓冲已知时把 seek 目标落到 `bufferedPosition` 之外（进度条与歌词均须拒绝或 clamp 到已缓冲终点）。
- **禁止**WebDAV 播放增长中的本地 `.partial` / 未完成 `file://` 文件；未完整缓存时必须使用远程 URL + Basic Auth headers 直链播放。
- **禁止**把 `prepareWebDavAudioFile` / 渐进下载作为播放路径；完整缓存命中才允许 `file://` 本地播放。
- **禁止**缓冲未知时画假缓冲条；播放页进度使用 `ion-range`，不再自绘缓冲色条层，也不再注入 `--buffered` UI 变量。
- **禁止**仅依赖 Capgo `currentTime` 事件驱动 UI 进度：playing 时必须有 `getCurrentTime` 轮询兜底，避免 timer 停转后条与时间冻结（#47）。
- **禁止**在无用户进度条手势时因 `ion-range` 的 programmatic `ionInput` 写入 `seekPreviewPosition`，否则会盖住 `playerState.position` 导致填充不前进（#47）。
- **冷启动播放会话**（`muses:playback-session`，#49）：存 `currentSongId` + `position`；`initializePlayer` 在原生无活跃曲时恢复为 **paused** 展示（不自动 play）；`stopPlayback` 清除 session；用户点播放走 `resumePlayback` → 必要时 `play` + `seek`。playing 中 position 节流写盘。
- **禁止**冷启动自动出声；**禁止** `applyNativeState(idle)` 在「仅 UI 恢复」窗口冲掉已恢复的 `currentSong`/session。
- **`resumePlayback` 路径分流（#52）**：仅 `restoredSessionUiOnly === true`（冷启动仅 UI、原生无 asset）才允许整曲 `playSongInternal`（+ 可选 seek）；普通 pause 后必须 `AudioPlayerNative.resume()`。resume 失败才回退 play+seek。**禁止**把任意 `paused` 都当成无 asset 而 unload 重播。
- **`applyNativeState` idle/stopped 清空守卫（#52）**：默认**不得**因 native `idle`/`stopped` 清空 `currentSong`；仅显式 `stopPlayback` 将 `allowNativeClearCurrentSong` 置 true 时允许清空（`stopPlayback` 自身也会同步清空）。陈旧 unload/重载 stopped 必须整段忽略（含 status），避免「UI 无曲 + 音频仍在播」或 status 被冲成 stopped。
- **禁止**预取密码进入 player state / localStorage / 日志；预取失败不得影响当前播放。
- **禁止**下一首元信息预取写当前 `playerState` 或复用/递增当前曲 `lyricsMatchToken` / `onlineCoverToken` / `onlineTextToken`；须独立 `metadataPrefetchToken`，只 upsert 曲库。
- **禁止**对非 WebDAV 下一首做元信息预取（与音频预取同范围，除非产品明确扩大）。
- **禁止**在播放 `matchOnline*` 内做「编辑强制搜 / 多候选全收集」；编辑用 `searchEditCloudMeta`，播放路径保持 first-hit / 仅补空。
- **禁止**编辑 API 内 `cacheRemoteCover` / 写库 / 写 `playerState`。
- **禁止**在线封面把 `data:` / base64 / 远程 URL 写入曲库；禁止覆盖已有安全封面；匹配失败不得影响播放。

---

## 测试要点

- 本地音源播放→通知出现 → 封面 / 标题 / 上一曲 / 下一曲 可用
- 无封面歌曲播放后在线匹配成功 → 本地 cache covers URI 写回且 UI/通知刷新；已有封面不请求；miss 时按 iTunes→kw→tx→wy→kg→mg 串行回退
- 无 artist/album 或弱 title 歌曲播放后在线匹配成功 → 空字段/弱 title 写回且 UI/通知文本刷新；强 title 与已有 artist/album 不覆盖
- WebDAV 无完整缓存→NativeAudio 使用远程 URL + Basic Auth headers，不调用 `prepareWebDavAudioFile`，`bufferedPosition` 保持 `null`
- WebDAV 完整缓存命中→`file://` 完整文件播放，`bufferedPosition = duration`，不带 Authorization headers
- 播放成功后预取下一首 WebDAV 音频 + 元信息写库（`peekNext` + `prefetchWebDavAudioFile` + `prefetchNextMetadata`）；本地下一首 / 单曲循环自身 / 空队列不预取
- 元信息预取进行中不得改写当前曲 playerState；token 过期不得写库
- 无 WebDAV 密码时仍可预取下一首在线元信息
- `peekNext` 与 `advanceToNext` 目标一致但不改 `currentIndex`
- partial URI 不得当作缓存命中；预取失败静默
- 暂停/停止→通知同步出成 `none` 状态
- 队列自动下一首→通知立即刷新为新歌曲
- 有封面 A → 有封面 B：最终带 artwork 的 `setMetadata` 使用 B
- 有封面 → 无封面：最终用占位 `data:` 覆盖，不残留 A
- 开播后懒扫描写入 `coverUri` 会再次 `setMetadata`
- 快速切歌时过期 token 丢弃旧封面回调
- seek 后立刻注入 finished → 不切歌、保留 currentSong
- 接近结尾的 finished → 仍自动下一曲
- 歌词点击 seek 与进度条 seek 共用同一保护逻辑
- `duration=0` 的 finished 不自动 advance
- 缓冲已知时拖到未缓冲区 → 不调用原生 seek
- 歌词点击未缓冲时间码 → 不 seek
- 本地 full buffer → 可全长 seek
- 切歌 / stop 后 `bufferedPosition` 重置为 null
- 缓冲增长单调合并；回退上报不得拉低
- 缓冲未知时 seek 仍按 duration clamp
- 有 ReplayGain 且开启音量均衡 → play 传入 `(rgDb + 6 dB preamp)` 换算并 clamp 的 volume（如 -6→约 1.0；-12→约 0.5；正 dB 仍最多 1.0）
- 无标签 / 关闭均衡 → volume 1.0
- 切歌后 volume 按新曲重算，不串曲
- 设置开关即时生效并写入 `muses:player-config`
- 有 session + 队列曲仍在 → 冷启动 `initializePlayer` 后 paused + 恢复 position；不调用 native play
- 点播放 → play + seek 到恢复进度
- stop → 清除 `muses:playback-session`；队列列表仍保留
- 曲不在队列/曲库 → 丢弃 session，无当前曲
- 两轮 pause/resume 后 `currentSong` 仍在；普通 resume 只调 `resume` 不二次 `play`（#52）
- 非显式 stop 的 stopped 事件不改 status、不清空 `currentSong`（#52）

---

## 常见错误

- **AMLL 歌词播完后全部失活变模糊**  
  根因：`PlayerPage.lyricRenderTime` 直接用 `position * 1000`，播完/暂停在末尾时超过最后一句歌词 endTime，AMLL 找不到活动行。  
  修复：钳制上限到最后一句 `endTime`（无 endTime 时 fallback `startTime`），最后一行保持完成高亮；无歌词时不钳制。

- **CarWith 连接时播完不切歌 / 媒体通知按钮失效（JS 冻结，08-18-carwith-bg-ctrl-fix）**  
  根因：CarWith 连接后手机 WebView 页面不可见，Chromium 冻结/深度节流 JS，complete 事件与媒体按钮命令（MediaSession callback → JS keepAlive handler）全部无人处理；JS 晚处理 complete 时又依赖滞后的 `state.position` 做自然结束判定，误判为伪 finished 而置 paused。
  修复（方案 A，不改 node_modules / manifest / 无原生直控）：  
  1. **WebView JS 保活**（`src/features/player/keepalive.ts`）：播放中常驻 gain=0 静音 Web Audio 轨（ConstantSource → gain 0），让隐藏页面携带「ongoing media」标签阻止 Chromium 冻结；仅 Android 且播放会话存在时运行；暂停/停止即停；任何异常静默降级；`muses:debug-keepalive=1` 开日志。
  2. **finished 判定语义变更**：complete 即自然播完（见第 7 点），不再依赖 position 佐证；仅 seek 保护窗内的 finished 才视为伪结束。  
  实测结论：预案在 CarWith 音频重定向下的 `isMusicActive()` 判定是否失真尚未确认（插件间无同步返回值通道，A 边界内无法换用 capgo isPlaying）；预案/心跳/对账保持为保活失效时的最终防线。

- **播放中拔出蓝牙耳机/有线耳机/断开 CarWith 车机不暂停**（08-18-bt-car-disconnect-pause）  
  根因：蓝牙耳机断开时的暂停/停止依赖系统音频焦点机制（capgo `OnAudioFocusChangeListener` 的 `AUDIOFOCUS_LOSS`→stop / `LOSS_TRANSIENT`→pause）；CarWith 车机断开时系统不发焦点变化，播放继续。  
  修复（`AudioPlayerPlugin.kt`）：原生注册 `AudioManager.registerAudioDeviceCallback`（API 23+，无需新权限），`onAudioDevicesRemoved` 过滤「输出且为破坏性类型」（蓝牙 A2DP/SCO、有线耳机、USB 音频、DOCK 底座）且 `jsExpectedPlaying==true` 时，500ms 去抖后调 capgo `pause`；**必须在 pause 前把 `jsExpectedPlaying=false`**，否则 pause 后 isMusicActive 转 false 会误触发 auto-next 预案自动播放下一首（静音输出继续"播"）。JS 状态同步零改动：capgo pause 发 `playbackState(paused)` → `native.ts` → controller 自动暂停。前端 `reportBridgePlaybackStatus` 需携带 `currentAssetId`（capgo pause 必须指定 asset）。纯判定逻辑抽成 companion 函数 `isDisruptiveDeviceRemoved` + JUnit 单测。

- **锁屏/后台时当前曲播完不自动切下一首（JS 冻结）**  
  根因：播放/切歌链路依赖 WebView JS；锁屏后 Chromium 对不可见 WebView 节流甚至冻结 JS，原生 complete 事件到达不了 JS，前端无兜底。  
  修复（方案 C，不改 node_modules/@capgo/*）：  
  1. **原生预案兜底**：`AudioPlayerPlugin` 新增 `setAutoNext`/`clearAutoNext`/`reportPlaybackStatus`；JS 每次起播成功注册「下一首预案」（`controller.registerAutoNextPlan`，含 assetPath/认证/volume/currentAssetId）；原生 1s 轮询 `AudioManager.isMusicActive()` + JS 上报的期望播放状态（`jsExpectedPlaying`），静音 2.5s 防抖后经 `Bridge.callPluginMethod("NativeAudio", ...)` 驱动 capgo 插件 preload/play；旧 asset 先播后卸；起播经 15s 验证窗口确认（`autoNextStarted`/`autoNextFailed` 事件）。  
  2. **JS 侧对账**：`syncUiToNativeSong`（`playSongInternal` 的 `nativeAlreadyPlaying` 模式，跳过原生 play 只同步 UI/媒体会话/新预案）+ `reconcileAfterBackground`（App.vue `appStateChange`/visibilitychange 回前台时：原生在播新曲→同步；原生已停→补切歌）。  
  3. **心跳兜底**：hidden 且 playing 时 1s 心跳 `getState()` 检原生状态（后台节流后约 1 次/分钟），complete 事件丢失时补切歌。  
  关键约束：切歌窗口（playSongInternal 调原生 play 前）必须先 `clearAutoNextPlan()`，否则原生兜底可能触发上一首的旧预案；预案/轮询全部 try/catch 静默，失败自动降级到心跳/对账。  
  另两个坑（check 阶段发现）：
  - **preload 遇 `already exists` 必须复用而非失败**：锁屏预案已 preload 的曲目，回前台自动切歌再次 preload 会 reject；若当作失败会误进恢复链跳曲且可能双声。处理：检测到 `already exists` 时复用 asset，`isPlaying` 为 true 则保留进度不重启。
  - **getState() 的 isPlaying 查询失败不能回退到 JS 缓存状态**：预案 unload 旧 asset 后查询失败，若 fallback 为 `currentStatus==='playing'` 会让回前台对账/心跳永远误判原生在播。应 fallback `false`（asset 不存在视为不在播）。

- **manifest 中缺少 MediaButtonReceiver 声明**：导致通知栏按钮显示但点击没反应  
  修复：在 `AndroidManifest.xml` 的 `<application>` 中添加形如  
  `<receiver android:name="androidx.media.session.MediaButtonReceiver" android:exported="true"> ... </receiver>`。
- **Tab 返回键调用 `App.exitApp()`**：Activity destroy → media-session unbind destroy → 播放中通知消失  
  修复：改用 `App.minimizeApp()`，仅退到后台。
- **`loading` 映射为 media-session `none`**：切歌时通知闪断/延迟  
  修复：loading 保持 active（`playing`），仅 stop/clear 时置 `none`。
- **`setMetadata` 同步等待封面 base64**：首帧通知被大图转换拖慢  
  修复：文字先上、封面后补。
- **`artwork: []` 切到无封面歌曲仍显示上一首封面**  
  修复：首帧与 clear 路径一律用 1×1 中性 JPEG `data:` 强制覆盖；prepare 失败也保留占位清空。
- **懒扫描补到封面后通知栏不更新**  
  修复：`syncDisplayStateFromSong` 检测 cover/title/artist/album 变化后调用 `syncMediaSessionSong`。
- **`file://` 缓存封面 `prepareArtworkDataUrl` 静默失败**  
  修复：原生侧 `file://` 优先 `FileInputStream`。
- **seek 到未缓冲区间后伪 finished 误切下一曲**  
  修复：源头限制 seek ≤ `bufferedPosition`；`seekPlayback` 成功后开启保护窗；`applyNativeState` 仅在非保护窗且接近自然结尾时 `handlePlaybackFinished`。
- **播放中进度条/时间不前进，seek 后仍冻结（#47）**  
  修复：`native.ts` playing 轮询 `getCurrentTime`；`PlayerPage` 仅在 `seekGestureLocked` 时写 seek preview，忽略 ion-range value 变化触发的伪 `ionInput`。
- **杀进程重开无当前曲 / 进度丢失（#49）**  
  修复：`session.ts` 持久化 songId+position；`initializePlayer` 恢复 paused UI；`resumePlayback` 仅在 `restoredSessionUiOnly` 时 play+seek；`stopPlayback` 清 session。
- **沉浸页二次暂停/播放后变空、迷你条「暂无播放歌曲」但声音仍在（#52）**  
  根因：`resumePlayback` 把任意 paused 都走 `playSongInternal` → unload 的 stopped 清空 `currentSong`。  
  修复：普通 resume 走 `AudioPlayerNative.resume()`；`allowNativeClearCurrentSong` 仅 `stopPlayback` 开启；非显式 stop 的 idle/stopped 整段忽略。
- **缓冲串曲 / 切歌后仍显示上一首缓冲条**  
  修复：`playSong` / `stopPlayback` / 播放失败均 `resetBufferState()`；继续调用原生 `cancelBufferSession`，清理旧 APK 或遗留会话启动的渐进下载。
- **没有 `npx cap sync android` 就部署**：前端代码改动不会反映到 APK  
  修复：每次前端改完后执行 `npm run build && npx cap copy android && cd android && ./gradlew :app:assembleDebug`。

---

## 6. 数据来源追踪（child1 + child4）

### 字段来源模型

每个 `SongItem` 的 title/artist/album/cover 字段都有一个来源标记 `metaSources[key]`：

```typescript
type FieldSource = 'embedded' | 'cloud' | 'manual'
```

| 来源 | 含义 | 写入场景 |
|------|------|----------|
| `embedded` | 值来自音频文件内置 tag | 扫描/懒扫/刮削写回文件成功 |
| `cloud` | 值来自在线补缺/刮削写回文件失败 | 在线补缺自动写库 |
| `manual` | 用户手改 | `updateSongUserEdit` |

### 来源读取

`getFieldSource(song, key)` 从 `metaSources` 读取来源；`manual` 由 `userEditedFields` 派生（不单独存储）。

### 存量兼容

旧 SongItem 无 `metaSources` → 读取时默认 `embedded`（保守默认）。`CURRENT_METADATA_VERSION` 从 3 升至 4，懒扫时自动触发重读补齐来源。

### 置信度分级

匹配质量分级（child4）定义 `MatchConfidence = 'high' | 'low'`：
- `classifyMatch`：歌词匹配（title exact+artist → high；contains+artist → high；其余 → low）
- `classifyTextMetaConfidence`：文本匹配（同理）
- `findBestMatch` 默认 `minConfidence='high'`：自动写库仅采纳高置信
- `shouldPersistOnlineLyrics` 新增 `confidence` 参数：`'low'` 仅可补空库

详见 `features-scrape.md` 的完整合约。
