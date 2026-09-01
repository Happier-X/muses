# 沉浸式上一曲点击无响应

## 目标

修复沉浸式播放页（`FullPlayerWebView + full-player.js`）点击上一曲（及关联下一曲）无响应的问题，使手机与平板的上一曲/下一曲可正常切歌且与队列/播放状态一致。

## 背景

- 前序已修复手势与图标链路：`toggleRepeat/Shuffle` 经 `set*Icon` 可视切换、`isInNoSwipeZone`、`bottomExclusionPx`、`requestDisallow(true)` 等，编译通过（`137b72f2`）。
- 现新现象：MuMu 上点击上一曲无法切换到上一曲（下一曲待确认）。锚点 `full-player.js:148-173,190-210` 的 `btn-prev/bottom-prev` 与 `btn-next/bottom-next` 经 `bindClick('previous'/'next') -> Android.onAction -> FullPlayerWebView.kt onAction previous/next -> viewModel.skipToPrevious/skipToNext -> PlayerConnection -> MediaController`。
- 可能分支：
  1) 触摸未到达 JS（无 `btn click previous`）
  2) Bridge 未到达 Kotlin（有 `btn click` 无 `-> previous`）
  3) Kotlin/Controller 未切歌（有 `-> previous` 但 `queue` 边界或 `repeatMode` 导致不切，如队首且非循环）
- 关联契约：`features-lyrics-playlist.md §7`；`PlayerConnection.skipToPrevious/next` 受队列长度与 repeat 影响。

## 需求

### 功能需求

1. **切歌可响应**：沉浸式手机与平板的上一曲/下一曲单击均能切歌（队内有前/后曲时），无可用前曲时行为符合 Media3 语义且不崩溃。
2. **不破坏既有**：循环/随机图标切换、横滑、下滑关闭、进度、歌词 seek、队列/更多保持可用。
3. **日志可诊断**：保留 `bindClick ok / btn click previous|next / -> previous|next` 日志。

### 非功能 / 约束

- 不改 Media3 队列契约，仅修复事件/桥接/展示层。
- 保持 `isInNoSwipeZone` 与手势分流不变。

## 验收标准

- [ ] 队列 ≥2 时，手机与平板分别点上一曲可回到上一首，再点下一曲回到原曲；队首/队尾边界符合预期且不抛异常。
- [ ] `adb logcat -s FullPlayer` 可见 `btn click previous` 与 `-> previous`（下一曲同理）。
- [ ] 横滑/下滑/进度/歌词无回归；`assembleMusesDebug`、`lintMusesDebug` 通过。

## 范围外

- 不改响度/歌词解析/播放列表持久化。

## 已确认事实与决策

- 2026-09-01 复测“进度归零了”：点上一曲时 `position>3s` 触发 `PlayerConnection.kt:257` 的 `seekTo(0)` 回零语义，非阻断；链路 `btn click previous -> -> previous` 已通。
- `skipToNext` 无阈值故下一曲应正常；队首/单曲循环等边界下 `hasPrevious=false` 亦不切。
- 待决策：保留行业常见的“>3s 回零、≤3s 切上一曲”语义并加强可观测性（推荐），或改为“始终切上一曲”（更直接但偏离惯例）。

