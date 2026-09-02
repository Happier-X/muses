# Design: 复制 MeloX 歌词实现到 muses（替换现有方案）

## Architecture

### 核心模型层（core/lyrics/）

```
core/lyrics/
├── model/
│   └── LyricModels.kt          # 歌词数据模型
├── parser/
│   ├── LrcLyricsParser.kt      # LRC 解析
│   ├── TtmlLyricsParser.kt     # TTML 解析
│   ├── KugouKrcLyricsParser.kt # KRC 解析
│   ├── QQMusicQrcLyricsParser.kt # QRC 解析
│   └── QQMusicMeiQrcDecoder.kt # QRC 解密
├── processor/
│   └── LyricTimelineProcessor.kt # 时间线处理
├── aligner/
│   └── LyricRomanizationAligner.kt # 罗马化对齐
├── store/
│   └── LyricBindingStore.kt    # 绑定持久化
└── client/
    └── AmlldbLyricsClient.kt   # AMLL TTML 客户端
```

### UI 层（feature/player/lyric/）

```
feature/player/lyric/
├── MeloXIOSLyricsPanel.kt     # iOS 风格歌词面板（2418行）
├── MeloXLyricsPanel.kt        # 兼容入口
└── MeloXPlaybackState.kt      # 适配器：muses 播放状态 → MeloX 状态
```

## Data Flow

```
MusePlayer (播放状态)
    ↓
MeloXPlaybackStateAdapter (适配器)
    ↓
MeloXIOSLyricsPanel (UI 渲染)
    ↑
LyricsDocument (歌词数据)
    ↑
LyricTimelineProcessor (时间线处理)
    ↑
Parser (LRC/YRC/TTML/KRC/QRC)
```

## Adaptation Strategy

### 1. MeloXPlaybackUiState 适配

MeloX 的 `MeloXPlaybackUiState` 是一个复杂的 MediaController 包装器。我们创建一个适配器：

```kotlin
// feature/player/lyric/MeloXPlaybackState.kt
@Stable
class MeloXPlaybackStateAdapter(
    private val playerState: State<PlayerState>,
) {
    // 将 muses 的 PlayerState 映射到 MeloX 需要的属性
    val mediaId: String? get() = playerState.value.mediaId
    val title: String get() = playerState.value.title
    val artist: String get() = playerState.value.artist
    val isPlaying: Boolean get() = playerState.value.isPlaying
    val positionMs: Long get() = playerState.value.positionMs
    val durationMs: Long get() = playerState.value.durationMs
    // ... 其他属性
}
```

### 2. Settings 适配

MeloX 的设置类（`MeloXSettingsRuntime`、`MeloXLyricsStyle` 等）提供默认值：

```kotlin
// 临时方案：使用硬编码默认值
object MeloXSettingsDefaults {
    val lyricsStyle = MeloXLyricsStyle.Default
    val renderingQuality = MeloXLyricsRenderingQuality.High
    val annotationDisplayMode = MeloXLyricAnnotationDisplayMode inline
    val groupingMode = MeloXLyricsGroupingMode.Disabled
}
```

### 3. 字体适配

移除 `LocalMeloXFontFamily` 依赖，使用系统默认字体或 muses 主题字体。

### 4. 资源适配

将 MeloX 的 `R` 资源引用替换为 muses 的资源，或内联默认值。

## Compatibility Notes

- **包名迁移**: `com.lladlam.melox.*` → `com.muses.player.*`
- **依赖**: OkHttp 已存在于 `core/lyrics/build.gradle.kts`
- **Compose**: muses 使用 Jetpack Compose，与 MeloX 兼容
- **Media3**: muses 使用 Media3，与 MeloX 的 MediaController 兼容

## Trade-offs

### 选择 1：完整复制 vs 选择性复制
- **完整复制**: 保留所有功能，但维护成本高
- **选择性复制**: 只复制核心功能，简化维护
- **决策**: 完整复制，后续按需裁剪

### 选择 2：适配器模式 vs 直接修改
- **适配器模式**: 保持 MeloX 代码原样，通过适配器桥接
- **直接修改**: 修改 MeloX 代码以适配 muses
- **决策**: 适配器模式，减少冲突

### 选择 3：完全替换 vs 并存
- **完全替换**: 删除旧实现，一步到位
- **并存**: 新旧实现共存，逐步迁移
- **决策**: 完全替换，简化维护

## Rollback Plan

1. 通过 Git 恢复删除的文件
2. 重新编译验证
