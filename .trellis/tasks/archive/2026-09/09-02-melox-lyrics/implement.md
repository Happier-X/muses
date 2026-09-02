# Implementation Plan: 复制 MeloX 歌词实现到 muses（替换现有方案）

## Phase 0: 删除现有歌词代码

### Step 0.1: 删除 feature/player 歌词文件
- [ ] 删除 `feature/player/src/main/kotlin/com/muses/player/feature/player/lyric/` 下所有文件
- [ ] 删除 `feature/player/src/main/kotlin/com/mocharealm/accompanist/lyrics/` 目录
- [ ] 验证：编译报错（预期行为）

### Step 0.2: 清理 build.gradle.kts 依赖
- [ ] 检查 `feature/player/build.gradle.kts` 中的 lyrics-core 依赖
- [ ] 移除不再需要的依赖
- [ ] 验证：依赖解析通过

## Phase 1: 核心模型层（core/lyrics/）

### Step 1.1: 创建目录结构
- [ ] 在 `core/lyrics/src/main/kotlin/com/muses/player/core/lyrics/` 下创建子目录：
  - `model/`
  - `parser/`
  - `processor/`
  - `aligner/`
  - `store/`
  - `client/`

### Step 1.2: 复制 LyricModels.kt
- [ ] 从 MeloX 获取 `LyricModels.kt`
- [ ] 修改包名：`com.lladlam.melox.core.lyrics` → `com.muses.player.core.lyrics`
- [ ] 保存到 `core/lyrics/src/main/kotlin/com/muses/player/core/lyrics/model/LyricModels.kt`
- [ ] 验证：编译通过

### Step 1.3: 复制 LrcLyricsParser.kt
- [ ] 从 MeloX 获取 `LrcLyricsParser.kt`
- [ ] 修改包名
- [ ] 保存到 `core/lyrics/src/main/kotlin/com/muses/player/core/lyrics/parser/LrcLyricsParser.kt`
- [ ] 验证：编译通过

### Step 1.4: 复制 TtmlLyricsParser.kt
- [ ] 从 MeloX 获取 `TtmlLyricsParser.kt`
- [ ] 修改包名
- [ ] 保存到 `core/lyrics/src/main/kotlin/com/muses/player/core/lyrics/parser/TtmlLyricsParser.kt`
- [ ] 验证：编译通过

### Step 1.5: 复制 KugouKrcLyricsParser.kt
- [ ] 从 MeloX 获取 `KugouKrcLyricsParser.kt`
- [ ] 修改包名
- [ ] 保存到 `core/lyrics/src/main/kotlin/com/muses/player/core/lyrics/parser/KugouKrcLyricsParser.kt`
- [ ] 验证：编译通过

### Step 1.6: 复制 QQMusicQrcLyricsParser.kt
- [ ] 从 MeloX 获取 `QQMusicQrcLyricsParser.kt`
- [ ] 修改包名
- [ ] 保存到 `core/lyrics/src/main/kotlin/com/muses/player/core/lyrics/parser/QQMusicQrcLyricsParser.kt`
- [ ] 验证：编译通过

### Step 1.7: 复制 QQMusicMeiQrcDecoder.kt
- [ ] 从 MeloX 获取 `QQMusicMeiQrcDecoder.kt`
- [ ] 修改包名
- [ ] 保存到 `core/lyrics/src/main/kotlin/com/muses/player/core/lyrics/parser/QQMusicMeiQrcDecoder.kt`
- [ ] 验证：编译通过

### Step 1.8: 复制 LyricTimelineProcessor.kt
- [ ] 从 MeloX 获取 `LyricTimelineProcessor.kt`
- [ ] 修改包名
- [ ] 保存到 `core/lyrics/src/main/kotlin/com/muses/player/core/lyrics/processor/LyricTimelineProcessor.kt`
- [ ] 验证：编译通过

### Step 1.9: 复制 LyricRomanizationAligner.kt
- [ ] 从 MeloX 获取 `LyricRomanizationAligner.kt`
- [ ] 修改包名
- [ ] 保存到 `core/lyrics/src/main/kotlin/com/muses/player/core/lyrics/aligner/LyricRomanizationAligner.kt`
- [ ] 验证：编译通过

### Step 1.10: 复制 LyricBindingStore.kt
- [ ] 从 MeloX 获取 `LyricBindingStore.kt`
- [ ] 修改包名
- [ ] 适配 `MusicResourceId` 和 `MusicSource`（创建简化版本或适配器）
- [ ] 保存到 `core/lyrics/src/main/kotlin/com/muses/player/core/lyrics/store/LyricBindingStore.kt`
- [ ] 验证：编译通过

