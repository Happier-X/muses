# M2 设计：歌词与播放列表

> 前置：父任务 `../08-24-native-compose-rewrite/design.md`（模块结构、技术栈）；M1 `../08-24-native-m1-core-playback/`（脚手架与播放服务接口）。
> 本设计在 M1 交付的模块边界内扩展，不推翻既有架构。

## 1. 技术选型定案（2026-08-24，开发者决策：内嵌官方 AMLL）

| 项 | 选型 | 版本 | 备注 |
|---|---|---|---|
| 歌词+背景渲染 | WebView 内嵌官方 AMLL：`@applemusic-like-lyrics/core` + `@applemusic-like-lyrics/lyric`（备用） | 最新稳定 | Vite 打包进 APK assets；参考 Zeehan2005/AMLL-DroidMate 路线 |
| 前端构建 | Vite + vite-plugin-wasm + vite-plugin-top-level-await | — | 产物输出到 `feature:player` 的 androidAssets，Gradle 任务串联 |
| WebView 桥接 | Compose `AndroidView` 包 WebView + `WebViewAssetLoader` + `evaluateJavascript` | androidx.webkit | 参考 DroidMate：进度注入节流 100ms |
| 歌词解析 | `com.mocharealm.accompanist:lyrics-core`（仅解析） | 0.4.7+ | Kotlin 侧解析可单测；spike 已验证 API。映射为 AMLL LyricLine[] JSON 后注入 |
| 播放列表存储 | Room 关联表 | M1 已有 Room 基座 | 不用 songIds JSON，保证排序与外键清理 |
| 响度均衡 | ExoPlayer `player.volume` 线性增益 | 无新依赖 | 复刻 Web 层语义 |

**方案说明**：

- **一个 WebView 页面承担歌词+背景双职责**：页面内同时挂 AMLL `LyricPlayer` 与 `BackgroundRender`（PIXI WebGL），Compose 侧不再做 FluidBg 移植
- **桥接口**（DroidMate 验证过的模式）：
  - `window.updateLyrics(json)`：切歌时注入 LyricLine[]（含 words/translatedLyric/romanLyric）与封面 URL
  - 进度注入：`evaluateJavascript` **节流 100ms**（DroidMate 同款），或改用 addJavascriptInterface 高频回调（实现时二选一，先节流方案）
  - 封面：经 `WebViewAssetLoader` 以 https://appassets.androidplatform.net/assets/... 形式安全加载本地文件，避免 file:// 限制
- **accompanist lyrics-ui spike 已验证效果达标但弃用**（开发者仍选官方 AMLL 保真度）；保留为纯原生 fallback，spike 代码在 `C:\code\amll-lyrics-ui-spike`

**已验证的 lyrics-core API 事实**（spike，0.4.4）：

```kotlin
val parser = AutoParser()                    // 无 Builder；可传 PhoneticProvider
val synced: SyncedLyrics = parser.parse(ttmlText)
synced.lines: List<ISyncedLine>              // start/end/duration 均为 ms
// 映射目标：AMLL LyricLine { words: [{startTime,endTime,word}], startTime, endTime,
//                           translatedLyric, romanLyric }  （ms → AMLL 用 ms）
```

## 2. 模块与新增文件

```
native/
  frontend/amll-web/          新增：Vite 工程（AMLL core + LyricPlayer + BackgroundRender 页面）
                              build 产物 → feature:player/src/main/androidAssets/amll/
  core:model/        + Playlist、PlaylistWithSongs、LyricsData 领域模型
  core:data/         + PlaylistDao、PlaylistRepository、SettingsRepository 扩展（响度开关）
  core:media/        ~ PlayerConnection 扩展：positionMs Flow 细化；LoudnessController
                       （M1 已建 PlayerConnection；若缺字段在此补）
  feature:player/    + AmllWebView（AndroidView 包装 WebView + AssetLoader + JS 注入）、
                       AmllBridge（歌词 JSON 序列化/进度节流/生命周期 pause-resume）、播放页接入
  feature:playlist/  新增：PlaylistsPage、PlaylistDetailPage、AddToPlaylistSheet
  feature:library/   ~ 歌曲/专辑列表长按菜单加「加入播放列表」
```

依赖方向不变：`feature:*` → `core:data`(接口) / `core:model`。lyrics-core 与 webkit 依赖只出现在 `feature:player`；frontend 工程独立 package.json，Gradle 通过 exec 任务调 npm build（CI 缓存 node_modules）。

## 3. 数据流与契约

### 3.1 歌词链路（零网络）

```
SongEntity(lyrics, lyricsFormat, lyricsSource)     ← M1 扫描/未来 M3 刮削写入
  → LyricsParser.parse(raw): SyncedLyrics?          ← lyrics-core（Kotlin 侧，可单测）
  → AmllMapper.toAmlJson(lines): String             ← 映射 AMLL LyricLine[]，翻译/音译挂载
  → AmllWebView.evaluateJavascript("window.updateLyrics(<json>)")   ← 切歌时一次性注入
  → 定时器（100ms 节流）：window.updatePosition(ms)   ← 播放中持续注入；暂停即停发
  → WebView 内：LyricPlayer 渲染 + BackgroundRender 背景
```

