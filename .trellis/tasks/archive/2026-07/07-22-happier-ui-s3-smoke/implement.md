# 执行计划：happier-ui S3 冒烟

## 前置

1. 用户审阅 prd/design/implement
2. `task.py start` 后编码
3. 确认 `packages/happier-ui` 可导入

## 批次

### 1 — 脚手架

- [x] 根 workspaces 增加 `apps/*`
- [x] 创建 `apps/happier-ui-smoke`（package.json、vite、tsconfig、index.html）
- [x] 依赖 vue + happier-ui；`npm install`

### 2 — 冒烟 UI

- [x] `main.ts` import `happier-ui/tokens.css`
- [x] `App.vue` 展示 HEmptyState / HListRow / HSettingRow / HIconButton(slot)
- [x] `npm run build:smoke`

### 3 — Capacitor 钩子

- [x] `capacitor.config.ts`
- [x] README：dev / build / 可选 cap add android

### 4 — 文档与回归

- [x] 更新 `packages/happier-ui/README.md` 链到 smoke
- [x] 根 `dev:smoke` / `build:smoke` script
- [x] Muses build + unit 311 + lint
- [x] frontend spec 一句「apps 冒烟」

### 5 — 验证清单

- [x] smoke build 绿
- [x] 至少 3 类 H* 组件在源码中引用
- [x] 无 npm publish、无 Muses 业务依赖

## 建议 commit

1. `chore(apps): 新增 happier-ui-smoke 冒烟应用`
2. `docs: happier-ui S3 跨项目接入说明`

## 回滚

删 `apps/happier-ui-smoke` + 还原 workspaces。
