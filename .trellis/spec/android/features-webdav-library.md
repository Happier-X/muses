# WebDAV 曲库链路 — 扫描/播放/凭据规范（08-25-webdav-source-scan）

> 适用于 仓库根 Android 工程 WebDAV 音源的扫描入库、播放、认证链路。Web 层 `src/features/library/scanner.ts` 为功能语义参照。

---

## 组件契约

| 组件 | 层 | 职责 | 关键签名 |
|---|---|---|---|
| WebDavLibraryScanner | core:media | BFS PROPFIND 纯发现 + 文件名建库（零下载），只产出不入库 | `suspend fun scan(source: Source): List<Song>`；`buildSidecarLyricsUrl(url)` 供播放懒扫描复用 |
| LocalLibraryScanner | core:media | MediaStore 本地扫描；`isSupportedAudio`/`stableSongId`/`TAGS_VERSION`/`FILENAME_TAGS_VERSION` 为两扫描器共用常量方法 | `suspend fun scan(source: Source? = null, readTags: Boolean = true): List<Song>`（`readTags=false` 时 `tagsVersion=FILENAME_TAGS_VERSION(0)` 进懒补充） |
| CoverCacheWriter | core:media | 封面落盘 cache/covers/<sha256>.jpg（两扫描器共用，禁止回退私有实现） | `fun write(context, cacheKey, bytes): String?` |
| WebDavAudioCache（接口） | core:webdav | 播放 LRU 缓存抽象（500MB）；磁盘实现 DiskWebDavAudioCache 经 @Binds 注入，测试注入内存 fake | `getCachedFile(url): File?` / `putToCache(url, file, eTag?, lastModified?)` |
| WebDavAuthRegistry | core:webdav | 内存凭据表 baseUrl→(user,pass)；最长前缀匹配出 Basic header | `suspend fun refresh()` / `fun authorizationHeader(url): String?` |

## 入库与删除契约

- 扫描器不写库：产出 `List<Song>` 后由 ViewModel/Worker 调 `SongRepository.replaceSourceSongs(sourceId, songs)`
- 标签回写是懒扫描唯一入库路径：`songRepository.upsert(带 tagsVersion=TAGS_VERSION 的 Song)`
- **删除音源必须同步清理歌曲**：`deleteSource` = deleteById + clearPassword + `songRepository.deleteSourceSongs(id)` + registry.refresh()（漏掉第三步会出现「源删了歌还在」）
- Song.path 对 WEBDAV 一律存完整 HTTP URL；id 用 `stableSongId(sourceId, url)`

## 播放契约（限流教训，勿回退）

