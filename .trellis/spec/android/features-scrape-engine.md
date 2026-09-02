# 刮削与元数据引擎（M3 数据层）

> 任务 08-25-native-m3-scrape-engine。以 Web 层 `src/features/{scrape,metadata,cover,editMeta}` 源码为规格书翻译的纯 Kotlin 数据层，全部落在 core/scrape（+ core:model/core:data/core:media 增量）。UI 接线归后续任务。

## 模块与分层

```
core:scrape
├── text/      五源文本匹配链 kw→tx→wy→kg→mg（TextMetaMatcher）+ 置信度 + 负缓存
├── cover/     六源封面链 iTunes→kw→tx→wy→kg→mg（CoverMatcher）
├── writeback/ 写回编排五步 + 回滚 journal（DataStore）+ 失败文案映射
├── queue/     待刮削队列 / 历史（滚动 200）/ 可疑检测
├── editmeta/  编辑页强制云搜三维编排（文本/封面/歌词）
├── http/      ScrapeHttp：非 2xx 抛 IOException("http <code>") 不重试
└── di/        ScrapeModule（Hilt 装配，不接线 UI）
```

## 关键契约

- **Web=规格书**：算法常量逐一翻译并注释来源 .ts 文件（TTL 45min、负缓存容量 256、journal/历史上限 200、maxCandidates 默认 8）
- **存储替换**：localStorage → DataStore Preferences JSON snapshot（key：`scrape_rollback_journal` / `scrape_queue` / `scrape_history`），schema 带 version，解码宽松回退空表；编解码集中在 `writeback/WritebackJson.kt`（手工 JsonElement，未引 @Serializable 插件——AGP 9 内置 Kotlin 兼容性待验证）
- **写回五步**：快照→写文件（本地并行/WebDAV 串行）→写库（metaSources 标记 embedded/scrape）→逐行 success/file-failed/failed→撤销仅恢复库旧值
- **Room v4**：songs 表新增 `lyricsFormat/lyricsSource/metaTitle/metaArtist/metaAlbum/metaCover` 六列（MIGRATION_3_4）；Song 领域模型对应扩展
- **歌词维度边界**：editmeta 只做编排（去重 key=`source\1format\1text[0..120]`、ttml/yrc/qrc 优先粗排），具体歌词 provider 与 AMLL 聚合通过 `LyricsSearchPort` 注入，本任务不接线

## UI 接线契约（08-26-m3-scrape-metadata）

- **feature:scrape**：ScrapeScreen 四态机（queue/matching/preview/result）+ ScrapeViewModel 编排
  TextMetaMatcher+CoverMatcher+WritebackOrchestrator；ScrapeQueueAccessViewModel 供跨页面入队
- **写回安全红线**：预览候选 `checked = false` 默认全不选；「写回选中」按钮 enabled 绑定 any{checked}
- **歌曲页入口**：经 `onEnqueueScrape` 回调注入（feature:library 不直接依赖 core:scrape）；
  MultiselectBottomBar 与 ⋮ 菜单两处入口
- **EditMetaSheet 宿主在 MusesApp 层**：播放页 WebView「更多」键 → 桥动作 openEditMeta → 回调弹全局
  BottomSheet；song 为 null 时搜索/应用均 disabled
- **自动补缺**：`auto_scrape_enabled` DataStore 开关默认关；音源页扫描成功后
  `getUntaggedSongIds()`(tagsVersion<1) 入队。ScanWorker 路径因 core:scrape→core:media 循环依赖不接，
  只保留音源页扫描入口
- **LrclibProvider 需全局 Hilt 绑定**：LyricsModule @Provides（此前仅手动构造无绑定，UI 接线后暴露）
- **协程红线**：matcher/search 外包 catch 必须前置 rethrow CancellationException

## 已知缺口（接线 UI 前必须解决）

1. **WebDAV username 未持久化**：M1 Source 模型无 username 字段，`WebDavAudioTagFileWriter` 认证用户名暂传空串——需先补 username 存储
2. 刮削触发时机接线（入库后自动补缺调度）、ScrapePage 等页面归后续任务

