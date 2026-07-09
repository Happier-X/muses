# 平板 MVP：响应式断点（列表多列 + Tab 侧栏 + 内容限位）

## 目标

在屏幕物理宽度 ≥ 768px 时，将应用从手机单列 + 底部 Tab 的布局切换为：列表多列网格 + 左侧 Tab 侧栏 + 内容最大宽度限位居中。低于 768px 保持当前手机布局无回归。

归属于父任务 `07-09-tablet-adapt`（平板适配 A+B+D 首轮 MVP）。

## 背景与现状

- 项目无任何响应式断点代码；`src/theme/variables.css` 仅有注释
- 路由：`/tabs/*`（Songs/Albums/Artists/Playlists/Sources/Settings）+ `/player`、`/queue`（全屏页）
- `src/views/TabsPage.vue` 使用 `ion-tabs` + 底部 `ion-tab-bar`（6 个标签）
- Songs/Albums/Artists 三页均为 `ion-list > ion-item` 单列；Playlists 为空状态占位
- 用户明确：列表保持 `ion-list` 形态，仅排成多列（不做卡片网格重构）

## 关键决策（已与用户确认）

| 决策点 | 取值 |
|--------|------|
| 列表网格化形态 | 保持 `ion-list/ion-item`，外层 CSS Grid 排成多列 |
| 宽屏断点 | 768px（物理宽度） |
| Tab 宽屏形态 | `ion-split-pane` 左侧侧栏 + 内容区，标签按钮竖排 |
| 内容最大宽度 | 720px，居中，作用于各列表页 `ion-content` 内部内容 |
| 全屏页作用范围 | 仅 `/tabs/*` 生效；`/player`、`/queue` 本轮不动（父任务后续 C 子任务） |

## 需求

| ID | 需求 |
|----|------|
| R1 | 在 `src/theme/variables.css` 中定义全局平板断点 CSS 变量（如 `--muses-tablet-breakpoint: 768px`、`--muses-content-max-width: 720px`），单一来源 |
| R2 | SongsPage / AlbumsPage / ArtistsPage：宽屏下 `ion-list` 用 CSS Grid 排成多列（列数根据可用宽度自适应，每列最小宽度合理，如 `ion-item` 单列约 280-320px 自然分列） |
| R3 | TabsPage：用 `ion-split-pane when="768"` 在宽屏下显示左侧 `ion-menu` / 侧栏（含 6 个标签按钮竖排），内容区在右侧；窄屏保持当前 `ion-tabs` + 底部 Tab Bar |
| R4 | 各列表页 `ion-content` 内部内容最大宽度 720px 且居中（宽屏生效，窄屏不限） |
| R5 | 窄屏 (<768px) 视觉与交互与当前完全一致，无回归 |
| R6 | Playlists/Sources/Settings 三页（非 list 或非密度 list）也受益于 R4 内容限位，但不强制网格化 |

## 技术约束

- 不修改原生 Android 代码（MainActivity.kt / AndroidManifest.xml）
- 不引入新依赖（只用 Ionic 已有组件 `ion-split-pane`、原生 CSS Grid、CSS `@media`）
- 列表页模板改动最小化：尽量只在外层加 grid 容器或 CSS，不重构 `ion-item` 内部结构
- 遵循 `.trellis/spec/frontend/` 规范：显式 import Ionic 组件、`<style scoped>` 局部样式、全局变量放 `variables.css`

## 边界情况

| 场景 | 预期 |
|------|------|
| 768px 边界附近反复横跳 | split-pane / media query 自然切换，无闪烁死循环 |
| 分屏（窄宽） | <768px → 回退手机布局（父任务 G 未来专项优化） |
| Settings 表单项 | 受 R4 限位保护，不被横向拉满 |
| 暗色模式 | 与现有 `prefers-color-scheme: dark` 共存，无冲突 |
| MiniPlayer | 本轮不动其形态（父任务 F 未来子任务） |

## 验收标准

- [ ] 屏幕宽度 ≥768px 时，SongsPage 显示多列 `ion-item`（至少 2 列，宽度允许时更多）
- [ ] 屏幕宽度 ≥768px 时，AlbumsPage / ArtistsPage 同样多列
- [ ] 屏幕宽度 ≥768px 时，Tab 控件从底部 Bar 变为左侧侧栏，6 个标签按钮竖排，点击切换内容区
- [ ] 屏幕宽度 ≥768px 时，列表内容居中且最大宽度 720px，两侧留白
- [ ] 屏幕宽度 <768px 时，所有页面与改动前视觉/交互完全一致
- [ ] `npm run lint` 零错误
- [ ] `npm run build` 通过（vue-tsc + vite build）
- [ ] `npm run test:unit` 不引入新的失败（已有 player.spec.ts 的 Media Session 环境失败不算）

## 超出范围

- Player / Queue 页双栏改造（后续 C 子任务）
- 横屏占比专项校准（后续 E 子任务）
- MiniPlayer 宽屏形态（后续 F 子任务）
- 分屏窄宽专项优化（后续 G 子任务）
- 列表项重构为卡片（用户明确否决）