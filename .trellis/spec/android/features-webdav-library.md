# WebDAV 曲库链路 — 扫描/播放/凭据规范（08-25-webdav-source-scan）

> 适用于 仓库根 Android 工程 WebDAV 音源的扫描入库、播放、认证链路。Web 层 `src/features/library/scanner.ts` 为功能语义参照。

---

## 组件契约

| 组件 | 层 | 职责 | 关键签名 |
|---|---|---|---|
| WebDavLibraryScanner | core:media | BFS PROPFIND 纯发现 + 文件名建库（零下载），只产出不入库 | `suspend fun scan(source: Source): List<Song>`；`buildSidecarLyricsUrl(url)` 供播放懒扫描复用 |
| LocalLibraryScanner | core:media | MediaStore 本地扫描；`isSupportedAudio`/`stableSongId`/`TAGS_VERSION` 为两扫描器共用常量方法 | `suspend fun scan(source: Source? = null, readTags: Boolean = true): List<Song>` |
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
- **标签读取只在播放时懒扫描**（用户决策 2026-08-26：扫描期「读取音乐标签」功能已删除）：PlayerConnection 在当前曲入缓存后调 `lazyScanTags`——TagReader + sidecar .lrc + CoverCacheWriter → `songRepository.upsert` 回写（Room Flow 自动刷新列表）。幂等键：文件名建库 tagsVersion=0（FILENAME_TAGS_VERSION），懒扫描成功写 TAGS_VERSION；失败静默保持文件名歌下次重试。本地源扫描仍保留 readTags 开关（无网络成本）。
- 认证统一走 OkHttpClient Interceptor + WebDavAuthRegistry；interceptor **不得覆盖请求已携带的 Authorization**（避免压掉 OkHttpWebDavClient.authenticate 的手动 header）。PlaybackService 禁止自建裸 OkHttpClient。
- 预取模式（PlayerConnection.prefetchScope）：串行下载、换队列 cancel 旧任务、单首失败回退 http URL 不阻塞队列、完成后 mainHandler.post 回主线程 setMediaItems（Media3 主线程铁律）。
- 凭据生命周期：源 save/update/delete 及引导页保存四处都必须调 `registry.refresh()`。
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

## 构建注意

- 多 flavor 项目（muses/miui）：装包验证一律 `assembleMusesDebug`（产物 app-muses-debug.apk）；裸 `assembleDebug` 打的是无 flavor 旧 variant 包，装机后表现为"改动没生效"
