# S2：仓内 monorepo 抽取 happier-ui

## Goal

在 **不 npm 公网发布、不引入第二应用** 的前提下，把 S1 已硬化的 **通用 token + 语义组件** 抽到仓内包 **`happier-ui`**，Muses 通过 **npm workspaces** 依赖本地包；应用行为与视觉不回归。为后续多 Capacitor 项目复用铺路。

## Architecture decision（继承并锁定）

| 项 | 决策 |
|----|------|
| 包名 | **`happier-ui`**（`packages/happier-ui`） |
| 工具 | 根目录 **npm workspaces**（`"workspaces": ["packages/*"]`）；当前 lockfile 已出现 workspaces 字段痕迹，以根 `package.json` 显式配置为准 |
| 实现 | **纯 Vue 优先**；包 peer 仅 `vue`；**不** peer `@ionic/vue` 为硬依赖（过渡期若仍用 `ion-icon`，peerOptional 或 app 侧注入，design 定案） |
| Token | 包内权威 **`--h-*`**；Muses 可继续 `--muses-*` 别名（放在 app theme 或包 re-export） |
| 视觉 | 首版仍参照 HeroUI Native 节奏；无 RN / heroui-native 依赖 |
| 禁止 | 整库复刻 Ionic；把 Cover/Player/业务塞进包；本任务 npm publish |

## Background

- 已完成：`muses-ui-expand`（M* 迁移）、`cap-ui-package` S0+S1（`--h-*`、纯 Vue ListRow/IconButton/SettingRow）
- 现状：组件仍在 `src/components/ui/`，token 在 `src/theme/tokens.css`
- 根项目现为单包 `muses`；需引入 monorepo 而不打断 Ionic/Capacitor/Android 构建

## Scope

### In scope

| 交付 | 说明 |
|------|------|
| `packages/happier-ui` | package.json、exports、源码入口、tokens.css |
| 迁入组件 | EmptyState、IconButton、ListRow、SettingRow（及包内必要类型） |
| Muses 接入 | 依赖 `happier-ui`；`main`/主题/import 路径切换；别名或 re-export 保兼容 |
| 留 app | `MCover`、`MPage`（HOST-IONIC）、MiniPlayer 等 |
| Spec | directory-structure / component-guidelines 更新 monorepo 路径 |
| 验证 | lint / build / unit 全绿 |

### Out of scope

- `npm publish` / 改名 scope 上架
- 第二 Capacitor 应用冒烟（S3）
- 去掉 `ion-icon` 换纯 SVG（可记 TECHDEBT，非本任务必达）
- PlayerPage 大拆、Storybook

## Open decisions（实现前可默认）

- [x] 默认：包源码用 **Vite/TS 源码直引**（workspace 链到 `packages/happier-ui/src`），不做复杂 lib 预构建（降低摩擦）
- [ ] 若 `ion-icon` 仍在 IconButton：peerOptional `@ionic/vue` **或** 图标改 slot-only（推荐实现时尽量 slot/component 注入，减少 peer）
- [ ] app 侧是否保留 `@/components/ui` **薄 re-export**（推荐：是，减少一次改全仓 import）

## Acceptance Criteria

- [ ] 存在 `packages/happier-ui` 且根 workspaces 可安装
- [ ] 包导出 tokens + 通用组件；**无** `@/features`、无业务模块
- [ ] Muses 构建与单测通过；列表/设置/空态仍可用
- [ ] `MCover` / `MPage` 仍在 app（或明确仅 re-export 本地）
- [ ] frontend spec 描述 monorepo 与 import 约定
- [ ] 无 npm publish、无 heroui-native

## Notes

- 复杂任务：需 design.md + implement.md，审阅后 `task.py start`
- 依赖归档任务：`archive/.../07-22-cap-ui-package`
