# 跨项目 Capacitor UI 包：边界与逐步抽取

## Goal

把 Muses 内已 dedupe 的 **语义 UI + token** 演进为可在 **其他 Capacitor + Vue 项目** 复用的组件库；本任务交付 **S0 边界锁定 + S1 边界硬化**，为后续仓内 `happier-ui` 包（S2）铺路。**禁止**整库复刻 Ionic。

## Architecture decision（已锁定）

| 项 | 决策 |
|----|------|
| 包名 | **`happier-ui`**（npm 名；导入可写作 `happier-ui`，若需 scope 再用 `@happier/ui` 别名，默认无强制 scope） |
| v1 实现 | **纯 Vue 优先**：包 **不** peer 依赖 `@ionic/vue`；Ionic 仅留在 Muses 适配层 / 业务页 |
| Token 前缀 | **`--h-*`**（用户约定 `h-xxx`）；Muses 过渡期可用 alias 把 `--h-*` 映射到现有 `--muses-*` 或反向，避免一次视觉回归 |
| 首版样式 | **照抄 HeroUI Native（移动端）** 默认主题与组件视觉节奏；纯 Vue + CSS/`--h-*` 复现，**不**引入 `heroui-native` / React Native |
| 本任务范围 | **S0 + S1**：文档锁定 + ui 硬化 + **按 HeroUI Native 校准首版外观**；**不含 S2 monorepo** |
| 复用目标 | 多 Capacitor WebView + Vue 的壳与原语，非音乐业务全集 |
| 禁止 | 全量自研第二套 Ionic；`MIon*` 1:1 镜像；把 RN 依赖带进包 |

## Background

- 已有 / 进行中：`tokens`、`MEmptyState` / `MCover` / `MPage`、`MIconButton` / `MListRow` / `MSettingRow`（`07-22-muses-ui-expand`）
- **依赖顺序（强制）**：先 **check + commit + 建议 archive** `muses-ui-expand`，再对本任务 `task.py start` 做 S1，避免双线改 `src/components/ui`

## Scope

### S0 — 规划（本任务）

- [x] 包名、Ionic 策略、token 前缀、范围锁定（本轮用户确认）
- [ ] v0.1 进包 / 留 app 表定稿于 design（实现前再扫一眼 expand 最终文件）

### S1 — 边界硬化（本任务代码，start 后）

- 审计 `src/components/ui/*`：禁止依赖 `@/features/*`
- 图标：组件只收 **icon 数据 / slot**，不 import `ion-lucide` 映射表进「未来包心」
- Page / ListRow 等：规划 **去 Ionic 实现** 的路径；S1 以「可抽、无业务依赖」为主，**若去 ion 改动面过大可分两步**（先去 features/硬编码，再换纯 Vue 实现）——以 implement 为准
- token：引入 `--h-*` 定义源 + Muses 兼容 alias（见 design）
- **视觉首版**：对照 HeroUI Native theming/colors，校准 token 与 IconButton/ListRow/SettingRow/EmptyState 形貌；与 PRODUCT「非 Material / 暗场听席」冲突时 PRODUCT 优先
- frontend spec 记录 `happier-ui` 与 HeroUI Native 参照

### S2 — 仓内 package（**非本任务默认**）

- `packages/happier-ui` 或 `packages/ui` + workspace
- Muses 改 import

### Out of scope

- npm 公网发布、第二真实项目、Storybook
- MiniPlayer / Player / Cover 进通用包
- 未完成 expand 前并行大改 API

## Acceptance Criteria

- [x] 开放决策锁定：包名 `happier-ui`、纯 Vue、`--h-*`、S0+S1、首版样式=HeroUI Native
- [ ] 书面边界：进包 / app 专属 / 禁止事项（design）
- [ ] v0.1 清单与阶段路径可执行
- [ ] 与 `muses-ui-expand` 先后顺序写清
- [ ] S1 后：`ui` 无 `features` 依赖；图标可注入；`--h-*` 与 Muses 兼容
- [ ] S1 后：lint / build / 相关单测通过
- [ ] frontend spec 更新抽取约定
- [ ] **不**在本任务默认完成 monorepo（S2）

## Notes

- 审阅 prd/design/implement 后再 start S1
- expand 未提交代码应先收尾
