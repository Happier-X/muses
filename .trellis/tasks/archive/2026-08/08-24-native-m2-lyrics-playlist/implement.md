# M2 执行清单

> 前置门槛：见 design.md §4「对 M1 的依赖契约」。M1 未合入前，仅允许执行阶段 0（不依赖 native/ 工程的准备项）。

## 阶段 0：前置准备（可立即开始）

- [x] 确认 M1 进度：`native/` 脚手架已合入 main（minSdk=26，非 PRD 所写 24）
- [x] 核对 `SongEntity` 字段：**缺** lyrics / lyricsFormat / replayGainTrackDb / coverUri → 已列入阶段 1 首个任务
- [x] 搭建 `native/frontend/amll-web/` Vite 工程：构建通过，四个 window 接口就绪，产物在 feature:player androidAssets/amll/（Gradle exec 串联待办，见 testdata/README.md）
- [x] 准备测试样本：2 首 TTML、1 首双语 LRC（`.trellis/tasks/.../testdata/`，无歌词用例不需文件）
- [x] 在 `gradle/libs.versions.toml` 登记 `lyrics-core`（0.4.7，仅解析用）；已编译验证（实际 API 差异在阶段 1 写 LyricsParser 时确认）
- [ ] minSdk 决策点：实际为 26；WASM 兼容需真机验证（阶段 1 回归时确认）

**验证**：`cd native && ./gradlew :app:assembleDebug` 仍绿

## 阶段 1：歌词链路（feature:player + core）

- [x] `LyricsParser` 封装：raw + format → `SyncedLyrics?`，解析失败/空行集返回 null 不抛异常（0.4.7 API 与 spike 一致：`AutoParser()`；TTML 解析为自实现无 javax.xml 依赖，Android 安全）
- [x] `AmllMapper`：SyncedLyrics → AMLL LyricLine[] JSON（KaraokeLine.syllables→words、SyncedLine 整行单 word+translation 直挂、Accompaniment 行 isBG）+ 单测（手写 JSON 序列化避免新依赖；字段名与 AMLL core 0.5.2 契约一致）
- [x] `AmllWebView`：AndroidView 包装 WebView + WebViewAssetLoader + 切歌注入（songId token 防过期回调）+ ~100ms 节流进度注入 + Lifecycle pause/resume 桥接（2026-08-24 完成；另加 `/cache/` PathHandler 把 file:// 封面映射为 appassets https 地址，规避混合内容拦截）
- [x] PlayerViewModel：歌词 StateFlow（无词/解析失败也发空行数组载荷，背景照常渲染）+ positionMs 钳制（min(end)，VM 侧 100ms 轮询节流）+ stickyCover（SongDao 反查 Room）+ translationEnabled 开关重建 payload（复刻 Web 层 #25 语义）（2026-08-24 完成；SongRepository 无 getById，直接注入 DatabaseModule 已提供的 SongDao，未改 core:data）
- [x] 播放页装配：AmllWebView 作底层背景层（替换原 AsyncImage 渐变背景，无词同样显示背景），左下翻译 FAB 仅 hasTranslation 时渲染、白字低视觉权重、激活态高亮（2026-08-24 完成；不改既有控制逻辑）

**验证**：真机播放含 TTML 内嵌歌词的歌曲 → 卡拉OK染色正常、背景流体渲染正常；切后台 `dumpsys cpuinfo` 观察 CPU 回落；`dumpsys meminfo` 看 WebView 内存
**回滚点**：本阶段全部改动限于 feature:player/core 扩展，revert 单分支即可

### 阶段 1 前半收尾记录（2026-08-24 晚）

- 新增文件：`feature/player/src/main/kotlin/com/muses/player/feature/player/lyric/AmllWebView.kt`（含 `coverUriToAppAssetsUrl` helper 与 CacheDirPathHandler）；扩展 `PlayerViewModel.kt` / `PlayerScreen.kt`
- webkit 依赖：libs.versions.toml 登记 androidxWebkit=1.14.0（仅追加不改既有行）；feature:player 加 implementation
- 进度注入节流实现取舍：节流放 VM 的 lyricPosition 轮询（100ms 读 currentPosition 并钳制末句 end），WebView 侧 combine(isPlaying) 仅播放中发射——比在 Composable 内再 throttle 更可单测
- `验证`：`:feature:player:testDebugUnitTest :app:assembleDebug` 全绿（2026-08-24）；真机回归项见下方遗留 TODO
- 遗留真机回归项：后台 CPU 回落、切歌粘性封面、播完钳制表现、WebView 内存（dumpsys meminfo）、WASM 在 minSdk=26 设备表现

## 阶段 2：播放列表（feature:playlist）

