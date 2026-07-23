# 全量改用 happier-ui 组件库

## Goal

将 Muses 业务 UI 从 Ionic 直用 / 旧 `@/components/ui` 别名，迁移到已发布的 **`happier-ui`** 语义组件与 `--h-*` token，在保留必要宿主能力的前提下完成全量替换（方案 A）。

## 背景（已核实）

### 依赖与发布

- npm 包：`happier-ui@0.0.1`（`latest`，2026-07-23 发布，`https://github.com/Happier-X/happier-ui`）
- peer：`vue ^3.5.0`、`@lucide/vue ^1.25.0`
- 导出：`HButton`、`HSwitch`、`HBottomSheet`、`HDialog`、`HInput`、`HCheckbox`、`HEmpty`、`HImage`、`HIcon`、`HTabBar`、`HNavBar`；`happier-ui/style.css`、`happier-ui/tokens.css`
- Muses 当前仍：`happier-ui: file:../happier-ui`，且 `vite.config.ts` / `tsconfig.json` 把包 **alias 到源码** `../happier-ui/src/...`
- Muses 已有 `lucide`，**尚未**声明 peer `@lucide/vue`

### 旧桥接与 API 断裂

- `src/components/ui/index.ts` 仍 re-export **已不在库中的** 名称：`HEmptyState`、`HIconButton`、`HListRow`、`HSettingRow` 及 `MEmptyState` / `MIconButton` / `MSettingRow`
- 现网仍消费这些别名：`SongsPage`、`QueuePage`、`PlaylistDetailPage`、`SettingsPage`、`AlbumsPage`、`ArtistsPage`、`SourcesPage`、`MiniPlayer` 等
- `MListRow` 依赖库内 `HListRow`（新库已无此导出）
- 新库命名：`HEmpty`（非 `HEmptyState`）、`HButton` + `HIcon`（非 `HIconButton`）、无 `HListRow` / `HSettingRow`

### Ionic 使用热点（约）

| 区域 | 相对热度 | 备注 |
|------|----------|------|
| `SourcesPage` | 最高 | list/item/input/card/modal/alert/action-sheet/checkbox |
| `PlayerPage` | 高 | `ion-button` / `ion-range` / 沉浸控制 |
| 列表页（Songs/Playlists/Albums/Artists/Queue） | 中 | `ion-list`/`ion-item` + 部分已用 M* |
| `TabsPage` / `MPage` / shell | 中 | `ion-page`/`header`/`toolbar`/`tab` 宿主壳 |
| `SettingsPage` | 中 | `MSettingRow` + Ionic 混用 |

### 既有产品约束

- 交付：`product` + `android`；非 Material；列表克制，沉浸页为听歌主舞台
- 通用组件进 `happier-ui`；**不进库**：`MCover`、MiniPlayer、Player 业务手势/AMLL、WebDAV 逻辑、`ion-page` 路由壳、Alert/ActionSheet/Modal **引擎**（历史路线图共识，待本任务最终确认）
- 主色仍对齐 HeroUI primary `#006FEE`（经 token / Ionic 桥）

## Requirements

### R1 — 依赖接入（已决：纯 npm）

- `package.json` 使用已发布的 `happier-ui@0.0.1`（或等价针定），**不再**使用 `file:../happier-ui`
- 移除 `vite.config.ts` / `tsconfig.json` 中指向 `../happier-ui/src` 的 alias；以 npm 包 `exports` / `dist` 为准
- 正确导入 `happier-ui/style.css` 与 `happier-ui/tokens.css`
- 安装 peer `@lucide/vue`（与库 `HIcon` 一致）

### R2 — 接入层与命名

- 重建 `src/components/ui`（或等价边界）：只 re-export / 薄包装 **新库真实导出**
- 删除对已消失 API（`HEmptyState`、`HIconButton`、`HListRow`、`HSettingRow` 等）的编译依赖；相关现有 UI 若无 0.0.1 对应组件，则保留/恢复 Ionic 实现，不在 Muses 自研替代品
- 业务页统一优先使用 `happier-ui` 已有通用组件；仅在库无对应能力时继续保留 Ionic
- 禁止为了追求替换率而在 Muses 新建通用 `MListRow` / `MSettingRow` / `MIconButton` 等平行组件

### R3 — 全量页面替换（方案 A）

在可验收范围内，业务 UI 尽量改为 happier-ui：

