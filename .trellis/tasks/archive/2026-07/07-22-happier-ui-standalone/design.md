# 技术设计：独立 happier-ui + Muses 逐个替换

## 1. 目标布局

```text
C:\code\
  muses\                          # 消费方（本仓库）
  happier-ui\                     # 独立库仓库
    package.json                  # name: happier-ui
    src/
      index.ts
      tokens.css
      components/H*.vue
    playground/                   # 可选：原 smoke
    README.md
    .gitignore
```

## 2. 从 Muses 迁出

| 源（muses） | 目标（happier-ui） |
|-------------|-------------------|
| `packages/happier-ui/**` | 库根 `src/` + 根 `package.json` |
| `apps/happier-ui-smoke/**` | `playground/`（改相对路径 alias） |

迁出后 Muses：

- 根 `workspaces` 可保留 `packages/*`（若空则仅 `apps/*` 或去掉 apps smoke）
- 依赖：`"happier-ui": "file:../happier-ui"`
- 删除 `packages/happier-ui`、`apps/happier-ui-smoke`（避免双源）
- `vite`/`tsconfig` 别名指向 `node_modules/happier-ui` 或 `../happier-ui/src`（file 依赖安装后一般无需硬编码）

## 3. 独立库 package.json

```json
{
  "name": "happier-ui",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "exports": {
    ".": "./src/index.ts",
    "./tokens.css": "./src/tokens.css"
  },
  "peerDependencies": { "vue": "^3.5.0" },
  "scripts": {
    "build:playground": "…",
    "dev:playground": "…"
  }
}
```

源码直引（与 S2 一致），不做复杂 lib 打包，除非后续发布需要。

## 4. Muses 替换策略（产品流程）

```text
阶段 A（本任务）：依赖外链 + 删除仓内副本；@/components/ui re-export 仍指向 happier-ui
阶段 B（后续日常）：在 happier-ui 改组件 → Muses npm install / 刷新 → 目视页面
阶段 C：逐步减少 app 包装（MListRow 封面等），新页面直接 import from 'happier-ui'
```

**逐个替换** 不指本任务改所有 views，而指：

1. 库 API 稳定后，页面从 `M*` re-export 可改为具名 `H*`（可选）
2. 领域组件（Cover）永远留 Muses
3. 有回归时只回滚 Muses 依赖版本/路径，库仓独立演进

## 5. Git

- `C:\code\happier-ui`：`git init`，首 commit 初稿
- **不**把独立仓内容塞进 muses 的 git tree（仅 file 依赖）
- Muses 任务只提交：依赖变更、删仓内包、文档

## 6. 风险

| 风险 | 缓解 |
|------|------|
| Windows file: 路径 | 使用 `file:../happier-ui` 相对 muses 根 |
| IDE 跳转 | tsconfig paths 可选 |
| 忘记 install | README 写 `npm i` 两边都要 |
| smoke 路径断裂 | playground 内 alias 相对 `../src` |

## 7. 回滚

Muses 恢复 `file:packages/happier-ui` 并从 git 恢复 packages；独立仓可弃用。
