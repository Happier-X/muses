# 沉浸式矮屏/横屏布局自适应

## Goal

在车机矮屏与手机横屏等高度受限场景下，沉浸式播放控制页封面保持正方形并可自适应缩放，同时压缩下方控制区占位，避免封面被压成长方形、控制区挤占过多垂直空间。

## Issue

- GitHub #17：`沉浸式播放页面在车机这种高度比较窄的屏幕，或者是手机上的横屏的时候，封面会被压缩成长方形，我需要能够自适应，下面的播放控制区域占了太多的位置`

## Confirmed Facts（代码可证）

### 当前控制页结构（`PlayerPage.vue`）

自上而下固定竖排：

1. `.cover-slot`（`flex: 1`，`max-height: min(52dvh, 340px)`；矮屏 `42dvh/260px`）
2. `.song-info`（歌名/歌手）
3. `.progress-area`（进度条 + 时间）
4. `.controls`（上一曲 / 播放暂停 / 下一曲；默认 52/68px）
5. `.mode-bar`（循环 / 随机 / 队列；44px）

容器：`immersive-shell` / panels 固定 `100dvh`、`overflow: hidden`，禁止纵向滚动。

### 封面正方形约定（已有但不足）

- Spec：`.cover` 必须 `aspect-ratio: 1; height: auto; object-fit: cover`。
- 宽屏：`.cover` width 含 dvh（`min(40vw, 48dvh, 320px)`；矮屏 `42dvh/260px`），避免 max-height clamp 把正方形拉扁。
- **窄屏**：`.cover` width 仅为 `min(72vw, 100%, 340px)`，仅靠 `max-height: 100%` 限制高度。
  - 当可用高度 < 宽度时，高度被 clamp、宽度仍宽 → **封面变成长方形**（Issue 根因之一）。

### 已有矮屏微调（`max-height: 720px`）

- 按钮略缩小（46/58/40）。
- cover-slot max-height 收到 `42dvh/260px`。
- **控制区仍占固定大块垂直空间**（进度 + 主控 + 模式栏 + gap + panel padding），矮屏/横屏下封面槽位被进一步挤压。

### 相关 spec

- `.trellis/spec/frontend/component-guidelines.md`：封面正方形、一屏适配、宽屏 width 必须含 dvh。

## Decisions

| 决策 | 结论 |
|------|------|
| 矮屏/横屏布局策略 | **A：保持竖排，全面收紧**（不改为左右分栏） |
| 控制区如何收紧 | **A：仅缩小尺寸**；保留歌名/进度/主控/模式栏全部控件 |
| 是否动歌词页 | **A：不动歌词页**；仅改控制页 |

## Requirements

1. **R1** 任意可视高度下，控制页封面（含占位）保持正方形，不得被压成长方形。
2. **R2** 窄屏 `.cover` / `.placeholder-cover` 的 `width` 必须同时受可用高度（dvh / cover-slot max-height）约束，与宽屏既有策略一致。
3. **R3** 矮屏/横屏下压缩 panel padding、`info-panel-inner` gap、进度热区、主控与模式栏按钮尺寸，释放垂直空间给封面槽位。
4. **R4** 正常竖屏手机高度（如 `>720px`）保持现有观感，不无故缩小控件。
5. **R5** 宽屏平板左右分栏与封面正方形约定不破坏；宽屏矮高继续 width 含 dvh。
6. **R6** 不隐藏任何控制控件；不改 seek / 播放语义；不改歌词页布局与 AMLL 参数。
7. **R7** 同步 `component-guidelines.md`：窄屏封面 width 也必须含高度约束；矮屏收紧策略写清。
8. **R8** 布局相关回归：现有 player 单测通过；若有样式断言则补封面正方形/矮屏收紧说明，不强制截图测试。

## Acceptance Criteria

- [ ] AC1：窄屏矮高 / 横屏下 `.cover` 与占位封面视觉为正方形（宽高比 ≈ 1），不再被 clamp 成长方形。
- [ ] AC2：矮屏控制区（padding/gap/按钮/进度热区）明显小于正常高度，封面槽位获得更多可用高度。
- [ ] AC3：正常竖屏高度下封面与控件尺寸与改前相当，无“整体缩水”回归。
- [ ] AC4：宽屏分栏 + 宽屏矮高封面正方形仍成立。
- [ ] AC5：歌词页样式与交互无本任务改动。
- [ ] AC6：`component-guidelines.md` 已同步；lint / type-check / 相关单测通过。

## Out of Scope

- 矮屏左右分栏布局
- 隐藏模式栏 / 进度时间行等控件
- 歌词页 / AMLL 字号与 header 改造
- 车机专属设置或方向锁
- 播放逻辑、缓冲、seek 语义

## Task Type

Complex
