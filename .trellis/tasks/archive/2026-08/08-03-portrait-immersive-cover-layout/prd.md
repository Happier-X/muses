# 竖屏沉浸式播放页封面排版优化

## Goal

优化手机竖屏沉浸式播放页（控制面板）的垂直排版：将「封面 → 信息 → 进度 → 主控 → 模式栏」作为一组**整体垂直居中并收紧块内间距**，消除封面偏上与大块松散留白，提升手机日常沉浸感。

## Background

- 控制页：`src/views/PlayerPage.vue` 的 `.info-panel` / `.info-panel-inner`；样式：`src/theme/tailwind.css` 的 `.player-overlay` 前缀规则。
- 竖屏现状根因：
  - `.info-panel-inner` 使用 `justify-between` + `h-full`，把弹性 `.cover-slot`（`flex: 1 1 auto`）拉满剩余高度，封面在槽内居中后视觉上仍偏上，下方控件被推到底，中间/上下留白松散。
  - 封面尺寸本身有 `max-height: min(52dvh, 340px)` 与正方形 width 约束，不是「封面过大」，而是**分配策略**问题。
- 平板（`≥768px`）已是 `justify-content: center` 分栏，不在主诉范围。
- 矮屏断点（`max-height: 720px` / `520px`）已有 padding/gap/按钮收紧；须与本改兼容，不得破坏一屏适配与正方形封面。

## Key Decision

- **视觉节奏：方案 3 — 整体垂直居中收紧**  
  全块（封面到模式栏）作为一组垂直居中，减小块内无效空白；不为了「封面更大」而单独放大封面。

## Requirements

- **R1**：手机竖屏（`<768px`）控制页内容组整体垂直居中；封面不再因 `justify-between` + 弹性槽而被顶到上半区。
- **R2**：块内垂直节奏更紧凑（封面与信息、信息与控件之间无明显「悬空」大空白）；保持可读与可点热区。
- **R3**：封面保持正方形；窄屏 width 仍须含 vw + 与 cover-slot `max-height`/dvh 对齐的上限（默认 / 720 / 520 各档不得回归长方形）。
- **R4**：一屏内展示全部控件（封面、信息、进度、主控、模式栏），禁止页面纵向滚动；矮屏仍不得隐藏模式栏/进度。
- **R5**：不改歌词页布局、手势关闭、横向切面板、进度 seek；平板分栏语义与观感无回归。
- **R6**：安全区继续由 panel 内容 padding 承担；禁止给 HPopup panel 或 AMLL/fallback 背景加 padding「修顶」。
- **R7**：实现上优先 CSS / 少量 utility class；`.cover-slot` 在竖屏须**停止吞噬剩余高度**（例如由 `flex: 1 1 auto` 改为可收缩但不扩张的策略），否则仅改 `justify-center` 仍会在槽内留出上下空带。

## Acceptance Criteria

- [ ] AC1：常见手机竖屏（约 700–900+ 逻辑高度）打开控制页，内容组整体居中，封面不再明显「靠上悬空」。
- [ ] AC2：封面到模式栏的块内间距收紧，整体不显松散；进度与按钮仍可舒适操作。
- [ ] AC3：默认 / `max-height:720` / `max-height:520` 下封面仍为正方形（width 含对应 dvh/`max-height` 对齐上限）。
- [ ] AC4：控制页无纵向滚动；模式栏与进度始终可见可用。
- [ ] AC5：`≥768px` 平板分栏与歌词页无回归；下滑关闭 / 横向切面板正常。

## Out of Scope

- 歌词页（含 AMLL、浮动 FAB）重排
- 播放逻辑、封面加载/粘性封面、背景渲染
- 平板专属视觉大改或新皮肤/主题
- 为方案 2 单独放大封面

## Technical Notes

- 主改文件：`src/views/PlayerPage.vue`（`.info-panel-inner` / `.cover-slot` utility）、`src/theme/tailwind.css`（`.player-overlay` 竖屏与矮屏规则）。
- 竖屏：`justify-between` → `justify-center`；`.cover-slot` 避免 `flex-grow` 吃满剩余高度（`flex: 0 1 auto` 或等价），保留 `min-height: 0` 与 `max-height` 以便矮屏收缩。
- 平板 media 已 `justify-content: center`，改动须 scoped 到窄屏或确认宽屏仍正确。
- 规范锚点：`.trellis/spec/frontend/component-guidelines.md`（正方形封面、矮屏收紧、一屏适配、class 白名单）。
- 完成后应把「竖屏整体居中收紧、cover-slot 不 flex-grow」写入 component-guidelines，避免回退到 `justify-between` 弹性顶栏。

## Risks

- 极矮屏上整组居中后，若未保留 cover 收缩，可能溢出；须靠既有 `max-height` 断点 + cover-slot 可 shrink 消化。
- 拇指区略远离底边是方案 3 的已知取舍，不在本任务内再改成「底对齐操作区」。