## 踩坑记录

- **WebDAV 写回 URL 双重前缀**：`WebDavLibraryScanner` 存 `path = item.url` 完整 URL，`WebDavAudioTagFileWriter` 若再 `buildWebDavUrl(serverUrl, song.path)` 会得 `serverUrl/https%3A...` 404。修复为按 `song.path` 形态分流：以 `serverUrl` 开头则抽后缀重编码，`http(s)://` 按自身 `scheme+host` 重建，否则按相对路径（`SongFileWriters.kt`）。
- **临时文件扩展名**：`File.createTempFile(..., ".tmp")` 致 `TagWriter` 报 `No Reader for .tmp`，需保留原文件真实后缀（取 `song.path` 最后段 `.ext`）再创建临时文件。
- **临时目录**：`ScrapeModule` 仅 `provide` 时 `mkdirs`，系统清 `cacheDir` 后 `createTempFile` 失败，需每次写入前 `mkdirs`。
- **JVM 单测 Log 桩**：`android.util.Log` 在 JVM 单测为 `Stub!` 抛异常，`Writeback` 侧需 `try/catch` 回退 `println`（`safeLog`）。
- `URLEncoder.encode(String, Charset)` 重载需 API 33（minSdk 26）：统一用 `text/provider/KwProvider.kt` 的 `urlEncode`（charset 名重载 + `+→%20` 对齐 encodeURIComponent）
- `ScrapeHttp.getJson` 返回 `JsonElement`（非 JsonObject）：取字段须先 `asObjectOrNull()` 或用 `path(...)` 下钻，不能直接 `[key]`
- itunes 封面放大正则带**前导斜杠** `/\d+x\d+([a-z]*)\./i`（漏掉会产生 `//600x600` 双斜杠）
- MockWebServer 测硬编码域名 provider：OkHttp interceptor 把 host 重写到 loopback（见 CoverProviderParseTest/TextMetaMatcherTest 的 httpFor）
- **jaudiotagger 封面写入 ImageIO 崩溃（09-02）**：`ArtworkFactory.getNew()` 默认 `isAndroid=false` 返回 `StandardArtwork`，其 `setImageFromData()/getImage()` 强依赖 `javax.imageio.ImageIO`/`java.awt`（Android 不存在），FLAC `FlacTag.createField()` 必调 `setImageFromData()` 触发 `NoClassDefFoundError`；`TagWriter.write()` 仅 `catch(Exception)` 漏掉 `Error`。修复：`MusesApplication.onCreate()` 显式 `setAndroid(true)` + `TagWriter` 改用 `AndroidSafeArtwork`（反射 `BitmapFactory.decodeByteArray` 取宽高，失败 0×0 并 `return true`）+ `catch(Throwable)` 兜底

## 限流与 429 退避（任务 08-27-scrape-throttle-429）

### 1. Scope / Trigger
- Trigger：刮削 `ScrapeHttp` 原为裸 OkHttp 透传，`TextMetaMatcher`（5 源）与 `CoverMatcher`（6 源）逐 provider 串行 `http.get` 无间隔，队列 10 首 ≈ 50+ 请求瞬发，必触发上游 429。需可执行限流+退避契约。

### 2. Signatures
- `ScrapeRateLimiter(intervalMs: Long = 250L, nowMs: ()->Long)`：`suspend fun acquire()`，`Mutex` 保护 `nextAvailableMs`，`delay(wait)` 非阻塞；`Unlimited = ScrapeRateLimiter(0)` 测试用
- `ScrapeHttp(client: OkHttpClient, rateLimiter: ScrapeRateLimiter)`：`suspend fun getText/getJson/getBytes(url, headers): T` 共享 `executeWithRetry`；`companion MAX_RETRY_AFTER_MS=8000L, DEFAULT_429_DELAY_MS=1000L, fun parseRetryAfterMs(value: String?): Long?`
- `ScrapeModule`：`@Singleton ScrapeRateLimiter.default()` 注入 `ScrapeHttp` 与 `HttpCoverBytesFetcher`（跨文本/封面/封面字节共享）
- `TextMetaMatcher.negativeCache: NegativeCache` / `CoverMatcher.negativeCache: NegativeCache` 由 `internal` 提升为 `public val`，新增 `fun invalidateNegativeCache(songId: String)` / `NegativeCache.remove(songId)` 供单曲重试
- `feature:scrape/ScrapeViewModel`：`retrySingle/throttledIds` 失效负缓存后重跑单曲，`matching` 态 `throttleMessage = "等待限流恢复…"`，`preview/result` 行 `触发限流，稍后重试`

