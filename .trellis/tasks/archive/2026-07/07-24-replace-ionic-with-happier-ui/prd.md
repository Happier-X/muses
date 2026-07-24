# 用 happier-ui 替换可替换的 Ionic 组件（父任务）

## Goal

在 happier-ui 0.0.2 能力范围内，把项目里可替换的 Ionic 组件系统性替换为 happier-ui 组件，并同步 spec 白名单，使"能用 happier-ui 就用 happier-ui"的约定落到代码与规范。

## Background

happier-ui 0.0.2 新增 `HProgress`、`HCard`、`HCellGroup`/`HCell`、`HIconButton` 等，填补了旧 spec 白名单里标记为"库缺口保留"的若干项（`ion-progress-bar`、`ion-card*`、icon-only 按钮）。当前代码已部分用 `HButton`/`HNavBar`/`HIcon`，剩余可替换点集中在 SourcesPage、各列表页与 PlayerPage。

### 组件可替换性结论（调研）

| Ionic | happier-ui | 结论 |
|------|-----------|------|
| `ion-progress-bar` | `HProgress` | 可换（SourcesPage，支持 indeterminate） |
| `ion-card`+header/title/subtitle/content | `HCard` + slots | 可换（SourcesPage 音源卡，subtitle 用 body 组合） |
| 文字 `ion-button` | `HButton` | 可换（SourcesPage modal "关闭" ×4） |
| 列表/导航栏 icon-only `ion-button` | `HIconButton` | 可换（QueuePage/SongsPage/PlaylistsPage/PlaylistDetailPage/MiniPlayer；icon 走 prop，默认 stroke） |
| PlayerPage 沉浸式 icon 控件 | — | **保留（缺口）**：需 `color=light` 深色态、图标 `variant=fill`、`is-active` 自定义态，HIconButton 均不覆盖 |
| `ion-range`（PlayerPage 进度条） | `HRange` | **需独立评估**：手势/缓冲 clamp/shadow parts 复杂，见子任务 |
| `ion-page`/`ion-content`/`ion-list`/`ion-item`/`ion-label`/`ion-note`/`ion-text` | — | 保留（结构容器/无对应能力） |
| `ion-action-sheet`/`ion-alert`/`ion-modal`/`ion-fab` | — | 保留（叠层/FAB 宿主） |

## Children

1. `07-24-replace-ionic-low-risk` — 低风险替换：HProgress + HCard + 文字按钮 HButton + 列表/导航栏 icon-only HIconButton。
2. `07-24-replace-player-range` — PlayerPage `ion-range` → `HRange` 评估与实现（能补齐手势/缓冲则替换，否则登记 gap 保留）。依赖 child 1 先落地（避免同文件冲突：PlayerPage icon 控件保留，range 单独处理）。
3. `07-24-sync-spec-whitelist` — 同步 spec 白名单表：progress-bar/card/icon-only 从"库缺口"移出、HIconButton 从"不得恢复"改为推荐；range 结论随 child 2 落定后更新。建议最后执行。

## Cross-child Acceptance Criteria

- [ ] 三个子任务各自 build + unit test 通过、无回归。
- [ ] 替换后 SourcesPage、各列表页、MiniPlayer 视觉与交互与替换前一致（图标、危险色、禁用态、点击行为）。
- [ ] spec `component-guidelines.md` 白名单与实际代码一致：已替换项不再标"库缺口"，仍保留的（PlayerPage 沉浸控件等）明确记为缺口并说明原因。
- [ ] 无新增硬编码主色/圆角/elevation；图标统一 `@lucide/vue` + `HIcon`/`HIconButton`。

## Notes

- 父任务不直接实现，只拥有源需求、子任务映射与最终集成审查。
- child 2 的 range 若评估为不可无损替换，保留 `ion-range` 并在 gap 记录理由，不强行替换牺牲续播手势正确性。