- **WEBDAV 曲目播放 = 整文件入缓存后 file:// 播**（对齐旧版 getOrDownload）：未命中缓存的先经 `client.get` 单次 GET 进缓存再播。
- **播放契约（限流教训，勿回退）**：WEBDAV 曲目播放 = ExoPlayer 直连 WebDAV URL **流式播放 + `CacheDataSource` 边播边缓存**（用户决策 2026-08-27：保持流式，不做整文件下载）。曾因「恢复队列一次性 `prepare` 全列触发请求 → 打爆限流 → 429 全站」而禁止直连流播；现通过两处消除 burst：(1) 流播 `OkHttpDataSource` 走 `@StreamingOkHttp` client（只 auth 不限流，见 `WebDavModule.provideStreamingOkHttpClient`），与扫描/预取限流桶隔离，流播单连接读取不再被饿死/超时重试；(2) media3 1.11 无 `Player.setPreloadItems`（相邻预加载 API 在 1.13+），默认不预加载整队列；若实测 `setMediaItems(全量)` + `prepare()` 仍发全列请求，再改为「只 `prepare` 当前曲 + 下一首」分批加载 + `PlayerConnection` 维护 UI 队列副本（解耦 ExoPlayer 队列）。重复播放命中 `CacheDataSource` 本地缓存不发网络。文件名建库 durationMs=0 的 mp3/flac 由 ExoPlayer 流播时自行解析容器 ID3 tag，无需额外网络请求。
- **标签读取只在播放时懒扫描**（用户决策 2026-08-26：扫描期「读取音乐标签」功能已删除）：WebDAV 为文件名建库 `tagsVersion=0`；本地 `readTags=false` 时同为 `FILENAME_TAGS_VERSION(0)` 占位入库（标题取 MediaStore/文件名、封面空，等待懒补），`readTags=true` 才即时 `TAGS_VERSION(1)`。播放时 `PlaybackService` 对 `tagsVersion < TAGS_VERSION` 的歌曲（WebDAV/local 通用，local 的 `content://` 经 `AudioTagReader.copyContentUriToCache` 拷贝后解析）调 `lazyScanTags`——TagReader + sidecar .lrc + CoverCacheWriter → `songRepository.upsert` 回写（Room Flow 自动刷新列表）。幂等键：`tagsVersion=0`（FILENAME_TAGS_VERSION），懒扫描成功写 TAGS_VERSION；失败静默保持文件名歌下次重试。本地 `readTags` 开关保留（无网络成本，关时走同 WebDAV 的懒补充）。
- 认证统一走 OkHttpClient Interceptor + WebDavAuthRegistry；interceptor **不得覆盖请求已携带的 Authorization**（避免压掉 OkHttpWebDavClient.authenticate 的手动 header）。PlaybackService 禁止自建裸 OkHttpClient。
- 预取模式（PlayerConnection.prefetchScope）：串行下载、换队列 cancel 旧任务、单首失败回退 http URL 不阻塞队列、完成后 mainHandler.post 回主线程 setMediaItems（Media3 主线程铁律）。
- 凭据生命周期：源 save/update/delete 都必须调 `registry.refresh()`。
- **限流假设显式化（防 E 类隐含假设复发）**：禁止假设「4 rps 安全」——所有外发 HTTP（刮削 `ScrapeHttp`、播放 `WebDavClient`）必须经 `WebDavRateLimiter` 单例，阈值显式声明为 4 rps 且集中在 `WebDavModule`，测试注入 `nowMs` 避免虚拟时间漂移。**流播链路例外**：ExoPlayer 经 `CacheDataSource` + `OkHttpDataSource` 对流式 URL 的播放读取，走 `WebDavModule.provideStreamingOkHttpClient`（`@StreamingOkHttp`，只 auth 不限流）——流播是单连接串行持续读取、请求率远低于 CDN 阈值、不构成 burst，套 4 rps 反而饿死/超时重试叠加 429（用户决策 2026-08-27，任务 08-27-webdav-playback-429）。
- **播放/刮削共享限流（任务 08-27-webdav-playback-429）**：`WebDavRateLimiter`（`core:webdav` 单例 4 rps/250ms，`synchronized` 兼顾协程/阻塞链路）经 `WebDavModule` 单例提供，`ScrapeRateLimiter` 为其 `typealias` 复用；`WebDavClient` 全量方法前 `acquire()` + `OkHttpClient` 拦截器 `acquireBlocking()` 双层覆盖（已在协程层限流的请求打 `X-Muses-Rate-Limited` marker 跳过二次限流）；**`CacheDataSource` 流播链路已剥离限流**（见上「流播链路例外」），走 `@StreamingOkHttp` client，不再经 `acquireBlocking()`；429 时 `parseRetryAfterMs`（秒/HTTP-date，≤8s）退避重试 1 次，二次 429 抛 `IOException("http 429")` 并 `ErrorLogStore.log(WARN)`，上层归为可重试 `NETWORK`。

## 错误文案矩阵（对齐 Web）

