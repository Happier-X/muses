# PRD: 复制 MeloX 歌词实现到 muses（替换现有方案）

## Goal

从 MeloX-Android 项目复制歌词核心模型、解析器和 UI 面板代码到 muses 项目，**完全替换**现有歌词实现。

## Background

### MeloX 歌词模块（源）
- **核心模型**: `LyricModels.kt` — 丰富的歌词数据模型（音节、代理、伴唱、罗马化、高亮策略）
- **解析器**:
  - `NeteaseLyricParser` — YRC + LRC 合并解析（含翻译/罗马化对齐）
  - `LrcLyricsParser` — 普通 LRC 解析
  - `TtmlLyricsParser` — Apple Music TTML 解析（含 iTunes 扩展）
  - `KugouKrcLyricsParser` — 酷狗 KRC 解析（含 XOR 解密）
  - `QQMusicQrcLyricsParser` — QQ 音乐 QRC 解析（含 3DES 解密）
  - `QQMusicMeiQrcDecoder` — QRC 专有解密
- **工具**: `LyricTimelineProcessor`（时间线处理）、`LyricRomanizationAligner`（罗马化对齐）、`LyricBindingStore`（绑定存储）
- **客户端**: `AmlldbLyricsClient` — AMLL TTML 数据库客户端
- **UI**: `MeloXIOSLyricsPanel` — iOS 风格歌词面板（2418行，Canvas 渲染、弹簧动画、逐词高亮）

### muses 现有歌词模块（将被删除）
- **解析**: `lyrics-core` 库（AutoParser + TTML/LRC/YRC/KRC/Lyricify 解析器）
- **UI**: `NativeLyricsPanel` + `NativeKaraokeLine`（自研，LazyColumn + 逐词渐变 + 弹簧动画）
- **在线搜索**: AMLL TTML + 五源 Provider Chain + LRCLIB
- **规范**: `features-lyrics-playlist.md`、`features-lyrics-online.md`

## In Scope

### 层次1：核心模型 + 解析器
复制以下文件到 `core/lyrics/` 模块：

| 源文件 | 目标位置 | 说明 |
|--------|----------|------|
| `LyricModels.kt` | `core/lyrics/.../model/LyricModels.kt` | 歌词数据模型 |
| `LrcLyricsParser.kt` | `core/lyrics/.../parser/LrcLyricsParser.kt` | LRC 解析 |
| `TtmlLyricsParser.kt` | `core/lyrics/.../parser/TtmlLyricsParser.kt` | TTML 解析 |
| `KugouKrcLyricsParser.kt` | `core/lyrics/.../parser/KugouKrcLyricsParser.kt` | KRC 解析 |
| `QQMusicQrcLyricsParser.kt` | `core/lyrics/.../parser/QQMusicQrcLyricsParser.kt` | QRC 解析 |
| `QQMusicMeiQrcDecoder.kt` | `core/lyrics/.../parser/QQMusicMeiQrcDecoder.kt` | QRC 解密 |
| `LyricTimelineProcessor.kt` | `core/lyrics/.../processor/LyricTimelineProcessor.kt` | 时间线处理 |
| `LyricRomanizationAligner.kt` | `core/lyrics/.../aligner/LyricRomanizationAligner.kt` | 罗马化对齐 |
| `LyricBindingStore.kt` | `core/lyrics/.../store/LyricBindingStore.kt` | 绑定持久化 |
| `AmlldbLyricsClient.kt` | `core/lyrics/.../client/AmlldbLyricsClient.kt` | AMLL TTML 客户端 |

注意：`NeteaseLyricParser` 内嵌在 `LyricModels.kt` 中。

### 层次2：UI 面板
复制以下文件到 `feature/player/` 模块：

| 源文件 | 目标位置 | 说明 |
|--------|----------|------|
| `MeloXIOSLyricsPanel.kt` | `feature/player/.../lyric/MeloXIOSLyricsPanel.kt` | iOS 风格歌词面板 |
| `MeloXLyricsPanel.kt` | `feature/player/.../lyric/MeloXLyricsPanel.kt` | 兼容入口 |

### 删除现有歌词代码

删除以下文件/目录：

| 路径 | 说明 |
|------|------|
| `feature/player/.../lyric/NativeLyricsPanel.kt` | 现有歌词面板 |
| `feature/player/.../lyric/NativeKaraokeLine.kt` | 现有卡拉OK行 |
| `feature/player/.../lyric/GzLyricsView.kt` | 现有歌词视图 |
| `feature/player/.../lyric/LyricsParser.kt` | 现有解析器封装 |
| `feature/player/.../lyric/LyricsPanel.kt` | 现有面板入口 |
| `feature/player/.../lyric/AmllMapper.kt` | AMLL 映射器 |
| `feature/player/.../lyric/AppleSpringPlacement.kt` | 弹簧放置 |
| `feature/player/.../lyric/RelativeMillisLrcParser.kt` | 自定义LRC解析 |
| `feature/player/.../lyric/` 下其他歌词相关文件 | - |
| `feature/player/src/main/kotlin/com/mocharealm/accompanist/lyrics/` | vendored 歌词库 |

## Out of Scope

- MeloX 的音乐源提供者（QQ音乐、网易云等 API 客户端）
- MeloX 的播放器 UI（非歌词部分）
- MeloX 的设置/账号管理
- 在线歌词搜索（保留现有 Provider Chain，适配新数据模型）

## Acceptance Criteria

1. 核心模型和解析器能在 muses 项目中编译通过
2. UI 面板能在 muses 项目中编译通过
3. 现有歌词相关代码已删除
4. 包名和依赖适配到 muses 的模块结构
5. 播放器能正常显示和滚动歌词

## Key Decisions

- **完全替换**: 删除旧实现，使用 MeloX 作为唯一歌词方案
- **包名迁移**: `com.lladlam.melox.*` → `com.muses.player.*`
- **适配器模式**: 保持 MeloX 代码原样，通过适配器桥接播放状态

## Adaptation Details

### MeloXIOSLyricsPanel 依赖的 MeloX 特有类型

| 依赖 | 用途 | 适配方案 |
|------|------|----------|
| `MeloXPlaybackUiState` | 播放状态 | 创建 `MeloXPlaybackStateAdapter` |
| `MeloXSettingsRuntime` | 运行时设置 | 创建 `MeloXSettingsDefaults` |
| `MeloXAppVisibility` | 应用可见性 | 简化为默认值 |
| `MeloXLyricsStyle` | 歌词样式 | 创建默认值 |
| `LocalMeloXFontFamily` | 自定义字体 | 使用系统字体 |
| `MeloXLyricsRenderingQuality` | 渲染质量 | 简化为默认值 |
| `MeloXLyricAnnotationDisplayMode` | 注释显示模式 | 简化为默认值 |
| `MeloXLyricsGroupingMode` | 分组模式 | 简化为默认值 |
| `R` | 资源 | 适配 muses 资源 |

### OkHttp 依赖

muses 的 `core/lyrics/build.gradle.kts` 已有 OkHttp 依赖，`AmlldbLyricsClient` 可直接使用。

## Risks

- MeloXIOSLyricsPanel 依赖 MeloX 特有的播放状态模型，需适配
- 解密相关代码（QQMusicMeiQrcDecoder、QQMusicQrcLyricsParser）可能有许可证问题
- 2418 行的 UI 面板维护成本较高
- 删除现有代码后无法快速回滚（需 Git 恢复）
