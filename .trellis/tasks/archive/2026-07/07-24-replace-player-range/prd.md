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

- [x] 给出 `HRange` 能否覆盖 PlayerPage 进度条的明确结论并记录依据。
- [x] 结论：保留 `ion-range`。父任务 `gaps.md` 已登记 `HRange` 差距（拖动生命周期事件、事件 payload、测试面/手势白名单），PlayerPage 保持 `ion-range` 不变。

## 评估结论（2026-07-24）

**结论：保留 `ion-range`，不替换。**

HRange 经源码确认为原生 `<input type="range">`，仅 `modelValue`/`min`/`max`/`step`/`disabled`/`size`/`ariaLabel`/`name` + `update:modelValue`。

- 结构上可行：`update:modelValue` 可复刻拖动预览；knob 隐藏与填充色可用 `:deep()` 伪元素 + `--h-range-*` CSS 变量达成；HRange 不因程序化 value 变化 emit，反而避开 #47 陷阱。
- 但不替换：（a）HRange 无拖动释放语义事件（`ionChange` 无对应，fallthrough `@change` 需跨平台实测）；（b）测试面广（player.spec.ts 多处 stub + 硬断言）；（c）该文件刚修 #47 续播跳动 bug，高回归敏感。
- 依 PRD"宁可保留不强行替换"，收益（少一个 Ionic 依赖）不抵代价。已在父任务 `gaps.md` 登记解除条件。

## Notes

- 回归敏感区，宁可保留也不强行替换破坏播放体验。
