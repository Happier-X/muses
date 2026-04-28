# LRC 歌词显示功能 ✅

## Goal

在 PlayerBar 中实时显示当前播放歌词，替换掉现有的艺术家和时间显示，提升用户听歌体验。

## Status: COMPLETED

## What I already know

* PlayerBar 目前显示：封面 + 歌曲标题 + 艺术家 + 时间
* PlayerState 包含 positionMs、durationMs，每 250ms 更新一次
* AudioTrack 目前没有歌词相关字段
* 项目使用 ExoPlayer + MediaController 架构
* WebDAV 和本地文件两种音源

## Assumptions (temporary)

* 歌词来源优先级：LRC 文件 > ID3 嵌入歌词
* LRC 文件与音频文件同名，扩展名为 .lrc（如 song.mp3 → song.lrc）
* 歌词需要在 PlayerState 中新增字段
* 需要根据播放位置实时计算当前歌词行

## Requirements (evolving)

* 解析标准 LRC 格式歌词文件（本地 + WebDAV）
* 从 ID3 tag 中读取嵌入歌词（USLT 帧）
* 歌词来源优先级：LRC 文件 > ID3 嵌入歌词
* PlayerBar 显示当前歌词行（替换艺术家和时间）
* 根据播放进度实时同步歌词
* 无歌词时显示艺术家作为 fallback

## Acceptance Criteria (evolving)

* [x] LRC 解析器能正确解析时间标签和歌词文本
* [x] 能从本地文件和 WebDAV 读取 LRC 文件
* [x] 能从音频文件的 ID3 tag 读取嵌入歌词（USLT 帧）
* [x] PlayerState 包含当前歌词文本字段
* [x] PlayerBar 显示歌词，替换掉艺术家和时间
* [x] 歌词与播放进度同步
* [x] 无歌词时显示艺术家作为 fallback

## Definition of Done (team quality bar)

* Lint / typecheck green
* 手动测试：播放有歌词的曲目，歌词正确同步显示
* 手动测试：播放无歌词的曲目，显示合理 fallback

## Out of Scope (explicit)

* 歌词滚动视图（完整歌词页面）
* 歌词编辑功能
* 嵌入式歌词（ID3 tag 中的歌词）— 需要引入第三方 ID3 库如 mp3agic 或 jaudiotagger
* 多行歌词预览

## Technical Notes

* 新增文件：
  - `LrcParser.kt` — LRC 格式解析器，支持时间标签和元数据
  - `LyricLoader.kt` — 歌词加载器，支持本地和 WebDAV，集成 ID3 提取
  - `Id3LyricsExtractor.kt` — ID3 嵌入歌词提取器（使用 mp3agic 库）
* 修改文件：
  - `PlayerState.kt` — 添加 `currentLyric` 和 `hasLyrics` 字段
  - `PlayerViewModel.kt` — 添加歌词加载和同步逻辑
  - `PlayerBar.kt` — 显示歌词替代艺术家和时间
  - `MetadataExtractor.kt` — 添加 `lyrics` 字段（预留）
  - `build.gradle.kts` — 添加 mp3agic 依赖
  - `libs.versions.toml` — 添加 mp3agic 版本定义
* 歌词加载优先级：外部 LRC 文件 > ID3 嵌入歌词
* 歌词加载时机：`onMediaItemTransition` 时调用 `loadLyricsForTrack`
* 歌词同步：在 `startPositionPolling` 中每 250ms 更新当前歌词行
* ID3 支持：通过 mp3agic 库读取 USLT 帧，支持 MP3 文件
