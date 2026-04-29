# 沉浸页面左滑歌词展示功能

## Goal

在沉浸式播放页面（NowPlayingScreen）中，左滑切换到歌词展示页面，完整复刻椒盐音乐歌词页面风格。

## What I already know

* **项目类型**: Kotlin + Jetpack Compose Android 音乐播放器
* **沉浸页面**: `NowPlayingScreen.kt` — 全屏播放器，当前用垂直下滑关闭
* **歌词解析**: `LrcParser.kt` — 支持 LRC 格式、逐字时间戳（但目前丢弃了逐字数据）
* **歌词加载**: `LyricLoader.kt` — 支持本地 LRC、WebDAV、嵌入 ID3 标签
* **歌词数据**: `ParsedLyrics`（含 `findLineIndex`、`getCurrentLine`），`LyricLine(timeMs, text)`
* **PlayerViewModel**: 持有 `positionMs`、`durationMs`、`isPlaying`、`albumArtUri`
* **歌词当前未在 UI 中展示**，只有 PlayerBar 中显示单行

## Assumptions (temporary)

* 左滑通过水平分页（HorizontalPager 或类似）切换歌词页/封面页
* 歌词页复用已有 `ParsedLyrics` 数据
* 需要新增 `LyricsPage.kt` 页面组件

## Requirements (evolving)

* **歌词页布局**：全屏沉浸，模糊+暗化专辑封面为背景
* **歌词展示**：当前行白色大字居中（22-28sp），过往行暗化（50-60%透明度），未来行更低（30-40%透明度），仅显示当前行 ±2 行
* **行级高亮**：当前行整体白色高亮（非逐字），无卡拉OK效果
* **切换方式**：左滑/右滑水平分页切换歌词页 ↔ 封面页
* **下滑关闭**：歌词页和封面页均可下滑关闭，阈值与现有一致（80dp）
* **点击跳转**：点击歌词行跳转到对应时间戳
* **自动滚动**：当前行切换时平滑滚动居中；用户手动滚动时暂停，3秒后恢复
* **无歌词状态**：显示"暂无歌词"提示
* **手势优先级**：水平滑动优先于垂直滑动（先判断方向）

## Acceptance Criteria (evolving)

* [ ] 左滑从封面页切到歌词页，右滑切回
* [ ] 下滑关闭手势在歌词页和封面页均可触发
* [ ] 当前行白色高亮，过往/未来行按透明度区分
* [ ] 当前行居中，自动跟随播放进度滚动
* [ ] 无歌词时显示"暂无歌词"
* [ ] 点击歌词行跳转播放位置
* [ ] 用户手动滚动后自动恢复跟随
* [ ] 模糊背景正确加载，暗化叠加层可见

## Decision (ADR-lite)

**Context**: 逐字卡拉OK效果实现复杂度高，需要改造 LrcParser 支持 WordTimestamp；手势协调影响交互一致性
**Decision**: 
- 采用行级高亮（A），不做逐字卡拉OK
- 下滑关闭覆盖全场景（B），保持体验一致
**Consequences**: 实现更轻量，进度可控；后续可迭代增加逐字效果

## Acceptance Criteria (evolving)

* [ ]

## Definition of Done (team quality bar)

* Tests added/updated (unit/integration where appropriate)
* Lint / typecheck / CI green
* Docs/notes updated if behavior changes

## Out of Scope (explicit)

* 歌词搜索/下载
* 歌词编辑
* 桌面歌词
* 迷你播放器歌词（已有）

## Technical Notes

### 椒盐音乐歌词页风格（Research 摘要）

**视觉效果**
- 背景：模糊+暗化专辑封面，顶部到底部暗色渐变叠加层
- 当前行：白色大字（22-28sp，bold），垂直居中
- 逐字高亮：金黄/琥珀色（#FFB347 / #FFA500），左到右渐进填充
- 过往行：降低透明度（50-60%），字号较小（16-18sp）
- 未来行：更低透明度（30-40%）
- 只显示当前行 ±1-2 行（共 3-5 行）

**动效**
- 卡拉OK逐字高亮：每字/每字符按时间戳依次变色（100-200ms 每段）
- 自动滚动：当前行切换时平滑滚动居中
- 用户手动滚动时暂停自动滚动，2-3秒后恢复
- 背景微视差滚动

**交互**
- 水平滑动：歌词页 ↔ 封面页切换（分页，非覆盖）
- 垂直下滑：关闭全屏播放器（已有）
- 点击歌词行：跳转到该行时间戳
- 无歌词：显示"暂无歌词"空状态

**手势冲突风险**
- 水平滑（切换页）与垂直滑（关闭页）需要明确协调
- 建议：识别滑动方向，水平为主（切页），垂直次之（关闭），阈值不同

### 代码缺口

1. **逐字时间戳丢失**：`LrcParser.kt` 解析了 `<mm:ss.xx>word</mm:ss.xx>` 但只存了行级 `LyricLine`，需要新增 `WordTimestamp` 数据结构
2. **PlayerState 只存单行歌词**：需要扩展为完整歌词对象 + 当前行索引
3. **无专门歌词组件**：需新增 `LyricsPage.kt`
4. **背景模糊未实现**：albumArtUri 可用，需加 blur + 暗化 overlay
5. **手势协调**：水平分页 + 垂直关闭需分别设置不同阈值

## Research References

* [`research/jiaoyan-lyrics-style.md`](research/jiaoyan-lyrics-style.md) — 椒盐音乐歌词页 UI/UX 详细研究
