# PRD：WebDAV 回写下载失败原因透传与健壮性

## 目标

刮削回写 WebDAV 时「下载音频失败」不再是一句笼统提示：带上 HTTP 状态码/超时等真实原因；降低大文件慢速下载的失败率；避免与播放渐进下载并发冲突。

## 现状事实（代码勘察）

### 原生层（android/.../WebDavPlugin.kt + WebDavAudioCache.kt）

- `writeMetadata` 流程：`getOrDownload(url)` 下载整个音频到缓存 → 副本写 tag → PUT 回网盘
- **吞异常点**：`WebDavPlugin.kt` writeMetadata 中
  ```kotlin
  val cachedFile = try { audioCache.getOrDownload(...) }
  catch (_: Exception) { failureResult("download_failed", "下载 WebDAV 音频失败，无法写标签。") }
  ```
  底层 `WebDavAudioCache.download()` 实际抛出 `IllegalStateException("webdavCacheDownloadFailed:<HTTP状态码>")` / `"webdavCacheEmptyFile"`，SocketTimeoutException 则直接冒泡——全部被吞
- **超时**：`CONNECT_TIMEOUT_MS=15s`、`READ_TIMEOUT_MS=30s`（OkHttp readTimeout 是包间隔非总时长，但 openlist/夸克中转卡顿 >30s 即失败）
- **并发冲突**：播放中的渐进下载（progressiveSessions 按 URL 注册）与回写的完整下载可能同时拉同一文件；`.partial` 存在时 getCachedFile 返回 null → 回写重新全量下载

### 前端层（src/features/scrape/failure-copy.ts）

- `download_failed` 当前映射为**固定文案**「下载 WebDAV 音频失败，请检查网络后重试」——若原生透传了更具体 message，会被固定文案覆盖。需改为优先使用原生 message

## 需求

1. **R1 异常详情透传（原生）**：writeMetadata 的 download catch 不再统一吞掉——按异常类型生成 message：
   - `webdavCacheDownloadFailed:<code>` → 「下载 WebDAV 音频失败（HTTP <code>）」
   - SocketTimeoutException → 「下载 WebDAV 音频超时，请检查网络后重试」
   - 其他 → 携带 exception.message
2. **R2 前端配合**：failure-copy.ts 的 `download_failed` 改为优先透传原生 message（原生 message 为空才用固定文案）
3. **R3 下载专用长超时（原生）**：完整文件下载使用独立更长读超时（60s），不影响 propfind/PUT 等
4. **R4 渐进缓存复用（原生）**：download 前若同 URL 有进行中的渐进会话，等待其完成（带上限超时）后再查缓存，避免并发双下载；等待超时则照旧自行下载

## 验收标准

1. gradle assembleDebug 通过；前端 lint/test/build 通过
2. MuMu 真机：构造一次回写失败（如断网/错误密码），行详情显示含 HTTP 码或「超时」的具体原因
3. 正常路径回归：有缓存时回写不触发下载；播放中歌曲回写不再双下载
4. PUT 失败既有文案（已含 HTTP 码）不变

## 范围外

- 不改 PUT 阶段逻辑与文案
- 不改渐进下载播放链路本身

## 约束

- 密码不进日志/异常信息
- Kotlin 改动仅限 WebDavPlugin.kt / WebDavAudioCache.kt
