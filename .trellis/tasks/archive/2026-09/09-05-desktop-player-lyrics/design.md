# 桌面播放页歌词技术设计

## 1. 组件与数据流

```text
core:common jvmShared
  └─ lyrics/panel/SimpleLyricsPanel.kt   # 上收（同包名 com.muses.player.core.lyrics.panel? 保持原包）
feature:player（安卓）→ 同包名透传消费
composeApp 桌面 PlayerScreen
  ├─ DesktopPlayerHook：currentSong 变化 → 读库 lyrics/lyricsFormat → LyricsParser.parseDocument → StateFlow
  ├─ 在线搜索按钮 → LyricsMatcher.match(OnlineLyricsQuery) → 结果直接渲染（+可选写库）
  └─ 布局：左封面/控制，右歌词面板（简化双栏，对齐安卓双面板语义）
```

## 2. 关键点

- SimpleLyricsPanel 依赖核对：仅 Compose/coroutines/数学 → jvmShared 直接放；若引 feature 内部类型则一并随迁或参数化。
- 时间轴定位：面板按 positionMs 推进当前行（安卓版已有此逻辑，桌面注入 positionMs 流）。
- 在线搜索结果展示：首版直接展示命中歌词文本渲染，不做候选列表（对齐安卓"简单面板"范围）。
- 歌词格式：LRC/QRC/TTML/YRC 经 LyricsParser 统一 parseDocument，面板已支持。

## 3. 风险

- SimpleLyricsPanel 若隐式依赖安卓资源/上下文需小改（预期无）。
- 桌面 positionMs 流已有（JvmPlayerPort.positionMs），对接零成本。

## 4. 回滚

- 上收为移动+透传，revert 单提交；桌面播放页改动自包含。