### 3. Contracts
- Request：`ScrapeHttp` 每次请求前 `rateLimiter.acquire()`（0 间隔不限流）；`Retry-After` 解析：1) 秒数 `toLong*1000` 2) HTTP-date `RFC_1123_DATE_TIME` 差值，失败回退 `DEFAULT_429_DELAY_MS`
- Response：非 429 非 2xx `throw IOException("http <code>")`；429 首次 `delay(min(computed,8000))` 后重试 1 次，二次 429 `throw IOException("http 429")`
- 上层归类：`TextMetaMatcher`/`CoverMatcher` 将 `IOException("http 429")` 归为 `NETWORK`（`OnlineTextMatchFailReason.NETWORK` / `OnlineCoverMatchFailReason.NETWORK`），`NO_MATCH` 才写 `NegativeCache`，`NETWORK` 不写；单曲重试前 `remove` 对应缓存
- UI：`queueTitles: Map<songId,title>` 透传用于 `throttledIds` 行优先显示标题回退 `take(8)`；限流提示 2s 后自动清除（单一 Job 管理防竞态）

### 4. Validation & Error Matrix
- `Retry-After: "120"` -> `120000L`
- `Retry-After: "Wed, 21 Oct 2015 07:28:00 GMT"` -> `date - now` 差值（≥0）
- `Retry-After: "invalid"` / null / 空白 -> `null` -> `DEFAULT_429_DELAY_MS`
- `Retry-After: "100"` (100s) -> `min(100000,8000)=8000` 截断
- 首次 429 -> delay 后重试
- 二次 429 -> `IOException("http 429")` -> 上层 `NETWORK`
- 非 429 4xx/5xx -> 直接 `IOException("http <code>")` 不重试
- `acquire()` 期间 `CancellationException` 原样重抛，不吞取消

### 5. Good/Base/Bad Cases
- Good：队列 20 首连续刮削，4 rps 节流下约 250ms 间隔，偶发单 Provider 429 后 1s 退避重试成功，进度显示 `等待限流恢复…` 后继续
- Base：单首双链均 `NETWORK`，result 行显示 `触发限流可单独重试`，重试前失效负缓存
- Bad：裸直通无限流，10 首瞬发 50+ 请求，稳定 429 且无文案、重试风暴

### 6. Tests Required
- `ScrapeRateLimiterTest`：4 rps 下 `acquire()` 虚拟时间精确断言 `0/250/500`，1 rps 变体 `0/100/200`，`Unlimited` 不延迟
- `ScrapeHttp429Test`：秒数解析、HTTP-date 解析、无效头回退、首次 429 重试成功、二次 429 抛错、非 429 不重试、Retry-After 上限截断、Cancellation 透传
- `TextMetaMatcherTest` / `CoverProviderParseTest`：429 归 `NETWORK` 不写负缓存验证（存量 119 tests 回归）
- UI 手测：MuMu 20 首队列 `matching` 进度可读，`preview/result` 限流文案出现，单曲重试入口可用

