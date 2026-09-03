# 设计：修复重新刮削后播放仍显示旧元信息

## 1. 背景与问题拆解

```
WritebackOrchestrator.applyScrapeChanges
  ├─ (2) 写文件：本地并行 / WebDAV 串行 → FileWriteResult(ok/code)
  └─ (3) 写库：upsert(song.copy(title/artist/album/coverUri/lyrics
          + metaSources 标记 embedded/scrape))
          ⚠ tagsVersion 未改变，WebDAV 仍为 0

PlaybackService.persistenceListener.onMediaItemTransition
  └─ if (tagsVersion < 1) AudioTagReader.readTagForUpdate(path) → upsert 覆盖库
     ⚠ 无 metaSources 守卫，FILE_FAILED 场景用旧文件标签覆盖新刮削值

AudioTagReader
  ├─ tagCache: Map<source, AudioTags> 内存常驻
  └─ downloadFile: cacheDir/audio_tags/audio_<hash>_* 磁盘缓存
     ⚠ 写文件成功后未失效，后续读仍命中旧缓存

UI 展示
  ├─ MusesApp.MainViewModel.nowPlaying: useMeta = tagsVersion<1 → 优先 mediaMetadata
  └─ SongsPage 列表当前行同逻辑
     ⚠ 已刮削字段仍被旧 mediaMetadata 抢占
```

## 2. 总体策略

采用"库优先 + 标记守卫 + 缓存失效"三件套，不提升全局 `TAGS_VERSION`，以 `metaSources` 作为"已刮削"权威标记。

## 3. 模块边界与改动点

```
core:scrape/writeback/WritebackOrchestrator.kt
  └─ 新增依赖 AudioTagReader（可选接口），文件写成功后失效缓存

core:data/tag/AudioTagReader.kt
  └─ 新增 fun invalidate(pathOrUrl: String) 清内存+磁盘缓存

core:media/playback/PlaybackService.kt
  └─ 懒扫描分支增加 metaSources 守卫，逐字段判定是否覆盖

app/navigation/MusesApp.kt (MainViewModel.nowPlaying)
feature/player/PlayerViewModel.kt (stickyCover/lyrics 链路保持，展示层不改)
feature/library/SongsPage.kt (当前行标题/副标题/封面)
  └─ 调整 useMeta → 按字段 metaSources 优先库值

core:data/mapper/Mappers.kt  不改，仅确认 toDomain 正确还原 metaSources
```

## 4. 详细设计

### 4.1 缓存失效（R3）

- `AudioTagReader` 新增：

```kotlin
fun invalidate(source: String) {
  tagCache.remove(source)
  // 磁盘：按 getCacheFile(url) 规则定位并 delete；content:///file:// 命中 copyContentUriToCache 产物
  getCacheFile(source).takeIf { it.exists() }?.delete()
  // content:// 场景：按 hash 匹配删除 content_* 前缀文件
}
```

- `WritebackOrchestrator` 注入 `audioTagReader: AudioTagReader? = null`（可空保持单测兼容）
- `writeSingleFile` 返回 `ok=true` 后，对该 `song.path` 调用 `invalidate`；`ok=false` 不清
- WebDAV 且 `coverRemoteUrl` 场景同属文件写成功分支一并失效

### 4.2 懒扫描守卫（R1）

`PlaybackService` 内 `entity.toDomain()` 后取 `metaSources`，重构覆盖逻辑：

```kotlin
val ms = entity.toDomain().metaSources
val tag = tagData // AudioTagReader 产物
val shouldOverrideTitle  = ms?.title == null && !tag.title.isNullOrBlank()
val shouldOverrideArtist = ms?.artist == null && !tag.artist.isNullOrBlank()
val shouldOverrideAlbum  = ms?.album == null && !tag.album.isNullOrBlank()
val shouldOverrideCover  = ms?.cover == null && tag.coverUri != null
val shouldOverrideLyrics = entity.lyrics.isNullOrBlank() && !tag.lyrics.isNullOrBlank()
// duration 始终允许取 max，不受守卫限制
```

仅对需覆盖字段取新值，其余保留 `entity` 原值；`tagsVersion` 置 1 的时机改为"至少有一个字段被覆盖或原为全空"时才抬升，避免无更新却抬升导致后续刮削字段被错误认为已固化（但已刮削字段已由守卫保护，抬升与否不影响正确性，选取"有覆盖才抬升"最保守）。

### 4.3 UI 优先级（R2）

`MainViewModel.nowPlaying` 当前：

```kotlin
val useMeta = song == null || song.tagsVersion < TAGS_VERSION
title = if (useMeta) metaTitle ?: song.title else song.title
```

调整为按字段：

```kotlin
val ms = song?.toDomain()?.metaSources // 或直接读 entity.metaTitle 非空判断
val title  = if (ms?.title != null) song.title else (metaTitle ?: song?.title ?: "未知歌曲")
val artist = if (ms?.artist != null) song.artist else (metaArtist ?: song?.artist ?: "未知艺术家")
val album  = if (ms?.album != null) song.albumTitle else (metaAlbum ?: song?.albumTitle ?: "未知专辑")
val cover  = if (ms?.cover != null) song.coverUri else (metaCover ?: song?.coverUri)
```

`SongsPage` 列表当前行同款改造，抽共用函数 `resolveDisplayTitle(song, mediaMetadata)` 置 `core:media` 或 `feature:library` 内局部函数，避免重复。

粘性封面：`PlayerViewModel.refreshLyricsWithEntity` 封面分支同步增加 `metaSources.cover` 守卫（有标记则优先库值）。

### 4.4 队列刷新（R4，可选）

不在本设计强制实现；若实现则在 `PlayerConnection` 新增：

```kotlin
fun refreshQueueMetadata(songs: List<Song>)
```

遍历 `player.mediaItemCount` 按 `mediaId` 查找新 `Song` 并 `replaceMediaItem(index, newItem)`，或文档化要求调用方刮削完成后以最新 `songs` 重新 `play(currentId, songs)`。

## 5. 数据流与时序

```
刮削预览确认
  → WritebackOrchestrator.applyScrapeChanges
    → 文件写 ok → AudioTagReader.invalidate(path) → 写库 upsert(metaSources)
  → Room Flow 触发 SongsPage / nowPlaying 观察，UI 立即显示新值（R2 保证优先库）

下一次播放切歌
  → PlaybackService.onMediaItemTransition
    → tagsVersion<1 但 metaSources 有值 → 守卫跳过覆盖 → 库值保留
    → tagsVersion<1 且无标记 → 正常懒扫描补齐（回归）
```

## 6. 兼容与回滚

- `AudioTagReader.invalidate` 为新增方法，无调用方回滚安全
- `PlaybackService` 守卫为新增条件分支，回滚即恢复旧覆盖逻辑
- UI 优先级调整为纯展示分支，回滚恢复 `useMeta` 旧逻辑
- 任何一步失败不影响写回主流程（try/catch 静默）

## 7. 测试策略

- `AudioTagReaderTest`：invalidate 后 `readTags` 不再命中旧缓存
- `WritebackOrchestratorTest`：文件成功分支断言 `invalidate` 被调用，失败分支不断言
- `PlaybackService` 懒扫描守卫：构造 `SongEntity(metaTitle=embedded, tagsVersion=0)` + 旧文件标签，播放过渡后库标题仍为刮削值
- 手测：WebDAV FILE_FAILED 歌曲刮削→播放→迷你条/列表/沉浸页均为新值
