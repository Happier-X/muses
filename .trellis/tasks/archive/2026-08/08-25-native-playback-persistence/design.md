# 技术设计：播放队列与会话持久化

## 1. 模块与文件

```
core:model/lyrics 旁新增 playback/PlaybackModels.kt（纯 Kotlin）
core:data/repository/PlaybackStateRepository.kt   # 队列快照 + 会话 + 配置（DataStore）
core:data/repository/RecentPlaysRepository.kt     # 最近播放（DataStore）
core:data/di 无需改动（复用既有 DataStore @Provides）
core:data/test PlaybackStateRepositoryTest / RecentPlaysRepositoryTest / MigrationTest
core/media/playback/PlaybackService.kt            # 恢复 + 节流保存接线（唯一修改的既有文件）
```

依赖方向：`core:media(服务) → core:data(store) → core:model`；无环。

## 2. 数据契约

### 2.1 队列+会话合并快照（key `playback_snapshot`，version=1）

```json
{
  "version": "1",
  "items":            [{"songId":"..."}],
  "originalOrder":    [{"songId":"..."}],
  "shuffleOrder":     [{"songId":"..."}] | null,
  "currentIndex": 0,
  "positionMs": 123456,
  "currentSongId": "..."
}
```

- Web 三 key 合并：queue(items/originalOrder/shuffleOrder) + session(currentSongId/position) 本就同生命周期，原子恢复避免半写
- 宽松解析：结构不符条目跳过；position 非有限/负数 → 0

### 2.2 播放器配置（key `playback_config`，version=1）

```json
{"version":"1","repeatMode":"all|one","shuffleEnabled":bool,"loudnessNormalizeEnabled":bool}
```

- 默认值对齐 queue.ts defaultConfig()：all / false / true（仅显式 false 关闭 loudness）

### 2.3 最近播放（key `recent_plays`，version=1）

```json
{"version":"1","entries":[{"songId","title","subtitle","coverUri"?,"playedAt"}]}   // 上限 50
```

## 3. 服务接线（PlaybackService）

- `@Inject lateinit var playbackStateRepository / recentPlaysRepository`
- **onCreate**（serviceScope.launch）：
  1. 读配置 → player.repeatMode/shuffleModeEnabled 应用；LoudnessController 已消费 settingsRepository.loudnessEnabled（保持现状，不双写）
  2. 读快照：songDao 按 songIds 批量解析（缺失项过滤）；resolved 非空 → setMediaItems + seekTo(defaultPositionStartMs=positionMs)；currentIndex 用于 seekTo(index, positionMs)
- **保存时机**：
  - `MediaSession.Listener.onEvents`：EVENT_MEDIA_ITEM_TRANSITION / EVENT_PLAY_WHEN_READY_CHANGED / EVENT_POSITION_DISCONTINUITY → scheduleSave()
  - scheduleSave：serviceScope 内 500ms debounce（避免 seek 拖动高频写）
  - onDestroy：cancel 前同步落盘一次（runBlocking 短超时，服务销毁路径可接受）
- **保存内容**：items = 当前 MediaItems 的 songId（mediaId）、shuffleOrder 由 player.isShuffleModeEnabled 决定取随机序或 null、currentIndex = currentMediaItemIndex、positionMs = currentPosition、currentSongId
- **最近播放**：onEvents EVENT_MEDIA_ITEM_TRANSITION 时按新 currentMediaItem.mediaId 查 songDao 登记

## 4. songId 载体

MediaItem.mediaId 即 Song.id（M1 约定）。恢复时从 SongDao.getById 批量解析；被删歌曲自然过滤（对齐 Web resolveSongsFromQueue 行为）。

## 5. 测试策略

- Repository roundtrip / 坏数据回退 / position 容错（PreferenceDataStoreFactory 临时文件，模式同 RollbackJournalStoreTest）
- RecentPlays：去重置顶 / 裁尾 50
- 迁移测试：MigrationTestHelper（core/data test 已有 room.testing + robolectric）覆盖 MIGRATION_4_5 列存在性与 v5 全 schema