| 场景 | 文案 |
|---|---|
| 密码缺失 | WebDAV 密码不存在，请重新添加该音源。 |
| 认证被拒 | WebDAV 认证失败（HTTP xxx）（来自 WebDavAuthException/WebDavRequestException） |
| 懒扫描读标签失败 | 静默保持文件名歌，下次播放重试（不阻塞播放） |
| 播放 429 限流 | `触发限流，稍后重试`（`PlaybackErrorCopy.RATE_LIMITED_RETRY`，白名单第 9 条），`Snackbar` 提供重试/关闭，`ErrorLogStore` 可查 `WARN WebDavClient http 429` |

## 测试锚点

- WebDavLibraryScannerTest：扩展名过滤+递归 / 扫描零下载 / sidecar URL 构造 / 密码缺失抛错且进度置终态 / 文件名建库 tagsVersion=0
- WebDavAuthRegistryTest：最长前缀匹配 / 无匹配 null / refresh 生效 / null user Basic 编码 / `'/'` 边界不误命中
- 挂起调用外包 catch 时必须前置 `catch (e: CancellationException) { throw e }`（两 scanner 与 PlayerConnection 预取均有示范）
- `WebDavRateLimiter` 精确时序单测需注入 `nowMs` 避免虚拟时间漂移；`WebDavClient` 429 分支需覆盖 `Retry-After` 秒/HTTP-date/无头/上限截断四态

## Wrong vs Correct

```kotlin
// CORRECT：ExoPlayer 直连 WebDAV URL 流式播放 + CacheDataSource 边播边缓存
// （setPreloadItems(false) 只加载当前曲；流播 client 不限流，与扫描/预取桶隔离）
MediaItem.Builder().setUri(song.path).build()
// 重复播放命中 CacheDataSource 本地缓存，不发网络；切歌时才加载下一首
```

## 场景：音频标签读取（jaudiotagger，任务 08-27-audio-tag-cache）

### 1. Scope / Trigger
- WebDAV 扫描仅文件名建库（tagsVersion=0），列表长期展示占位；需提供标签读取能力（标题/歌手/专辑/封面/歌词/时长），按用户决策仅在播放时懒扫描，不采用后台 Worker 批量补齐
- 触发：新增 jaudiotagger 标签读取能力（Range 部分下载）及跨层入库契约

### 2. Signatures
- `AudioTagReader @Singleton (Context, OkHttpClient)`：`fun readTags(source: String): AudioTags?` / `suspend fun readTagsSuspend(...)` / `fun readTagForUpdate(path, songId): TagUpdateData?` / `fun extractCover(source, songId): String?`（本地/WebDAV 通用，WebDAV 经 Range 部分下载；本地支持 `content://`（ContentResolver 拷贝）/ `file://` / 绝对路径四态）
- `SongDao.getUntaggedSongIds(): List<String>` 查询 `SELECT id FROM songs WHERE tagsVersion < 1`（供懒扫描判定，未直接批量处理）
- `gradle/libs.versions.toml`：`jaudiotagger = "3.0.1"`，`jaudiotagger = { group="net.jthink", name="jaudiotagger", version.ref="jaudiotagger" }`

### 3. Contracts
- 输入：`source` 为本地绝对路径/`content://`/`file://` 或 WebDAV HTTP(S) URL；`songId` 用于封面落盘 `cacheDir/audio_tags/cover_${songId}.jpg`
- 输出：`AudioTags(title?, artist?, album?, lyrics?, cover?: ByteArray, durationMs)`；`TagUpdateData(title?, artist?, album?, lyrics?, coverUri?, durationMs)`
- 标签映射：`FieldKey.TITLE/ARTIST/ALBUM/LYRICS` + `firstArtwork.binaryData` + `audioHeader.trackLength*1000`
- WebDAV 下载：优先 Range `bytes=0-65535`，解析探测无有效标题/歌手/专辑时扩大至 `bytes=0-262143`；Range 失败回退全量 GET；均经 OkHttp 拦截器自动注入 Authorization（来自 WebDavAuthRegistry）与限流（X-Muses-Rate-Limited 避重）
- 本地 `content://`：经 `ContentResolver.openInputStream` 全量拷贝到 `cacheDir/audio_tags/content_${hash}.tmp` 再解析（命中复用，避免切歌重复拷贝）；`file://` 去前缀后 File，直链绝对路径同理
- 封面落盘：返回绝对路径字符串，供 `SongEntity.coverUri` 直接写入
- 回写契约（播放时懒扫描）：`song.copy(title=tagTitle?:原标题, artist?:原, albumTitle=tagAlbum?:原, lyrics?:原, coverUri?:原, durationMs=max, durationSec=max, tagsVersion=1)` 经 `songDao.upsert`；读取失败保持 tagsVersion=0 下次播放重试，不批量后台处理

