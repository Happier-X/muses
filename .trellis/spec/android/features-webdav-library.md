# WebDAV 曲库链路 — 扫描/播放/凭据规范（08-25-webdav-source-scan）

> 适用于 `native/` 工程 WebDAV 音源的扫描入库、播放、认证链路。Web 层 `src/features/library/scanner.ts` 为功能语义参照。

---

## 组件契约

| 组件 | 层 | 职责 | 关键签名 |
|---|---|---|---|
| WebDavLibraryScanner | core:media | BFS PROPFIND 发现 + 可选读标签，只产出不入库 | `suspend fun scan(source: Source, readTags: Boolean): List<Song>` |
| LocalLibraryScanner | core:media | MediaStore 本地扫描；`isSupportedAudio`/`stableSongId`/`TAGS_VERSION` 为两扫描器共用常量方法 | `suspend fun scan(source: Source? = null, readTags: Boolean = true): List<Song>` |
| CoverCacheWriter | core:media | 封面落盘 cache/covers/<sha256>.jpg（两扫描器共用，禁止回退私有实现） | `fun write(context, cacheKey, bytes): String?` |
| WebDavAudioCache（接口） | core:webdav | 播放 LRU 缓存抽象（500MB）；磁盘实现 DiskWebDavAudioCache 经 @Binds 注入，测试注入内存 fake | `getCachedFile(url): File?` / `putToCache(url, file, eTag?, lastModified?)` |
| WebDavAuthRegistry | core:webdav | 内存凭据表 baseUrl→(user,pass)；最长前缀匹配出 Basic header | `suspend fun refresh()` / `fun authorizationHeader(url): String?` |

## 入库与删除契约

- 扫描器不写库：产出 `List<Song>` 后由 ViewModel/Worker 调 `SongRepository.replaceSourceSongs(sourceId, songs)`
- **删除音源必须同步清理歌曲**：`deleteSource` = deleteById + clearPassword + `songRepository.deleteSourceSongs(id)` + registry.refresh()（漏掉第三步会出现「源删了歌还在」）
- Song.path 对 WEBDAV 一律存完整 HTTP URL；id 用 `stableSongId(sourceId, url)`

## 播放契约（限流教训，勿回退）

- **WEBDAV 曲目播放 = 整文件入缓存后 file:// 播**（对齐旧版 getOrDownload）：未命中缓存的先经 `client.get` 单次 GET 进缓存再播。
- **禁止**让 ExoPlayer 直接对 WebDAV URL 流播无时长元数据（readTags=false 扫描产物 durationMs=0）的 mp3/flac——ExoPlayer 会发探测性 Range 分段请求（单首可达十余个），叠加失败恢复链跳歌重试形成请求风暴，触发网关（Cloudflare）429 全站限流。
- 认证统一走 OkHttpClient Interceptor + WebDavAuthRegistry；interceptor **不得覆盖请求已携带的 Authorization**（避免压掉 OkHttpWebDavClient.authenticate 的手动 header）。PlaybackService 禁止自建裸 OkHttpClient。
- 预取模式（PlayerConnection.prefetchScope）：串行下载、换队列 cancel 旧任务、单首失败回退 http URL 不阻塞队列、完成后 mainHandler.post 回主线程 setMediaItems（Media3 主线程铁律）。
- 凭据生命周期：源 save/update/delete 及引导页保存四处都必须调 `registry.refresh()`。

## 错误文案矩阵（对齐 Web）

| 场景 | 文案 |
|---|---|
| 密码缺失 | WebDAV 密码不存在，请重新添加该音源。 |
| 认证被拒 | WebDAV 认证失败（HTTP xxx）（来自 WebDavAuthException/WebDavRequestException） |
| 单文件读标签失败 | 静默降级为文件名建歌，不中断整体扫描 |

## 测试锚点

- WebDavLibraryScannerTest：扩展名过滤+递归 / readTags=false 零下载 / 读标签降级不中断 / 缓存命中零下载 / 密码缺失抛错且进度置终态
- WebDavAuthRegistryTest：最长前缀匹配 / 无匹配 null / refresh 生效 / null user Basic 编码 / `'/'` 边界不误命中
- 挂起调用外包 catch 时必须前置 `catch (e: CancellationException) { throw e }`（两 scanner 与 PlayerConnection 预取均有示范）

## Wrong vs Correct

```kotlin
// WRONG：ExoPlayer 直接流播 WebDAV URL（Range 探测风暴 → 429）
MediaItem.Builder().setUri(song.path).build()

// CORRECT：整文件入缓存后本地播
val uri = webDavCache.getCachedFile(song.path)?.let { Uri.fromFile(it) } ?: run {
    ensureCached(song.path)          // 单次认证 GET
    Uri.fromFile(webDavCache.getCachedFile(song.path)!!) // 失败时回退 song.path
}
```

## 构建注意

- 多 flavor 项目（muses/miui）：装包验证一律 `assembleMusesDebug`（产物 app-muses-debug.apk）；裸 `assembleDebug` 打的是无 flavor 旧 variant 包，装机后表现为"改动没生效"