- **翻译/音译**：优先消费 TTML 内嵌 translations；LRC 双语按现有 `mergeTranslation.ts` 语义简化合并（同时间轴行配对）。隐藏开关直接置空 translated/roman 字段后用 key 重建视图（复刻 Web 层 #25 语义）。
- **播完钳制**：Kotlin 侧发送 `min(positionMs, lines.last().end)`，规避 spec 记录的「播完全行失活模糊」问题
- **解析失败降级**：parse 返回 null → 注入空行数组，前端显示占位，BackgroundRender 照常渲染（spec：背景不得因无歌词卸载）

### 3.2 AMLL 背景生命周期治理（继承 spec 契约，原生等价实现）

| spec 契约（Web 层） | 原生实现（WebView 方案） |
|---|---|
| 有当前曲且有封面即渲染，不得因无歌词卸载 | AmllWebView 与歌词状态解耦，参数为 coverUri；无词时仍注入封面并跑背景 |
| 切后台/熄屏 pause 渲染循环 | Lifecycle ON_STOP 或播放页不可见 → evaluateJavascript("window.pauseRender()") / 恢复 → resumeRender()；WebView 缓存同进程，无需销毁重建（复刻 Web 层 v-if 教训：**禁止用销毁/重建控制暂停**） |
| 粘性封面 | PlayerViewModel 持 stickyCover: StateFlow<Uri?>，切歌新曲 coverUri 为空时沿用旧值，无当前曲才清空 |
| 有界缓存 | 封面文件复用 Coil 磁盘缓存；WebView 实例单例复用（App 级），禁止每首歌新建 |

### 3.3 播放列表

Room schema：

```
PlaylistEntity(id PK, name, createdAt, updatedAt)
PlaylistSongEntity(playlistId FK→PlaylistEntityonDelete=CASCADE, songId FK→SongEntity, position)
                     PK(playlistId, position)
```

- 入队播放：`PlaylistRepository.getSongs(id)` → `PlayerConnection.playQueue(songs)`
- 拖动排序：交换 position 后单事务批量 update
- 删除歌曲联动：SongEntity 外键 CASCADE 由 Room 保证（对齐 653e466 的失效清理语义）

### 3.4 响度均衡

```
gainDb = SongEntity.replayGainTrackDb ?: 0
linear = 10^((gainDb + preampDb) / 20)        // preampDb = +6，开启时
volume = linear.coerceIn(0.1, 1.0) * 用户音量   // clamp 语义复刻 Web 层
ExoPlayer.volume = volume                      // onMediaItemTransition 时重算
```

- LoudnessController 注册 `Player.Listener`，开关/切歌时重算；设置存 DataStore（`loudnessEnabled: Boolean`）
- 单元测试：Q7.8 ÷256、±30dB 边界、clamp 行为（移植 loudness.ts 的测试用例语义）

## 4. 对 M1 的依赖契约（M2 开工前置检查）

M2 实现开始前必须确认 M1 已提供（缺失则先小步补齐）：

1. `native/` 脚手架与上述模块骨架存在且可构建
2. `SongEntity` 含 `lyrics / lyricsFormat / replayGainTrackDb` 字段（M1 扫描器已能产出，见 AudioMetadataReader.kt 既有逻辑）
3. `PlayerConnection` 暴露 `positionMs: StateFlow<Long>` 与 `currentSong`
4. 播放页（基础形态）已挂载，有背景层插槽

## 5. 权衡与回滚

- **为何不用 accompanist lyrics-ui**：开发者最终选择官方 AMLL 观感保真 + 自带 BackgroundRender 免自研背景；代价是 WebView 内存（约 100MB 级）与桥接层维护，已列入风险。spike 已验证其效果达标，保留为纯原生 fallback（接口隔离：PlayerViewModel 只暴露领域模型，UI 层可整体替换）。
- **WebView 方案风险与缓解**：① 后台 CPU——沿用 spec 生命周期治理契约（pause/resume 显式控制）；② 内存——WebView App 级单例复用，播放页关闭不销毁；③ 桥接丢消息——切歌注入带 songId token，过期回调丢弃（复刻 Web 层 generation 模式）；④ WASM 兼容性——vite-plugin-wasm 处理，最低支持 API 29（DroidMate 同款 minSdk，高于我们 M1 的 24，需真机确认低版本表现或在低版本降级纯原生 fallback）
- **回滚点**：feature:playlist 与 feature:player 歌词改动相互独立，可分别回滚；Room 迁移向前追加，不修改 M1 已有表。

## 6. 测试策略

- 单测：解析器封装（TTML/LRC 样本）、AmllMapper JSON 映射、响度计算（含 Q7.8/clamp）、PlaylistRepository（内存 Room）
- Compose UI 测试：AddToPlaylistSheet 交互、无歌词空态
- 真机回归：后台 CPU（对比 spec 记录的 40-71% 基线应显著回落）、切歌粘性封面、长队列拖动排序、WebView 内存占用（`dumpsys meminfo`）