### 4. Validation & Error Matrix
- `AudioFileIO.read(File)` 抛异常 → `readTags` 返回 null，不崩溃
- WebDAV 401/403 → OkHttp 拦截器未命中凭据或凭据失效，下载抛异常 → 懒扫描静默失败，保持文件名歌，下次播放重试
- 本地 `content://` 打开失败（权限缺失/文件已删）→ 抛 `FileNotFoundException` → `readTags` 返回 null，懒扫描保持 `tagsVersion=0` 下次重试，不阻塞播放
- Range 不支持（200 而非 206）→ 视为失败触发全量下载
- `coverUri` 写入前 `cover?.let` 为 null → 不落盘，保持原 coverUri
- `title` 空白 → 回退原 `song.title`（文件名）避免空标题

### 5. Good/Base/Bad Cases
- Good：WebDAV mp3 带 ID3v2 → 播放时 Range 64KB 命中，标题/歌手/封面一次补齐，tagsVersion 置 1，列表实时刷新
- Base：无标签纯文件名文件 → read 成功但字段全空，判无更新，不污染原数据，下次播放仍为文件名
- Bad：WebDAV URL 失效/密码错误 → 下载抛异常，懒扫描静默失败，不阻塞播放，下次播放重试

### 6. Tests Required
- 单测：`AudioTagReader` 本地 mp3/flac 解析标题/封面/歌词字段正确（mock File）
- 单测：Range 成功 206 与回退 200 分支（MockWebServer 返回不同 code）
- 集成：播放时懒扫描对 1 条未标记歌曲执行 `readTagForUpdate`，断言 songDao 中 tagsVersion 变 1 且 coverUri 非空
- 手工：播放 WebDAV 歌曲后观察列表标题由文件名变为真实标签

### 7. Wrong vs Correct
#### Wrong
```kotlin
// 直接在扫描期对每个 WebDAV 文件逐个全量下载读标签 → 瞬时 N 次 GET → 429 限流打爆
files.forEach { item ->
  val tmp = File(cacheDir, item.name)
  webDavClient.get(item.url, tmp) // 全量
  val tags = AudioFileIO.read(tmp).tag
}
// 或后台 Worker 批量补齐 → 扫描后立刻 N 次请求仍可能触发限流，且与播放时懒扫描重复
```
#### Correct
```kotlin
// 扫描期零下载文件名建库；播放时按需 Range 64KB 读标签，认证经 OkHttp 拦截器注入
val songs = files.map { filenameSong(sourceId, it) } // tagsVersion=0
songRepository.replaceSourceSongs(sourceId, songs)
// 播放时：AudioTagReader.readTagForUpdate(song.path, song.id) -> songDao.upsert(tagsVersion=1)
```

## 刮削后懒扫描与展示的守卫（09-03-fix-rescrape-stale-metadata）

### 1. Scope / Trigger
- 触发：重刮削后 `tagsVersion` 仍为 0（写库不抬升），下一次播放的懒扫描无条件用文件旧标签覆盖库内刮削新值；同时 UI `useMeta = tagsVersion<1` 分支优先旧文件标签，导致迷你条/列表当前行仍显示旧标题。

