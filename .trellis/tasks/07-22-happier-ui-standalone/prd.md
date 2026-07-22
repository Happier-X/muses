# 独立仓库 happier-ui 与 Muses 逐个替换

## Goal

在 **`muses` 同级目录** 建立独立项目 **`happier-ui`**，作为跨 Capacitor/Vue 应用的 UI 库开发主战场；**Muses 通过本地 `file:` 依赖接入**，业务页 **逐步** 从仓内 `packages/happier-ui` / `@/components/ui` 迁到独立库导出。本任务完成 **独立库脚手架 + 从现仓拷贝初稿 + Muses 依赖切换策略**；**不**要求一次替换全部页面，**不** npm 公网发布。

## Architecture decision（默认锁定）

| 项 | 决策 |
|----|------|
| 路径 | **`C:\code\happier-ui`**（与 `C:\code\muses` 同级） |
| 版本控制 | **独立 `git init`**（与 muses 仓库分离） |
| 库形态 | 独立仓库内 monorepo 或「根即包」：推荐 **根 package 即 `happier-ui` 库** + 可选 `playground/` 冒烟（从 `apps/happier-ui-smoke` 迁移） |
| 初稿来源 | 拷贝 `muses/packages/happier-ui` 与冒烟应用精简版；可再演进 |
| Muses 本任务 | 依赖改为 **`file:../happier-ui`**（或等价）；**可暂保留** 仓内 `packages/happier-ui` 作对照，或标记 deprecated——实现时二选一写清，**默认：切换依赖后删除仓内副本，避免双源** |
| 替换节奏 | **开发在独立库**；Muses **按组件/页面逐个** 验证，不强制本任务改完所有 views |
| 禁止 | 把 Muses 业务/Cover/Player 塞进独立库；本任务 npm publish |

## Background

- 已完成仓内 S0–S3：`--h-*`、H* 组件、`packages/happier-ui`、`apps/happier-ui-smoke`
- 用户意图：库在 **同级独立项目** 开发，再在 Muses **逐个替换**
- 仓内 monorepo 适合起步；长期双源会漂移，故迁出独立仓

## Scope

### In scope

| 交付 | 说明 |
|------|------|
| `C:\code\happier-ui` | 可 `npm install` / build 的库项目 + README |
| 组件与 tokens | 从现 `packages/happier-ui` 迁入 |
| 可选 playground | 从 smoke 迁入，便于无 Muses 开发 |
| Muses 接线 | `package.json` 依赖指向 `../happier-ui`；import 仍可走 re-export |
| 文档 | 两边 README：如何 link、如何逐个替换 |
| 验证 | 独立库 build；Muses lint/build/unit 不回归 |

### Out of scope

- npm 公网发布 / 改 scoped 名上架
- 一次改完所有 Muses 页面视觉
- 删除 Ionic 应用壳
- 在独立库重做完整设计系统站点

## Open decisions（可默认）

- [x] 路径 `C:\code\happier-ui`
- [x] 独立 git
- [x] 本任务默认 **Muses 改 file 依赖**；仓内 `packages/*` 副本 **删除或改 re-export 代理**（实现推荐：依赖外链后删 `packages/happier-ui`，smoke 可迁到独立仓 playground）
- [ ] playground 放独立仓根下 `playground/` 还是 `apps/smoke/`（默认 `playground/`）

## Acceptance Criteria

- [ ] `C:\code\happier-ui` 存在且为独立 git 仓库（至少 init + 初稿文件）
- [ ] 库可导出 tokens + H* 组件；无 Muses 业务依赖
- [ ] Muses 能通过 `file:../happier-ui` 构建通过
- [ ] 文档说明：库内开发流程 + Muses 逐个替换步骤
- [ ] 无双源冲突策略写清（删仓内包或单一 re-export）
- [ ] 无 npm publish

## Notes

- 复杂任务：design + implement，审阅后 `task.py start`
- 用户确认：可以（默认路径与独立 git）
