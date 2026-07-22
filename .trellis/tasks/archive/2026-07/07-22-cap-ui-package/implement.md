# 执行计划：happier-ui S0+S1

## 前置（强制）

1. **收尾** `07-22-muses-ui-expand`：`trellis-check` → 提交 → 建议 `/trellis:finish-work` 或 archive  
2. 用户审阅本任务 prd/design/implement，回复规划 OK  
3. `task.py start 07-22-cap-ui-package` 后再改 S1 代码  

## S0 清单（文档，可在 start 前完成）

- [x] 锁定包名 `happier-ui`
- [x] 锁定纯 Vue、`--h-*`、范围 S0+S1
- [x] design 边界表与 token 方案 A
- [ ] start 前再确认 expand 已入库的 ui 文件列表与 v0.1 表一致

## S1 清单（代码）

### Token

- [x] 将权威变量迁为 `--h-*`（HeroUI Native 角色 + primary `#006FEE`）
- [x] 语义映射写入 tokens 注释 + design/spec（accent/surface/muted/separator）
- [x] 保留 `--muses-*: var(--h-*)` 兼容别名（方案 A）
- [x] `variables.css` 桥接读 `--h-*`

### 组件硬化 + 首版样式（HeroUI Native 形貌）

- [x] 审计 `src/components/ui/**`：无 `@/features`
- [x] `MIconButton`：纯 button + props.icon；无 ion-lucide import
- [x] `MEmptyState` / `MSettingRow` / `MListRow`：纯 Vue（ListRow 用 div 避嵌套 button）
- [x] `MPage`：HOST-IONIC 注释保留 ion-page
- [x] `MCover`：app-only 标注
- [x] **不**添加 heroui-native / RN 依赖

### 文档

- [x] `.trellis/spec/frontend/*` + DESIGN 轻量同步
- [ ] 可选：`docs/happier-ui.md`（未做）

### 验证

- [x] `npm run lint` / `build` / `test:unit -- --run`（311）
- [ ] 抽查 Songs / Settings / MiniPlayer 无视觉崩坏（需目视）

## 明确不做（本任务）

- [ ] `packages/` monorepo（S2）
- [ ] npm publish

## 建议 commit（S1）

1. `refactor(theme): token 权威前缀迁为 --h-* 并保留 muses 别名`
2. `refactor(ui): 硬化 happier-ui 边界（去 features / 纯 Vue 可行组件）`
3. `docs(spec): 记录 happier-ui 与 --h-* 约定`

## 回滚

alias 保留使视图层可暂不改；组件实现可分文件 revert。
