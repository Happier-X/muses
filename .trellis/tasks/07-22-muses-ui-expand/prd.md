# 扩展 Muses UI 语义组件库

## Goal

在 **不整库复刻 Ionic** 的前提下，把 `src/components/ui` 按 **使用频率与产品语义** 逐个扩展：业务页优先 `M*` + token，Ionic 仅作内部实现；视觉对齐「暗场听席、flat、HeroUI primary 克制、非 Material」。

## Architecture decision（已锁定）

- **主路径**：Ionic 上的 **薄语义二次封装**
- **禁止**：`MIonButton` / `MIonItem` 等 **1:1 同名镜像**
- **禁止**：本任务 **全量自建** 导航栈 / Modal / Tab 引擎
- **允许**：主舞台或 Ionic 调不动处 **局部自建**
- **风格**：PRODUCT/DESIGN——沉静可靠、列表干脆、沉浸柔和；无 MD elevation；主色 `#006FEE` 仅状态/主操作
- **触控**：关键可点目标尽量 ≥48dp 量级（Android 交付），但不引入 Material 组件美学
- **依赖**：`tokens.css` 唯一视觉数值源

## Scope（已锁定）

- **迁移**：Queue + Settings + 列表行统一 + IconButton
- **列表行**：抽 **`MListRow` 组件**（非仅 CSS 类）
- **必做**：`MIconButton`
- **节奏**：**按组件队列逐个交付**（每完成一个即迁移 ≥2 处再进下一个），不是一次堆满目录

### 本轮组件队列（按现网频次与复用价值）

| 顺序 | 组件 | 语义 | 内部可包 | 首迁页面 |
|------|------|------|----------|----------|
| 0 | （已有）`MEmptyState` / `MCover` / `MPage` | 空态 / 封面 / 页壳 | ion 结构 | 保持并复用 |
| 1 | `MIconButton` | 图标触控 | `ion-button` fill=clear | MiniPlayer、列表 more、Queue 移除 |
| 2 | `MListRow` | 曲目/队列行 | `ion-item` 或纯布局 | Songs、Queue、PlaylistDetail |
| 3 | Queue 页整体收敛 | empty+cover+row+token | — | QueuePage |
| 4 | `MSettingRow`（或 `MFormRow`） | 设置行 | `ion-item`+toggle/input | SettingsPage |
| 5 | Spec：「何时直连 ion-*」 | — | — | frontend spec |

### Out of scope

- 为每个 ion 标签做同名 `MIon*`
- 复刻 Modal / ActionSheet / 路由栈
- PlayerPage 大拆
- Storybook / 独立 npm 包

## Acceptance Criteria

- [ ] design/spec 写明：语义封装、禁止 1:1、禁止本任务全量复刻、非 Material
- [ ] `MIconButton` 交付并在 ≥2 处使用
- [ ] `MListRow` 交付并在 ≥2 页面使用（含 playing 态）
- [ ] Queue 完成 empty/cover/行/`M*` 收敛
- [ ] Settings 表单行语义化 + token（不重写表单引擎）
- [ ] 新视觉值走 `--muses-*`；无新增 elevation
- [ ] lint / type-check / 相关单测通过
- [ ] frontend spec 更新 ion 直连边界

## Notes

- 复杂任务：`design.md` + `implement.md`，审阅后 `task.py start`
- 依赖：tokens、现有 `ui/*`、PRODUCT/DESIGN
