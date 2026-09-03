# 移除音量均衡功能

## Goal

彻底移除播放器音量均衡（响度归一化 / ReplayGain）功能，清理数据层、播放服务、扫描与标签解析、设置入口及相关测试与规范，确保无残留开关、字段与逻辑，构建与单测通过。

## 背景

- 现有实现分散在多层：`core:model/Song.replayGainTrackDb`、`core:data/Entities.replayGainTrackDb + MIGRATION_2_3`、`core:media/TagReader` 解析、`LocalLibraryScanner` 落库、`core:media/loudness/LoudnessCalculator|Controller`、服务侧 `PlaybackService` 的 `LoudnessController` 挂载、`core:data/SettingsRepository.loudnessEnabled`（DataStore `loudness_enabled`）、`core:model/playback/PlayerConfig.loudnessNormalizeEnabled`（`playback_config` JSON）、`app/settings/SettingsScreen` 的「音量均衡」Toggle
- 用户要求完全移除该功能，后续不再按 ReplayGain 调整 `ExoPlayer.volume`

## Requirements

### R1 - 模型与数据库
- `core:model/Song.replayGainTrackDb` 字段删除
- `core:data/db/Entities.SongEntity.replayGainTrackDb` 删除
- `core/data/mapper/Mappers.kt` 的 `toDomain/toEntity` 移除对应映射
- `MusesDatabase` 版本由 5 → 6，新增 `MIGRATION_5_6`：重建 `songs` 表去掉 `replayGainTrackDb` 列（兼容老 SQLite：`CREATE TABLE songs_new (...不含该列) → INSERT SELECT → DROP songs → RENAME`），保留其它列与数据；注释更新
- 保留历史迁移 `MIGRATION_2_3` 不删（旧库升级路径仍需经过 2→3，但 5→6 会清理列），仅在新迁移中清理

### R2 - 标签解析与扫描
- `core/media/metadata/TagReader.kt`：删除 `TrackTags.replayGainTrackDb`、`parseReplayGainTrackDb`、`parseReplayGainDbString`、`normalizeReplayGainDbValue`、`REPLAYGAIN_TRACK_ALIASES`、`REPLAY_GAIN_DB_ABS_MAX`，`parse()` 不再产出 RG
- `core/media/scanner/LocalLibraryScanner.kt`：`TagReaderResult` 移除 `replayGainTrackDb`，`scan()` 构造 `Song` 时不再传入，`readTagsSafely` 返回空值同步移除
- 如存在 `WebDavLibraryScanner` / `AudioTagReader` 对 RG 的透传，一并清理

### R3 - 响度计算与播放控制
- 删除整包 `core/media/loudness/`：`LoudnessCalculator.kt`、`LoudnessController.kt` 及单测 `LoudnessCalculatorTest.kt`
- `core/media/playback/PlaybackService.kt`：移除 `LoudnessController` 导入/字段/初始化 `loudnessController = LoudnessController(...)`、`start()` 挂载、`onDestroy` 的 `stop()` 与 `serviceScope` 关联，相关 `settingsRepository/songDao` 注入保留仅供其他逻辑使用（如仍无他用可保留注入但不再传给 loudness）
- `PlaybackService` 的 `volume` 始终保持系统默认 `1.0`，不按标签调整

### R4 - 设置与持久化
- `core/data/repository/SettingsRepository.kt`：移除 `loudnessEnabled: Flow<Boolean>`、`setLoudnessEnabled()`、`LOUDNESS_ENABLED` key
- `core/data/repository/PlaybackStateRepository.kt`：移除 `PlayerConfig.loudnessNormalizeEnabled` 的编解码（`decodeConfig` 默认值分支与 `writeConfig` 的 `put`），历史 JSON 中该键宽松忽略（缺键即默认行为移除）
- `core/model/playback/PlaybackModels.kt`：`PlayerConfig.loudnessNormalizeEnabled` 字段删除，注释更新为 `repeat/shuffle` 仅两项
- `app/settings/SettingsScreen.kt`：移除 `SettingsViewModel` 中 `loudnessEnabled` StateFlow 与 `setLoudnessEnabled`，`SettingsScreen` 中「音频」分组及 `SaltListItem` + `SaltToggle`（含 `VolumeUp` 图标、文案）整块删除，仅保留「关于」与「反馈」分组；无用 import 清理

### R5 - 测试与规范
- `core/media/metadata/TagReaderTest.kt`：删除或更新对 `replayGainTrackDb` / `R128 Q7.8` 的断言
- `core/data/repository/PlaybackStateRepositoryTest.kt`：移除 `loudnessNormalizeEnabled` 相关两项测试，保留 `repeat/shuffle` 默认值与 roundtrip
- `core/data/db/MigrationsTest.kt`：更新期望建表语句去掉 `replayGainTrackDb` 列，新增 5→6 迁移验证（可选）
- `LoudnessCalculatorTest.kt` 随目录删除
- 规范同步：`.trellis/spec/android/features-lyrics-playlist.md` 中「响度均衡」小节删除或标注已移除；`.trellis/spec/android/index.md` 中持久化默认值说明移除 `loudnessNormalize`；前端 spec 若提及 ReplayGain 仅作历史保留不强制改

## Acceptance Criteria

- [ ] `grep -rn "replayGain\|loudness\|Loudness\|R128" --include="*.kt" core app` 在 `core`/`app` 源码中零命中（`.trellis` 历史归档除外）
- [ ] `core/media/loudness/` 目录已删除
- [ ] `Song` / `SongEntity` / `TrackTags` 无 `replayGainTrackDb` 字段，`TagReader` 不再解析 RG
- [ ] `MusesDatabase` version=6 且 `MIGRATION_5_6` 存在，旧库（v5 含 RG 列）升级后 `songs` 表无该列且数据保留
- [ ] `SettingsRepository` 无 `loudnessEnabled`，`PlaybackStateRepository` 的 `PlayerConfig` 仅 `repeatMode/shuffleEnabled`
- [ ] `SettingsScreen` 无「音量均衡」UI，编译无 `loudness` 引用
- [ ] `PlaybackService` 无 `LoudnessController` 相关代码，运行时 `player.volume` 恒 1.0
- [ ] `./gradlew :core:data:testDebugUnitTest :core:media:testDebugUnitTest :app:lintMusesDebug :app:assembleMusesDebug` 全部通过

## Out of Scope

- 新增其他音量/均衡功能
- 播放队列、歌词、WebDAV、刮削逻辑改动
- 历史归档任务与旧 spec 历史描述的追溯性重写（仅更新现行契约）


## Notes

- DB 采用重建表方式兼容 `ALTER TABLE DROP COLUMN` 在旧 SQLite 不可用的情况
- 设置页移除后布局仅剩「关于」「反馈」两分组，需保持原有 `hazeSource` 与 `SaltNavbar` 结构
