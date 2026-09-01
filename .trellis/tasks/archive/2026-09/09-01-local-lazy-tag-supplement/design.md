# 设计 — 本地扫描未勾选标签时像 WebDAV 一样懒补充

## 1. 背景与现状

- `LocalLibraryScanner.scan(source, readTags)`：MediaStore 枚举 → 可选 `TagReader.read(File(data))` → 组装 `Song`。当前无论 `readTags` 真假，均写 `tagsVersion = TAGS_VERSION(1)`。
- `WebDavLibraryScanner.scan`：零下载文件名建库 `tagsVersion=FILENAME_TAGS_VERSION(0)`，播放时 `PlaybackService.persistenceListener.onEvents(MEDIA_ITEM_TRANSITION)` 对 `tagsVersion < TAGS_VERSION` 的 `SongEntity` 调 `AudioTagReader.readTagForUpdate(path, id)` 回写。
- `AudioTagReader.resolveFile(source)`：`http(s)` → Range 下载到缓存；否则 `File(source)`。本地 `Song.path` 为 `content://` URI，故 `File("content://...")` 不存在，懒读链路对本地失效。
- `SongDao.getUntaggedSongIds` 查询 `tagsVersion < 1` 已覆盖本地/WebDAV 两种待补标记，但本地从未产生 0。

## 2. 目标与非目标

- 目标：`readTags=false` 的本地产物 `tagsVersion=0`，复用既有播放懒扫描链路完成标签补齐；`AudioTagReader` 打通 `content://`。
- 非目标：不改 DB schema、不改 Song.path 存储形态（仍 `content://`）、不引入后台批量补齐 Worker、不改 WebDAV 限流与播放缓存契约。

## 3. 方案总览

```
[扫描] LocalLibraryScanner.scan(readTags=false)
  -> Song(tagsVersion=0, title=MediaStore/文件名, coverUri=null)
  -> SongRepository.replaceSourceSongs
                ↓
[播放] PlaybackService.onEvents(MEDIA_ITEM_TRANSITION)
  -> if tagsVersion < 1 then AudioTagReader.readTagForUpdate(content://, id)
                ↓
  AudioTagReader.resolveFile 支持 content://
                ↓
  TagReader/AudioFileIO 解析 -> TagUpdateData -> songRepository.upsert(tagsVersion=1)
  -> Room Flow 刷新列表 + rebuildDerivedIndexes
```

## 4. 详细设计

### 4.1 LocalLibraryScanner

- 常量：新增 `FILENAME_TAGS_VERSION = 0`（与 `WebDavLibraryScanner.FILENAME_TAGS_VERSION` 同值，语义对齐），保留 `TAGS_VERSION = 1`。
- 构造 Song 时：
  ```kotlin
  val effectiveTagsVersion = if (readTags) TAGS_VERSION else FILENAME_TAGS_VERSION
  Song(..., tagsVersion = effectiveTagsVersion, ...)
  ```
- 其余字段保持：`tags` 仍为 Empty 时标题回退 `item.titleFromStore ?: displayName`，`durationMs` 取 MediaStore，`coverUri=null`。不再做额外分支。
- `stableSongId` 与 `isSupportedAudio` 不变。

### 4.2 AudioTagReader

- 扩展 `resolveFile(source: String): File` 分支：
  1. `http(s)://` → 现有 `downloadFile`（Range）
  2. `content://` → `copyContentUriToCache(source)`：`context.contentResolver.openInputStream(Uri.parse(source))` 流拷贝到 `cacheDir/audio_tags/content_${hash}.tmp`（hash 取 `source.hashCode()` + 后缀），返回临时 File。失败抛异常上层转 null。
  3. `file://` → `File(Uri.parse(source).path ?: source)`
  4. 其它 → `File(source)`（物理绝对路径兼容）
- `readTags` 对拷贝临时文件同样走 `parseTags`（先 `AudioFileIO.read`，失败回退 `ID3v2 ByteBuffer`）。临时文件无需长期保留，`tagCache` 仍按原 `source` 字符串作内存键。
- `readTagForUpdate` 与 `extractCover` 均复用 `resolveFile`，故一并支持本地 URI。
- `getCacheFile` 等命名保持；新增 `copyContentUriToCache` 私有方法，异常返回抛给外层 catch → `readTags` 返回 null。
- 注意：`content://` 文件可能较大（FLAC 100MB），全量拷贝解析标签有 IO 成本。但复用 `AudioFileIO.read(File)` 需本地文件；jaudiotagger 不支持直接 InputStream，故必须落临时文件。权衡：本地懒读仅在用户实际播放时触发单次，非批量扫描，成本可接受。可在后续优化为 Range 式部分拷贝，但首版保持简单全拷贝。