| 页面/组件 | 替换方向（初稿） |
|-----------|------------------|
| Songs / Queue / PlaylistDetail / Playlists / Albums / Artists | `HEmpty`、`HImage` 等已有能力直接替换；列表行、专用图标按钮因库缺口保留现状并登记 |
| Settings | 开关 → `HSwitch`；设置行因库缺口保留现状并登记 |
| Sources | 表单 → `HInput`/`HCheckbox`/`HButton`；空态 → `HEmpty`；Modal/Alert/ActionSheet 引擎保留 Ionic |
| TabsPage | 底栏视觉 → `HTabBar`（路由仍由 Ionic 宿主） |
| 页头 | 可用 `HNavBar` 替换 toolbar 视觉层；`ion-page` 路由壳保留 |
| MiniPlayer | 仅替换 0.0.1 已有且 API 对应的控件；业务逻辑及缺口控件保留并登记 |
| PlayerPage | 仅替换已有对应能力；`ion-range` 与缺口控件保留并登记 |

### R4 — 明确不迁或后置

- 音乐领域：`MCover`、播放队列语义、WebDAV/扫描业务
- Ionic/Capacitor 宿主：路由栈、手势返回、StatusBar 等
- **库中不存在的组件本次不替换**：保留当前 Ionic / 业务实现，不在 Muses 新造通用替代品
- 所有缺口必须整理成清单，至少记录：候选 happier-ui 组件名、当前 Ionic/业务落点、所需 API/状态、优先级、阻塞的替换范围；以后在 happier-ui 仓库独立开发、发布后再迁移

### R5 — 质量门槛

- `npm run lint` / `npm run build`（含 `vue-tsc`）通过
- 主路径可手测：列表浏览、播放/队列、设置、音源关键流程不回归
- 视觉仍符合 `PRODUCT.md` / `DESIGN.md`（暗场听席、flat、非 Material）

## Acceptance Criteria

- [ ] Muses 依赖策略落地（npm 版本 / 本地联调约定写清）
- [ ] 入口正确加载 happier-ui 样式与 token；peer `@lucide/vue` 就绪
- [ ] 无对旧导出名（`HEmptyState` / `HIconButton` / `HListRow` / `HSettingRow` 等）的编译依赖
- [ ] happier-ui 0.0.1 **已有对应组件**的业务控件完成替换；无对应组件的 Ionic / 业务实现按缺口清单保留
- [ ] 业务代码不再使用 `ion-icon` / `@/icons/ion-lucide`；统一 `@lucide/vue` + `HIcon`（Ionic/Capacitor 内部依赖除外）
- [ ] app-only / Ionic 宿主 / 库缺口边界文档化且代码一致
- [ ] 形成可供后续 happier-ui 开发的缺口清单（组件名、落点、API、优先级、阻塞范围）
- [ ] lint + typecheck/build 通过；约定主路径手测通过

## 已决

| 项 | 决定 |
|------|------|
| 替换范围 | **方案 A 全量替换** |
| 依赖源 | **纯 npm** `happier-ui@0.0.1`；去 `file:` 与源码 alias |
| 库发布 | 已发布 npm，以 **0.0.1 导出** 为准 |
| Ionic 宿主边界 | **保守宿主**：保留 `ion-page` 路由壳、Modal/Alert/ActionSheet 引擎与 `ion-range`；替换其余已有对应组件的可见业务控件 |
| 库缺口 | **本次不替换、不在 Muses 补造**；完整记录，后续去 happier-ui 开发并发版 |
| 图标体系 | **全面迁移**：业务侧从 `ion-icon` + `ion-lucide` 迁到 `@lucide/vue` + `HIcon`；周围仍为 Ionic 的控件也执行 |

## 待决（规划中）

无。规划产物：`design.md`、`implement.md`、`gaps.md`、`implement.jsonl`、`check.jsonl` 已就绪，待用户审阅后 `task.py start`。

## Notes

- 复杂任务：本 PRD 之外需 `design.md` + `implement.md`，再 `task.py start`
- 历史归档：`07-22-happier-ui-component-roadmap` / `standalone` 等；库侧组件已演进，**以 npm 0.0.1 导出为准**
- 用户选择：**方案 A 全量替换**；**依赖纯 npm**；库已发布
- Ionic 宿主边界：**保守宿主**——保留 `ion-page` 路由壳、Modal/Alert/ActionSheet 引擎和 `ion-range`
- 库缺口策略：**没有的先不替换**，全部整理记录，未来在 happier-ui 仓库开发
- 本任务在 Muses 侧产出 `gaps.md` 作为真实落点清单；本轮不跨仓创建/开发 happier-ui 任务，未来进入 happier-ui 时据此转录
- 图标选择：**B 全面迁移**；保留语义映射（队列 `ListMusic`、顺序播放 `ListOrdered`、专辑 `Disc3`、艺术家 `MicVocal`、音源 `Folder`、字幕开关 `Captions/CaptionsOff`），但改为 Vue 图标组件 + `HIcon`
