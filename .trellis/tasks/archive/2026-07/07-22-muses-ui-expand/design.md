# 技术设计：逐个扩展 Muses UI（Ionic 语义封装）

## 1. 风格契约（「符合我想要的风格」）

对齐 PRODUCT / DESIGN，**不是**对齐 Material 3 组件观感（虽 platform=android）：

| 原则 | 落地 |
|------|------|
| 暗场听席 | 列表层安静；主色仅 playing / 选中 / 主操作 |
| Flat | 无 elevation、无 FAB 厚阴影；分隔用 1px / 色差 |
| 列表干脆 / 沉浸柔和 | `MListRow` 硬朗行高；沉浸控不在本任务大改 |
| HeroUI Primary | `#006FEE` via token；禁止回退 Ionic 默认蓝 |
| 稳先于炫 | 不改播放/队列业务语义，只换壳与样式源 |
| 触控 | IconButton 默认 48×48 热区，图标 ~22–24 |

**反例（封装时禁止引入）**：侧边色条、渐变字、装饰玻璃、运营卡片、霓虹终端风。

## 2. 边界

```text
views / MiniPlayer
    → 只 import @/components/ui
components/ui (M*)
    → 可 import @ionic/vue + tokens
tokens.css
    → 唯一数值
variables.css
    → 仅 ion 桥接
```

业务页 **新增** UI 不得直接写死色/圆角；存量 ion 在迁移时逐步收敛。

## 3. 组件契约（本轮）

### 3.1 `MIconButton.vue`

- **Props**：`icon`（与现网 ion-icon data 一致）；`ariaLabel: string`（必填，a11y）；`disabled?`；`size?: 'md' | 'lg'`（md=48 热区 / lg 主控可后扩）；`variant?: 'default' | 'on-media'`（on-media 白/半透明，给沉浸预留，本轮可不强迁 Player）
- **事件**：`click`（透传，阻止冒泡可选 prop `stopPropagation`）
- **实现**：内用 `ion-button` fill=clear + `ion-icon`；CSS 清 padding、固定 min 宽高、颜色 `ink-muted` / active 用 primary 或 ink
- **禁止**：包装成带 elevation 的 FAB

### 3.2 `MListRow.vue`

- **Props**：
  - `title: string`
  - `subtitle?: string`
  - `coverSrc?: string | null`
  - `playing?: boolean`
  - `button?: boolean`（默认 true，可点行）
  - `detail?: boolean`
- **Slots**：`start`（覆盖默认 MCover）；`end`（more / remove）；`default` 一般不用
- **实现**：优先 `ion-item` 保持 Ionic 列表键盘/焦点行为；`--min-height: var(--muses-song-row-height)`；playing 用 `--muses-color-playing-bg`
- **组合**：默认内嵌 `MCover` size=md
- **注意**：Songs 虚拟列表需保持绝对定位行高不变；组件本身不负责 virtualizer

### 3.3 Queue 页

- Empty → `MEmptyState`
- 行 → `MListRow` + end 槽 `MIconButton` 移除
- 背景/padding → token（含 bottom 避开 MiniPlayer 若适用）
- **不**改队列业务 API

### 3.4 `MSettingRow.vue`（名称可最终定为 MSettingRow）

- **Props**：`label: string`；`description?: string`；`lines?: 'none' | 'inset' | 'full'`
- **Slots**：`end`（toggle / 文本值 / chevron 动作）
- **实现**：`ion-item` + `ion-label`；颜色与间距 token；**不**封装 ion-toggle 本体（toggle 仍放 slot）

## 4. 为何不是「挨个 ion 标签封装」

现网频次：`ion-button` > `ion-icon` > `ion-item` > toolbar/header…

若按标签镜像会得到 `MIonButton` 等，违反已锁定架构，且无法表达「playing 行」「设置行」语义。

**映射**：

| 频次高的 ion | 收敛到的 M* |
|--------------|-------------|
| ion-button（图标） | MIconButton |
| ion-item + 封面/双行 | MListRow |
| ion-item + toggle | MSettingRow |
| ion-page/header | MPage（已有） |
| ion-list | 暂不封装（结构容器） |
| ion-modal / alert | 本任务不做 |

## 5. Token 可能增补

- `--muses-touch-target: 48px`
- `--muses-icon-size-md: 22px` / `24px`
- IconButton 按下态可复用 existing duration

## 6. 风险

| 风险 | 缓解 |
|------|------|
| 虚拟列表 + MListRow 行高漂移 | 固定 min-height token；单测/目视 Songs |
| ion-item 默认 MD 涟漪/线 | CSS 变量压平；保持 flat |
| Settings 结构不一 | 只统一行壳，复杂 Sources 表单后移 |
| 一次改太多页 | implement 按组件批次 commit |

## 7. 回滚

每组件一批 commit；Queue / Settings 迁移可独立 revert。