### 4.3 PlaybackService

- 已有逻辑：
  ```kotlin
  if (entity.tagsVersion < LocalLibraryScanner.TAGS_VERSION) {
    val tagData = audioTagReader.readTagForUpdate(entity.path, entity.id)
    // hasUpdate -> upsert(tagsVersion=1) else upsert(tagsVersion=1) 标记已处理
  }
  ```
  无需新增分支，仅依赖 4.1/4.2 使本地 0 值可命中并可解析。
- 可选增强：若 `tagData == null`（content 拷贝失败/文件缺失），保持 `tagsVersion=0` 下次重试（现有 null 分支已如此，不额外置 1）。与 WebDAV “无更新时置 1” 的分支区分：`null` = 读失败重试，`hasUpdate==false` = 无标签已处理。

### 4.4 Repositories / DAO

- 无改动。`SongRepository.upsert -> songDao.upsert + rebuildDerivedIndexes` 已满足本地懒回写重建专辑/艺术家索引需求。

## 5. 数据流与兼容

- 扫描写入：`tagsVersion` 由调用方 `SourcesViewModel.startScan` 透传 `scanReadTags` 决定；ViewModel 已有 `scan(source, readTags=scanReadTags)` 分派，无需改动。
- 播放回写：同 WebDAV 唯一入口 `songRepository.upsert`，Room Flow 自动刷新 UI。
- 兼容历史库：已存在 `tagsVersion=1` 的本地歌曲不受影响；存量 `tagsVersion=0` 的 WebDAV 歌曲判断条件不变（`WebDavLibraryScanner.TAGS_VERSION` 与 `LocalLibraryScanner.TAGS_VERSION` 同为 1）。
- `file://` / 绝对路径历史数据：`File(...)` 分支仍兼容。

## 6. 权衡与备选

- **备选 A：Scan 时全量读标签后再异步批量回写**——与需求“懒补充”相悖，且 WebDAV 已验证批量会触发限流，不采纳。
- **备选 B：Local 懒补充走 ContentResolver 查询 MediaStore DATA 列拿物理路径后 File(path)**——需额外查询权限且 DATA 列在 Android 10+ 被弃用，不稳定；直接流拷贝更通用。
- **风险：本地大文件全拷贝**——仅播放触发单次，且后续 tagsVersion=1 不再触发，频率低；后续可优化为头部 256KB 截断读取（类似 WebDAV Range），首版不做。

## 7. 异常处理

- `content://` openInputStream 抛 SecurityException / FileNotFoundException → `readTags` 返回 null，外层静默，ErrorLogStore 留 WARN（现有 PlaybackService 已有）。
- jaudiotagger 解析失败 → null 同上。
- `CancellationException` 保持原样重抛，不计日志。

## 8. 验证策略

- 单元：`LocalLibraryScanner` readTags=false 时产出 tagsVersion=0；`AudioTagReader` 对 `content://` / `file://` / 绝对路径 / http 四分支解析（可用 Robolectric 或 fake ContentResolver）。
- 集成/手工：MuMu 真机本地文件夹扫描（开关关/开两态）→ 播放懒补验证；WebDAV 回归。
- 门禁：`assembleMusesDebug`、`testDebugUnitTest`、`lintMusesDebug`。

## 9. 涉及文件

- `core/media/scanner/LocalLibraryScanner.kt`（新增常量 + tagsVersion 条件）
- `core/data/tag/AudioTagReader.kt`（resolveFile 分支扩展 + content 拷贝）
- 可选：`PlaybackService.kt`（无需改动，仅复用；若需为 content 失败打更清晰日志可微调）
- 文档：`.trellis/spec/android/features-webdav-library.md` 补充本地 readTags=false 懒补充说明

## 10. 回滚

- `LocalLibraryScanner` 回退 `tagsVersion` 条件至恒为 1，即回退本需求；
- `AudioTagReader` 保留新增分支不影响既有 http/File 路径，保留亦无害。