### Step 1.11: 复制 AmlldbLyricsClient.kt
- [ ] 从 MeloX 获取 `AmlldbLyricsClient.kt`
- [ ] 修改包名
- [ ] 确认 OkHttp 依赖可用
- [ ] 保存到 `core/lyrics/src/main/kotlin/com/muses/player/core/lyrics/client/AmlldbLyricsClient.kt`
- [ ] 验证：编译通过

## Phase 2: UI 层（feature/player/lyric/）

### Step 2.1: 创建 MeloXPlaybackStateAdapter
- [ ] 创建 `feature/player/src/main/kotlin/com/muses/player/feature/player/lyric/MeloXPlaybackState.kt`
- [ ] 定义 `MeloXPlaybackStateAdapter` 类
- [ ] 映射 muses 的 `PlayerState` 到 MeloX 需要的属性
- [ ] 验证：编译通过

### Step 2.2: 创建 MeloXSettingsDefaults
- [ ] 创建 `feature/player/src/main/kotlin/com/muses/player/feature/player/lyric/MeloXSettingsDefaults.kt`
- [ ] 定义默认值对象
- [ ] 替换 MeloX 设置类依赖
- [ ] 验证：编译通过

### Step 2.3: 复制 MeloXIOSLyricsPanel.kt
- [ ] 从 MeloX 获取 `MeloXIOSLyricsPanel.kt`（2418行）
- [ ] 修改包名：`com.lladlam.melox.ui.player` → `com.muses.player.feature.player.lyric`
- [ ] 替换 `MeloXPlaybackUiState` 为 `MeloXPlaybackStateAdapter`
- [ ] 替换设置类依赖为 `MeloXSettingsDefaults`
- [ ] 移除或替换 `LocalMeloXFontFamily` 依赖
- [ ] 替换 `R` 资源引用
- [ ] 保存到 `feature/player/src/main/kotlin/com/muses/player/feature/player/lyric/MeloXIOSLyricsPanel.kt`
- [ ] 验证：编译通过

### Step 2.4: 复制 MeloXLyricsPanel.kt
- [ ] 从 MeloX 获取 `MeloXLyricsPanel.kt`
- [ ] 修改包名
- [ ] 替换依赖
- [ ] 保存到 `feature/player/src/main/kotlin/com/muses/player/feature/player/lyric/MeloXLyricsPanel.kt`
- [ ] 验证：编译通过

## Phase 3: 集成与测试

### Step 3.1: 更新 build.gradle.kts
- [ ] 检查 `core/lyrics/build.gradle.kts` 是否需要额外依赖
- [ ] 检查 `feature/player/build.gradle.kts` 是否需要额外依赖
- [ ] 验证：依赖解析通过

### Step 3.2: 集成 PlayerScreen
- [ ] 修改 `PlayerScreen.kt` 使用新的 `MeloXLyricsPanel`
- [ ] 适配播放状态传递
- [ ] 验证：歌词面板正常显示

### Step 3.3: 集成测试
- [ ] 测试 LRC 解析
- [ ] 测试 TTML 解析
- [ ] 测试 KRC 解析
- [ ] 测试 QRC 解析
- [ ] 测试歌词面板渲染
- [ ] 测试歌词滚动
- [ ] 测试逐词高亮
- [ ] 测试翻译显示
- [ ] 测试罗马化显示

### Step 3.4: 回归测试
- [ ] 验证播放器 UI 正常
- [ ] 验证性能无明显下降

## Validation Commands

```bash
# 编译检查
./gradlew :core:lyrics:compileDebugKotlin
./gradlew :feature:player:compileDebugKotlin

# 测试
./gradlew :core:lyrics:test
./gradlew :feature:player:test

# 全量构建
./gradlew assembleMusesDebug
```

## Risk Mitigation

1. **编译失败**: 逐步复制，每步验证编译
2. **依赖冲突**: 检查现有依赖，避免重复
3. **功能异常**: 通过 Git 保留历史，可快速恢复
4. **性能问题**: 监控渲染性能，必要时优化

## Rollback Points

1. **Phase 0 完成后**: 如果删除有问题，可通过 Git 恢复
2. **Phase 1 完成后**: 如果核心层有问题，可删除 `model/`、`parser/` 等目录
3. **Phase 2 完成后**: 如果 UI 层有问题，可删除 `MeloXIOSLyricsPanel.kt` 等文件
4. **Phase 3 完成后**: 通过 Git 恢复整个任务
