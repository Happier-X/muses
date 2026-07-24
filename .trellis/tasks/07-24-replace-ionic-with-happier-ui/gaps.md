# happier-ui 0.0.2 组件缺口登记

记录替换 Ionic 组件过程中，因 happier-ui 能力不足而**保留 Ionic 实现**的位置与原因。供后续 spec 同步（child 3）与库升级评估参考。

## 来自 child 1（低风险替换）

### 1. MiniPlayer 播放/暂停 + 队列按钮
- 位置：`src/components/player/MiniPlayer.vue`（播放/暂停、队列）
- 保留：`ion-button fill="clear"` + `HIcon`
- 原因：
  - 播放/暂停图标用 `variant="fill"`（实心），而 `HIconButton` 内部渲染 `HIcon` 时**只透传 `icon`/`size`，不透传 `variant`**，图标恒为 `stroke` 描边 → 会视觉回归。
  - 队列按钮与相邻播放/暂停按钮同容器，为保持一致性一并保留。
- 解除条件：`HIconButton` 支持透传 icon `variant`（或新增 `iconVariant` prop）。

### 2. QueuePage 清空队列 + 列表项删除按钮
- 位置：`src/views/QueuePage.vue`（导航栏清空队列、列表项删除）
- 保留：`ion-button fill="clear" color="danger"` + `HIcon`
- 原因：这是"透明背景 + danger 前景色"组合。`HIconButton` 的 variant 里：
  - `ghost` = 透明背景 + **primary（蓝）** 前景，颜色不对；
  - `danger` = **实心红底** + 白字，背景不对；
  - `danger-soft` = 浅红底 + 红字，带背景色。
  - 无"透明底 + danger 前景"组合 → 任一选择都非零回归。
- 解除条件：`HIconButton` 支持 `ghost` + danger 前景色（如 `variant="ghost" tone="danger"`）。

## 说明
- PlayerPage 沉浸式 icon 控件的缺口见父任务 `prd.md` 白名单表（`color=light` 深色态 / `variant=fill` / `is-active`）。
- PlayerPage `ion-range` 缺口结论由 child 2 `07-24-replace-player-range` 落定。
