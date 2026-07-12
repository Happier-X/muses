# 歌词页 AMLL 视觉复刻

## Goal

将沉浸式播放器的**歌词页**视觉与布局，按用户参考截图复刻为更接近现代音乐 App 的大字左对齐 AMLL 歌词体验；继续使用 `@applemusic-like-lyrics`，不自研歌词引擎。

## Background

- 当前 `src/views/PlayerPage.vue` 已有：
  - 窄屏左右滑：控制页 ↔ 歌词页
  - 宽屏双栏：左控制 + 右歌词
  - AMLL `BackgroundRender`（mesh 渐变）+ `LyricPlayer`
  - LRC 解析（`parseLrc` + `normalizeLrc`）
  - 无歌词空状态
  - 下滑关闭 overlay
- 上一轮 `07-12-immersive-player-ui` **只改了控制页**，歌词页明确列为非目标。
- 用户提供了参考截图，并确认「继续用 AMLL」。
- 参考图分析（非像素级精确）：深色背景、歌词左对齐大字、当前行约在屏高 40% 附近高亮，前后行更暗/更小。

## Requirements

### 必须

1. 按参考截图复刻歌词页的视觉风格（对齐、字号层级、当前行位置、边距），仍基于 AMLL `LyricPlayer`。
2. **窄屏**歌词页顶部展示轻量信息：主标题歌名 + 副标题歌手（无歌手时仅歌名）；其下为全宽 AMLL 歌词区。
3. **窄屏**歌词页底部不放迷你进度/播放控制，仅安全区留白；完整控制仍在控制页。
4. **宽屏**右侧歌词区**不重复**顶部歌名/歌手（左侧控制页已有），只保留与窄屏一致的 AMLL 大字左对齐歌词视觉参数。
5. 保留现有能力：
   - LRC 解析与同步滚动
   - 无歌词空状态
   - 窄屏左右滑切换控制页/歌词页
   - 下滑关闭（含歌词滚动触顶后才允许下拉关闭的既有逻辑）
   - 宽屏双栏并排
   - AMLL 动态背景 + fallback 背景
6. 不改播放引擎、队列、媒体会话、迷你播放条业务逻辑。
7. `npm run test:unit`、`npm run lint`、`npm run build` 通过；必要时更新 `tests/unit/player.spec.ts`。

### 已确认

- 顶部区域：**B. 顶部轻量信息**——窄屏歌词页顶部展示歌名 + 歌手。
- 顶部字段：**B. 歌名 + 歌手**——主标题歌名，副标题歌手；不拼接专辑。
- 底部区域：**A. 纯歌词到底**——无迷你进度/播放控制。
- 宽屏策略：**B. 宽屏精简**——右侧只有歌词，不要顶部信息；AMLL 视觉参数与窄屏一致。

### 非目标

- 不自研歌词渲染器，不替换 AMLL。
- 不新增在线歌词搜索、TTML/逐词歌词源扩展（除非现有 LRC 已含逐词）。
- 不新增歌词行点击 seek、翻译/音译切换等新交互（除非 AMLL 默认行为无需额外开发）。
- 不改控制页主布局（除非为共享样式必须微调）。
- 不主动 git commit / push。

## Target Layout

### 窄屏歌词页

1. 顶部安全区 + 轻量信息（左对齐：歌名主标题、歌手副标题）
2. AMLL `LyricPlayer`：左对齐、大字、当前行约在可视区中上部（约 0.35–0.45）
3. 底部仅安全区，无迷你控制

### 宽屏歌词栏

1. 无顶部歌名/歌手条
2. 同套 AMLL 左对齐大字参数填满右侧栏

## Acceptance Criteria

- [x] 窄屏歌词页：顶部有歌名 + 歌手（可省略空歌手），底部无迷你控制。
- [x] 窄屏歌词页视觉与参考图在对齐方式、字号层级、当前行垂直位置上明显对齐。
- [x] 宽屏右侧不显示重复的歌名/歌手条，但歌词仍为大字左对齐 AMLL 风格。
- [x] 仍使用 AMLL `LyricPlayer` / `parseLrc`，无自研滚动引擎。
- [x] 有歌词/无歌词、左右滑、下滑关闭、宽屏双栏仍可用。
- [x] 单元测试与 lint/build 通过。
- [x] 可沉淀的歌词页 UI 约定回写 `.trellis/spec/frontend/`。

## Notes

- 任务目录：`.trellis/tasks/07-12-lyrics-page-amll-ui`
- 主要改动面预计：`src/views/PlayerPage.vue`、`tests/unit/player.spec.ts`
- AMLL 可调：`alignAnchor` / `alignPosition` / `enableBlur` / `enableScale` / `wordFadeWidth` + CSS 字号边距