### 7. Wrong vs Correct
#### Wrong
```kotlin
// 裸直通，无限流无退避，二次 429 风暴
class ScrapeHttp(private val client: OkHttpClient) {
  suspend fun getText(url: String) = client.newCall(Request.Builder().url(url).build()).execute().let {
    if (!it.isSuccessful) throw IOException("http ${it.code}")
    it.body!!.string()
  }
}
// 匹配器内部直接抛错，未区分 NETWORK/NO_MATCH，负缓存误写
```
#### Correct
```kotlin
class ScrapeRateLimiter(private val intervalMs: Long = 250L, private val nowMs: ()->Long) {
  private val mutex = Mutex()
  private var nextAvailableMs = 0L
  suspend fun acquire() { val wait = mutex.withLock { /* 计算 wait */ }; if (wait>0) delay(wait) }
}
class ScrapeHttp(private val client: OkHttpClient, private val rateLimiter: ScrapeRateLimiter) {
  suspend fun getText(url: String) = executeWithRetry(url) { res -> res.body!!.string() }
  private suspend fun <T> executeWithRetry(url: String, onSuccess: (Response)->T): T {
    rateLimiter.acquire()
    var attempt=0
    while(true){ val res = client.newCall(buildRequest(url)).execute()
      if(res.code!=429){ if(!res.isSuccessful) throw IOException("http ${res.code}"); return onSuccess(res) }
      val delayMs = parseRetryAfterMs(res.header("Retry-After"))?.let{ min(it,8000)} ?: 1000
      res.close(); if(attempt>=1) throw IOException("http 429"); delay(delayMs); rateLimiter.acquire(); attempt++
    }
  }
}
// TextMetaMatcher: catch (e: IOException) -> Fail(NETWORK)，仅 NO_MATCH 写 NegativeCache
```

## 封面写入与 jaudiotagger Android 适配（09-02 fix-flac-scrape-imageio-crash）

### 1. Scope / Trigger
- Trigger：刮削回传含封面写入 FLAC/OGG 时 `StandardArtwork.getImage()` 调 `javax.imageio.ImageIO` 在 Android 运行时 `NoClassDefFoundError` 崩溃；`AndroidArtwork.setImageFromData()` 直接抛 `UnsupportedOperationException` 亦导致 FLAC 失败；`TagWriter.write()` 仅 `catch(Exception)` 漏 `Error`。
- 影响：`core:media/metadata/TagWriter.kt` + `app/MusesApplication.kt`，覆盖本地与 WebDAV 两条写回链路

### 2. Signatures
- `MusesApplication.onCreate()`：`TagOptionSingleton.getInstance().setAndroid(true)`（try-catch 兜底，不阻断启动）
- `TagWriter.write(file: File, request: TagWriteRequest): WriteResult`：`catch(Throwable)`，`CancellationException`/`InterruptedException` 原样重抛，其余折叠 `failure("write_failed", msg)`
- `TagWriter.createArtwork(bytes: ByteArray): Artwork`：返回 `AndroidSafeArtwork`，不走 `ArtworkFactory.getNew()`
- `AndroidSafeArtwork : Artwork`：持有 `binaryData/mimeType/description/pictureType(3)/width/height/isLinked/imageUrl`；`setImageFromData(): Boolean` 反射 `BitmapFactory.decodeByteArray(..., inJustDecodeBounds=true)` 取宽高，失败 0×0 并 `return true`；`getImage()` 抛 `UnsupportedOperationException`；`setFromFile()` 抛异常；`setFromMetadataBlockDataPicture()` 按 `isImageUrl` 分流复制

### 3. Contracts
- Request：`TagWriteRequest(coverBytes: ByteArray?, clearCover: Boolean, ...)`；`coverBytes.isNotEmpty()` 才写封面，先 `deleteArtworkField()` 再 `addField(artwork)`
- Response：`WriteResult(ok, code, message)`；`ok=true` 表示 `AudioFileIO.write` 成功；异常一律 `ok=false code=write_failed`
- 初始化契约：进程启动必 `setAndroid(true)`，否则 `ArtworkFactory` 仍可能返回 `StandardArtwork`；`TagWriter` 已不依赖工厂，双保险
- 封面为 `null/empty` → 不写；`clearCover=true` → `deleteArtworkField()`

