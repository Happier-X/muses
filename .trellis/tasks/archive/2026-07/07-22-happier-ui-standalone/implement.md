# 执行计划：独立 happier-ui

## 前置

1. 用户审阅 prd/design/implement（本轮已「可以」）
2. `task.py start` 后执行
3. 确认 `C:\code\happier-ui` 尚不存在或可覆盖策略

## 批次

### 1 — 创建独立库

- [x] `mkdir C:\code\happier-ui` + `git init`
- [x] 拷贝 tokens、H* 组件、package.json、README
- [x] 调整 exports / peer；`.gitignore`
- [x] playground（smoke）

### 2 — 独立库可运行

- [x] happier-ui `npm install`
- [x] playground build 绿

### 3 — Muses 接线

- [x] `happier-ui`: `file:../happier-ui`
- [x] 删除 `packages/happier-ui`、`apps/happier-ui-smoke`、workspaces
- [x] vite/tsconfig 别名指向 `../happier-ui`
- [x] `npm install` + lint/build/unit

### 4 — 文档

- [x] 独立库 README
- [x] Muses DESIGN/spec
- [x] 可选 AGENTS 未改

### 5 — 验证

- [x] 独立仓初稿 commit `968ae23`
- [x] Muses lint/build/unit 311 绿

## 建议 commit（仅 muses 仓）

1. `refactor(deps): happier-ui 改为仓外 file:../happier-ui`
2. `docs: 独立 happier-ui 与逐个替换说明`

独立仓单独 `git commit`。

## 回滚

见 design §7。
