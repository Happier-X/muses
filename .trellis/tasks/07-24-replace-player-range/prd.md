# PlayerPage 进度条 ion-range → HRange 评估与替换

父任务：`07-24-replace-ionic-with-happier-ui`
前置：建议在 `07-24-replace-ionic-low-risk` 完成后进行（避免同文件冲突，PlayerPage 会被两个子任务触碰的部分需协调）。

## Goal

评估 PlayerPage 播放进度条能否从 `ion-range` 迁移到 `HRange`；能则无回归替换，不能则明确登记为库缺口并保留 Ionic 实现。

## Background（高风险原因）

- 位置：`src/views/PlayerPage.vue:62` 的进度条 `ion-range`
- 现有依赖能力：
  - 拖动手势事件（`ionKnobMoveStart` / `ionKnobMoveEnd` / `ionInput`）驱动 seek 预览
  - 拖动中把视觉 clamp 到已缓冲终点，用本地 `preview` 值驱动
  - 隐藏 knob、自定义已播放填充（CSS shadow parts / `.progress-range`）
  - 刚修复过冷启动续播进度跳动 bug（commit 63e3f71），回归敏感
- `HRange` 现有 API 仅：`modelValue`/`min`/`max`/`step`/`disabled`/`size`/`ariaLabel`/`name` + `update:modelValue`
  - **缺**：knob 拖动开始/结束事件、缓冲 clamp、knob 隐藏与填充自定义的公开钩子

## Requirements

- 先做可行性判断：`HRange` 的 `update:modelValue` 能否配合本地状态复刻"拖动预览 + 释放 seek + 缓冲 clamp"，且视觉（隐藏 knob、已播放填充）可用 token/外层样式达成。
- 可行：替换并保持所有播放交互与视觉一致，重点回归续播、拖动 seek、缓冲显示。
- 不可行：在父任务缺口清单登记 `HRange` 能力差距（拖动生命周期事件、缓冲 clamp、knob/fill 自定义），保留 `ion-range`，本子任务以"评估结论 + 缺口登记"结案。

## Acceptance Criteria

- [ ] 给出 `HRange` 能否覆盖 PlayerPage 进度条的明确结论并记录依据。
- [ ] 若替换：拖动 seek、续播定位、缓冲 clamp、knob 隐藏/填充视觉与替换前一致；`npm run build` + unit test 通过；手动验证进度条交互无回归。
- [ ] 若保留：父任务缺口清单登记 `HRange` 差距，PlayerPage 保持 `ion-range` 不变。

## Notes

- 回归敏感区，宁可保留也不强行替换破坏播放体验。