### 4. Validation & Error Matrix
- `NoClassDefFoundError: javax/imageio/ImageIO` -> `write_failed` 不崩溃（Throwable 兜底）
- `UnsupportedOperationException` from `setImageFromData` -> 不再发生（SafeArtwork 返回 true）
- `CancellationException` / `InterruptedException` -> 原样重抛，不计日志，不转 `write_failed`
- `coverBytes` 非法或 `AudioFileIO.read/write` 失败 -> `write_failed`
- `song.path` 对应文件不存在 -> `write_failed`（`checkFileExists` 抛）
- 协程取消在 `WebDavAudioTagFileWriter` 外层已 `catch(CancellationException) throw`，与 `TagWriter` 重抛一致

### 5. Good/Base/Bad Cases
- Good：FLAC 含 JPEG 封面 `1200x1200` 刮削回传，`BitmapFactory` 解得 `1200×1200`，`FlacTag.createField` 成功，`TagReader.read().coverBytes` 与写入一致，不崩溃
- Base：封面为 4 字节最小 JPEG `FF D8 FF D9`，解码失败宽高回退 0×0，`MetadataBlockDataPicture` 仍以 `width=0 height=0` 写入成功，读回字节一致
- Bad：未设 `setAndroid(true)` 且走 `StandardArtwork`，FLAC 写入触 `ImageIO` 抛 `NoClassDefFoundError`，`catch(Exception)` 漏捕导致进程崩溃

### 6. Tests Required
- `TagWriterTest` 存量 5 项回归：基本标签、歌词+封面、clearLyrics、null 不覆盖、缺失文件 `write_failed`
- 新增冒烟（已验证，可选固化）：`FlacTag.createField(safeArtwork)` / `VorbisCommentTag.createField` / `ID3v24Tag.createField` 均不抛，`FLAC addField` 后 `artworkList.size==1`
- 手测：真机 WebDAV FLAC/MP3/OGG 各一首含封面刮削回传，日志 `WebDavWrite` 无 `NoClassDefFoundError`，`FileWriteResult.ok==true`
- Lint：`:app:lintMusesDebug` 通过；`:core:media:testDebugUnitTest` `:core:scrape:testDebugUnitTest` 通过

### 7. Wrong vs Correct
#### Wrong
```kotlin
// 直接用工厂，默认 isAndroid=false，走 StandardArtwork + ImageIO，FLAC 必崩
object TagWriter {
  private fun createArtwork(bytes: ByteArray): Artwork {
    val artwork = ArtworkFactory.getNew() // -> StandardArtwork on Android if not setAndroid
    artwork.binaryData = bytes
    artwork.mimeType = sniff(bytes)
    return artwork
  }
  fun write(file: File, req: TagWriteRequest): WriteResult = try {
    val af = AudioFileIO.read(file); val tag = af.tagOrCreateAndSetDefault
    tag.addField(createArtwork(req.coverBytes!!)); AudioFileIO.write(af); success()
  } catch (e: Exception) { failure("write_failed", e.message) } // Error 漏捕
}
```
#### Correct
```kotlin
// MusesApplication.onCreate(): TagOptionSingleton.getInstance().setAndroid(true)
object TagWriter {
  fun write(file: File, req: TagWriteRequest): WriteResult = try {
    val af = AudioFileIO.read(file); applyRequest(af.tagOrCreateAndSetDefault, req); AudioFileIO.write(af); success()
  } catch (e: Throwable) {
    if (e is CancellationException) throw e
    failure("write_failed", e.message ?: "写入失败")
  }
  private fun createArtwork(bytes: ByteArray): Artwork = AndroidSafeArtwork().apply {
    binaryData = bytes; mimeType = sniff(bytes); pictureType = 3 // DEFAULT_ID
  }
  private class AndroidSafeArtwork : Artwork {
    // ... 存储字段 ...
    override fun setImageFromData(): Boolean {
      val d = binaryData ?: return true
      val wh = tryDecodeBounds(d) // 反射 BitmapFactory inJustDecodeBounds
      width = wh?.first ?: 0; height = wh?.second ?: 0; return true
    }
    override fun getImage(): Any = throw UnsupportedOperationException()
  }
}
```
