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

## 来自 child 2（PlayerPage 进度条评估）

### 3. PlayerPage 播放进度条
- 位置：`src/views/PlayerPage.vue`（`.progress-range` 进度条 `ion-range`）
- 保留：`ion-range` + `@ionInput`/`@ionChange`
- 结论：**HRange 结构上可行，但收益不抵回归风险，保留 ion-range**。
- 原因（HRange 能力差距）：
  - **无拖动生命周期语义事件**：HRange 是原生 `<input type="range">`，仅 emit `update:modelValue`（等价原生 `input`），没有 `ionChange`/`ionKnobMoveEnd` 对应的"释放时统一提交 seek"钩子。当前逻辑用 `ionInput` 做拖动预览、`ionChange` 做释放 seek，语义清晰分离。可用 fallthrough 监听原生 `@change` 兜底（HRange 未声明 change 为 emit），但跨平台时序需实测。
  - **事件 payload 差异**：`readRangeEventValue` 依赖 `CustomEvent.detail.value`，换 HRange 后需重写为读 `target.value` / modelValue payload。
  - **视觉可达成**：knob 隐藏可通过 `:deep(::-webkit-slider-thumb)`、填充色有 `--h-range-fill`/`--h-range-track-bg`/`--h-range-thumb-*` CSS 变量，能对齐深色沉浸态（此项非阻塞）。
- 不替换的核心理由：
  - 该文件刚修完 #47 冷启动续播进度跳动 bug（commit 63e3f71），进度条为高回归敏感区。
  - 测试面广：`tests/unit/player.spec.ts` 有 3+ 处 IonRange stub 与 1 处硬断言"进度控件为 ion-range"（约 3421 行），迁移成本高。
  - 手势拦截白名单 `INTERACTIVE_SELECTOR` 依赖 `ion-range` 选择器，需同步改为 `input[type="range"]`。
  - PRD 明确"宁可保留也不强行替换破坏播放体验"。
- 解除条件：happier-ui 为 `HRange` 提供拖动生命周期事件（如 `drag-start`/`drag-end`）或显式 `change` emit 契约，明确区分"拖动预览"与"释放提交"。

## 说明
- PlayerPage 沉浸式 icon 控件的缺口见父任务 `prd.md` 白名单表（`color=light` 深色态 / `variant=fill` / `is-active`）。
