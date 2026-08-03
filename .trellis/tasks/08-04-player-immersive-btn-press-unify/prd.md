# 沉浸页图标按钮按下态统一

## Goal

让沉浸式播放页所有 `HButton` 图标键（主控、mode-bar、歌词 FAB）在深色底上的默认色、hover、按下（`:active`）与语义 `.is-active` 反馈一致，不再出现「有的闪浅灰块、有的半透明白」的分裂感。

## Background

- 组件已统一：`HButton variant="ghost" is-icon-only shape="circle"` + `HIcon`。
- 库 ghost 默认：黑字 + hover/active 浅灰实心底（`surface-secondary` / `border-subtle`），适合浅色列表页，不适合沉浸深色底。
- 现状分裂：
  - **主控 / mode-bar**：宿主只覆盖了 `color`（白 / 半透明白），**未**覆盖 `:hover` / `:active` → 按下仍闪浅灰块。
  - **歌词 FAB**（`08-03-lyric-fab-style-translation`）：已有真实 `color`/`background` + hover/active 半透明白底。
  - **`.is-active`**（单曲循环 / 随机 / 翻译开）是常驻语义高亮，与手指 `:active` 混用时更显不一致。

## Requirements

- **R1**：`.player-overlay` 内所有 ghost 图标按钮共享同一套沉浸式交互色：浅色图标、透明/微透明默认底、hover/active 半透明白底，**禁止**依赖库默认浅灰 active。
- **R2**：主控三键、mode-bar 三键、歌词 FAB（翻译 + 播放）按下反馈视觉同族（允许尺寸/默认透明度不同，但 active 机制一致）。
- **R3**：语义 `.is-active`（repeat one / shuffle on / 翻译开）仍可区分，且其 hover/active 不掉回库浅灰。
- **R4**：不改按钮尺寸热区、图标语义、播放逻辑、手势隔离；不改 MiniPlayer（非沉浸全屏底，可另议）。
- **R5**：规范写明「沉浸页 ghost 图标键必须覆盖 color/background/hover/active，禁止只改字色」。

## Acceptance Criteria

- [ ] AC1：主控上一曲/播放/下一曲按下为半透明白底（或同族），非浅灰实心块。
- [ ] AC2：mode-bar 循环/随机/队列按下反馈与主控同族。
- [ ] AC3：歌词 FAB 与控制页按钮按下同族（可复用同一选择器族）。
- [ ] AC4：`.is-active` 常驻态可辨，且按下/hover 不闪库默认浅灰。
- [ ] AC5：`component-guidelines` / 必要时 `features-player` 与实现一致。

## Out of Scope

- 改 happier-ui 库内 ghost 默认（本任务宿主覆盖即可）
- MiniPlayer / 列表页按钮按下态
- 重做主控尺寸、加 solid 圆底阴影
- 进度条 HRange 样式

## Technical Notes

- 优先在 `src/theme/tailwind.css` 用 `.player-overlay .h-button--ghost`（或 `.controls` / `.mode-bar` / `.lyric-fab` 共用基类）统一覆盖；特异性需压过库 `@media (hover) hover` 与 `:active`。
- 歌词 FAB 现有规则可收敛进同一基类，避免三套复制。
- mode-bar 默认字色可继续略淡（约 0.58），active/hover 时提亮即可。

## Risks

- 选择器过宽误伤 Player 内非图标 ghost（当前无文案 ghost，风险低）。
- 桌面 hover 半透明白在深底上可能偏亮——保持与现有 FAB 同级即可。
