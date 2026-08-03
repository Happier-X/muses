# 歌曲页跳转当前曲二次点击下移

## Goal

修复 SongsPage「跳转到当前播放」FAB：连续多次点击时，当前曲行应稳定停在列表可视区顶部正确位置，不得在第二次及之后点击时再往下挪。

## Background / Root Cause

- 入口：`src/views/SongsPage.vue` 的 `scrollToCurrentSong` + `HFloatingBubble`。
- 当前实现双路径滚动：
  1. `rowVirtualizer.scrollToIndex(index, { align: 'start', behavior: 'smooth' })`
  2. 再对挂载行 `row.scrollIntoView({ behavior: 'smooth', block: 'start' })`
- 虚拟行外层带 `scroll-mt-[108px]`（约 108px），规范原文是为「滚动端口顶对齐时扣 navbar + shuffle-bar」；但**当前滚动容器是 navbar/shuffle 下方的 `listParentRef`**，顶栏已不在滚动端口内，108px 属于**错误二次偏移**。
- 第一次点击：目标常在屏外，`scrollToIndex` 主导，观感正常。
- 第二次点击：目标已在/接近 `align: 'start'`，`scrollIntoView` + 无效 `scroll-margin-top` 再滚一次，行视觉下移约一截顶栏高度。
- 对照：`QueuePage` 仅用 `scrollToIndex`（`align: 'center'`），无 `scrollIntoView`，无此问题。
- 规范 `component-guidelines` 仍写「`[data-song-id]` + `scrollIntoView` + scroll-margin」，与现网虚拟列表实现及正确偏移模型不一致，需随修同步。

## Requirements

- **R1**：在当前曲已在列表中时，连续多次点击 FAB，目标行停靠位置一致，不得二次下移。
- **R2**：目标行主信息完整落在列表可视区内（不被 navbar/shuffle 挡住——靠滚动容器已在 chrome 下方保证，而非对行再加 108px scroll-margin）。
- **R3**：虚拟列表仍用 `@tanstack/vue-virtual`；跳转以 `scrollToIndex` 为主，禁止依赖会与虚拟定位冲突的二次 `scrollIntoView`（或等价错误 margin 补偿）。
- **R4**：可选轻高亮（约 1.2s）可保留；无当前曲 / 不在列表时 FAB 行为不变。
- **R5**：不改播放、队列、搜索等无关逻辑；QueuePage 可不改（已正确），除非复用小 helper 时顺带对齐文档。

## Acceptance Criteria

- [ ] AC1：从列表远处点 FAB → 当前曲出现在列表可视区顶部（或末尾无法再滚时的最大位置），主信息不被顶栏挡。
- [ ] AC2：在 AC1 结果上再点 FAB（及第 3、4 次）→ 行位置不发生可感知下移/跳动。
- [ ] AC3：当前曲靠近列表底部时，仍停在容器允许的最大 scroll，不强制「假置顶」造成空白。
- [ ] AC4：宽屏单列与窄屏行为一致；虚拟滚动与点击播放无回归。
- [ ] AC5：`component-guidelines` 中 FAB 跳转描述与实现一致（虚拟列表 + `scrollToIndex`，禁止错误 scroll-margin/`scrollIntoView` 双滚）。

## Out of Scope

- 重做 FAB 视觉/拖拽
- 搜索过滤列表
- QueuePage 行为变更（非本 bug）
- 虚拟列表行高/overscan 大改

## Technical Notes

- 推荐：`scrollToCurrentSong` 只保留 `scrollToIndex(..., { align: 'start' })`（可去掉 smooth 双动画竞态，或仅 virtualizer 一侧 smooth）；挂载后只设高亮，**删除** `scrollIntoView`。
- 删除行上 `scroll-mt-[108px]`（或证实无害后移除），避免以后再被 `scrollIntoView` 误用。
- 若需等行挂载再高亮，保留 `nextTick` + rAF；高亮不依赖二次滚动。
- 更新 `.trellis/spec/frontend/component-guidelines.md`「SongsPage 跳转到当前播放 FAB」与 anti-pattern（若有）。

## Risks

- 仅 `scrollToIndex` 时，个别 WebView 上 measure 未完成可能首帧偏差；可用 post-frame 再 `scrollToIndex` 一次兜底，仍不要 `scrollIntoView`。