### 2. Signatures
- `PlaybackService.persistenceListener.onEvents(EVENT_MEDIA_ITEM_TRANSITION)` 懒扫描分支：`val ms = entity.toDomain().metaSources`
- `AudioTagReader.invalidate(source: String)`：清 `tagCache[source]` + `getCacheFile(url).delete()` / `content_${hash}_*` 前缀文件
- `WritebackOrchestrator(audioTagCacheInvalidator: ((String)->Unit)?)` 写文件 `ok==true` 时 `invalidator?.invoke(song.path)`
- UI：`MusesApp.MainViewModel.nowPlaying` / `SongsPage` 行内 `if (song.metaSources?.field != null) song.field else metaField ?: song.field`

### 3. Contracts
- 懒扫描覆盖前逐字段守卫：`ms?.title/artist/album/cover != null` 跳过该字段，`lyricsSource != null && lyrics 非空` 跳过歌词；`durationMs` 始终 `max`
- 无实际更新也抬升 `tagsVersion=1`（避免已刮削歌曲重复 Range 请求）；读失败（`tagData==null` / 抛异常）保持 `tagsVersion=0` 下次重试
- `invalidate` 仅在文件写成功分支调用，失败不失效；异常静默
- UI 对已刮削字段强制库值优先，未标记字段保持原 `tagsVersion<1 → mediaMetadata` 回退链路

### 4. Validation & Error Matrix
- `metaTitle=SCRAPE, 文件标题旧值` → 懒扫描后库标题仍为刮削新值
- `metaCover=EMBEDDED, 文件无有效标签` → 懒扫描仍抬升为 1，不重复探测
- `metaSources==null, tagsVersion=0, 文件标题新值` → 正常覆盖并抬升（回归）
- `invalidate("http://...")` → `audio_<hash>_*` 文件被删 + 内存 miss
- `invalidate("content://...")` → `content_<hash>_*` 前缀文件被删

### 5. Good/Base/Bad Cases
- Good：WebDAV `FILE_FAILED` 刮削（`metaTitle=SCRAPE`）→ 播放切歌守卫跳过标题覆盖 → 迷你条/列表均显示库内新标题
- Base：本地刮削含封面成功 → `invalidate(path)` → 下次 `readTags` 重新解析新封面字节
- Bad：无守卫时刮削后播放→文件旧标签覆盖库新值→重刮削成果丢失；无 `invalidate` 时成功写入后仍读旧缓存

### 6. Tests Required
- 懒扫描守卫：构造 `SongEntity(metaTitle=embedded, tagsVersion=0)` + 旧文件标签，断言切歌后库标题不变
- `AudioTagReader`：写入缓存→`invalidate`→断言 `getCacheFile` 不存在且内存 miss
- `WritebackOrchestrator`：文件成功断言 `invalidator` 调用 1 次，失败断言不调用
- 手测：WebDAV `FILE_FAILED` 歌曲刮削→播放→迷你条/列表/沉浸页标题一致为新值

### 7. Wrong vs Correct
#### Wrong
```kotlin
// 无守卫：直接用文件标签覆盖
val domain = entity.toDomain().copy(
  title = tagData.title ?: entity.title, // 覆盖刮削新值
  tagsVersion = 1
)
// UI 无守卫：tagsVersion<1 永远优先旧 mediaMetadata
title = if (tagsVersion < 1) metaTitle ?: song.title else song.title
```
#### Correct
```kotlin
// 逐字段守卫，未标记才覆盖
val resolvedTitle = if (ms?.title != null) domainBefore.title
  else tagData.title?.takeIf { it.isNotBlank() } ?: entity.title
// UI 按已刮削优先库值
val title = when {
  song.metaTitle != null -> song.title
  song.tagsVersion < 1 -> metaTitle ?: song.title
  else -> song.title
}
// 写回成功后失效缓存
if (fileResult.ok) audioTagCacheInvalidator?.invoke(song.path)
```

## 构建注意

- 多 flavor 项目（muses/miui）：装包验证一律 `assembleMusesDebug`（产物 app-muses-debug.apk）；裸 `assembleDebug` 打的是无 flavor 旧 variant 包，装机后表现为"改动没生效"