- [x] Room：PlaylistEntity + PlaylistSongEntity（CASCADE 外键、position 排序）+ DAO + 迁移（M1 会话已建 MusesDatabase v1，故注册进现有库 → v1→2 迁移；songId FK+CASCADE 直接建成，M1 SongEntity 已落地）
- [x] PlaylistRepository：CRUD、增删歌、拖动排序（事务）、按 id 取歌曲列表（排序暂为上移/下移按钮，拖拽后置）
- [x] PlaylistsPage / PlaylistDetailPage（Salt 风格）
- [x] 播放列表导航入口接入 app 抽屉 + 详情路由（check 发现缺失后补齐）
- [x] AddToPlaylistSheet 接入 library 歌曲长按菜单（combinedClickable + ModalBottomSheet；feature→feature 依赖例外已注释说明）
- [x] 播放列表整体入队 → PlaylistRepository.getSongs(id) → PlayerConnection.play(first, songs)；PlaylistDetailPage TopAppBar 加「播放全部」按钮

**验证**：单测（内存 Room CRUD/排序/CASCADE 8 用例全绿）；UI 手工回归清单待真机
**回滚点**：Room 迁移独立追加，回滚需 drop 新表迁移脚本

### 阶段 2 check 记录（2026-08-24）

- check 顺手修复两个并行开发造成的编译/DI blocker：`WebDavClient.kt` StandardCharsets.forName→Charset.forName；`DatabaseModule/Repositories.kt` 补 SongDao/SourceDao/CryptoEngine 绑定（需知会 M1 会话避免重复修补）

### 并行修复追加记录（2026-08-24 晚，阶段 1 前半实现期间）

M1 在途代码又有三处编译阻塞，已最小修复（需知会 M1 会话）：

- `PlayerViewModel.kt` QueueViewModel：注解错位 `@HiltViewModel constructor` → `@Inject constructor`
- `PlayerScreen.kt` / `library/Screens.kt`：Coil 2.x 包名 `coil.compose.AsyncImage` → Coil 3 的 `coil3.compose.AsyncImage`（依赖是 io.coil-kt.coil3:coil-compose 3.5.0，与父任务定版一致）
- `feature/player/build.gradle.kts` 加了 accompanist.lyrics.core / coil.compose / junit 三条依赖
- [x] PlaylistRepository 层单测（去重追加/updatedAt touch/顺序/CASCADE，4 用例全绿）；AddToPlaylistSheet Compose UI 测试留阶段 4

## 阶段 3：响度均衡

- [x] LoudnessCalculator 移植（Q7.8 ÷256、±30dB 校验、+6dB preamp、clamp [0.1,1.0]）+ 单测全绿（边界语义：超 ±30 走 Q7.8 兜底换算，仍越界才丢弃——与 Web 层一致）
- [x] LoudnessController：服务侧 ExoPlayer 上挂 Player.Listener onMediaItemTransition 重算 volume（MediaController 无 volume 能力，必须服务侧）；开关变化即时重设；onDestroy 先停 controller 再释放 player
- [x] DB v3：songs.replayGainTrackDb 列（MIGRATION_2_3 向前追加）；TagReader 解析接入扫描器持久化；Song 领域模型/映射器同步
- [x] SettingsRepository.loudnessEnabled（默认 false，DataStore 持久化）；设置页 UI 入口留 M3（M1 尚无 SettingsPage，登记遗留 TODO）

**验证**：单测覆盖 loudness.ts 全部边界用例语义；真机 A/B 开关听感
**回滚点**：独立功能开关，默认关，可直接停用

## 阶段 4：收尾验收

- [ ] PRD Acceptance Criteria 逐项核对
- [ ] `cd native && ./gradlew lint testDebugUnitTest :app:assembleDebug` 全绿
- [ ] 真机回归：后台 CPU 对比 spec 基线、切歌粘性封面、播完钳制、长队列排序
- [ ] spec 更新候选：accompanist API 事实与升级注意、背景生命周期原生实现模式 → `.trellis/spec/android/`

### 阶段 2+3 收尾 check 记录（2026-08-24，提交前全量检查）

- 核过：MIGRATION_2_3 与 schemas/…/3.json 一致（`REAL DEFAULT NULL` 在 PRAGMA table_info 中 dflt_value=NULL，与实体无 defaultValue 的期望 schema 匹配，旧库升级安全）；getSongs 走 @Transaction DAO + first() 快照一致，playAll 空列表早退；library→playlist feature 例外未泄漏 Room 类型到 library 编译期（sheet 公开 API 仅 List<String>）；PlaybackService onDestroy 先停 loudness/取消 scope 再 release player，@AndroidEntryPoint 在 MediaSessionService 上无已知坑（super.onCreate 先注入）
- should-fix 已修：LoudnessController.applyForCurrentItem 改单飞写入（applyJob 取消在途查询，快速连点切歌不再乱序覆盖 volume；stop() 同步 cancel，防复位 1.0 后被旧任务覆盖）；enabled 加 @Volatile 保证跨线程可见性
- nit 未改（可后续顺手）：PlaylistRepository.getSongs 返回类型用全限定名而非 import；Screens.kt 调 AddToPlaylistSheet 用全限定名而非 import；PlayerViewModel 歌词进度轮询空闲时仍 10Hz delay（VM 作用域内，开销可忽略）；重扫 TagReaderResult.empty 时 replayGainTrackDb/lyrics 会被 null 覆盖（沿袭 M1 既有语义，非本次回归）
- 门禁：`./gradlew lint testDebugUnitTest :app:assembleDebug` 全绿（含修复后复跑 :core:media 测试 + assembleDebug）
