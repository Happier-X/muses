# 执行计划：happier-ui monorepo S2

## 前置

1. 用户审阅 prd + design + implement
2. `task.py start` 后再改代码
3. 读 `.trellis/spec/frontend/*` 与现 `src/components/ui/*`、`tokens.css`

## 批次

### 1 — 脚手架

- [x] 根 `package.json` 增加 `workspaces: ["packages/*"]`
- [x] 创建 `packages/happier-ui/package.json`（exports、peer vue）
- [x] 根依赖 `happier-ui`（`file:packages/happier-ui`）
- [x] `npm install` 成功

### 2 — 迁 token

- [x] `tokens.css` → `packages/happier-ui/src/tokens.css`
- [x] app `src/theme/tokens.css` 仅 `@import`
- [x] 删除 app 内重复权威定义

### 3 — 迁通用组件

- [x] HEmptyState / HIconButton / HSettingRow / HListRow + M* 别名导出
- [x] ListRow 无 MCover；app `MListRow` 包装 coverSrc
- [x] IconButton optional peer `@ionic/vue` + slot
- [x] `packages/happier-ui/src/index.ts`

### 4 — App 接线

- [x] `src/components/ui` re-export + MCover/MPage/MListRow
- [x] views 仍 `@/components/ui`
- [x] vite/tsconfig 别名

### 5 — 工具与文档

- [x] eslint `.` 已覆盖 packages（flat 默认）
- [x] frontend spec + DESIGN
- [x] 包 README

### 6 — 验证

- [x] lint / build / unit 311
- [ ] 目视：Songs / Queue / Settings / MiniPlayer

## 建议 commit

1. `chore: 引入 packages/happier-ui workspace 脚手架`
2. `refactor(ui): 将 token 与通用组件迁入 happier-ui`
3. `docs(spec): monorepo 与 happier-ui 包边界`

## 回滚

按 commit revert；优先保证 app 可 build。
