# S3：第二 Capacitor 应用冒烟验证 happier-ui

## Goal

在 **不 npm 公网发布** 的前提下，用 **第二个最小 Capacitor + Vue 应用** 接入仓内 `happier-ui`，证明 token 与通用组件可 **跨宿主复用**（非 Muses 业务页），并留下可重复的冒烟步骤。

## Architecture decision（默认锁定，可改）

| 项 | 决策 |
|----|------|
| 冒烟应用位置 | 仓内 **`apps/happier-ui-smoke`**（与 `packages/happier-ui` 并列，根 workspaces 扩展为 `packages/*` + `apps/*`） |
| 宿主形态 | **Vite + Vue 3 + Capacitor 8** 最小壳；**可不装完整 Ionic 路由栈** |
| 图标策略 | 冒烟优先 **`HIconButton` 默认 slot + 内联 SVG**，避免冒烟应用硬依赖 `@ionic/vue`；文档注明 path+`ion-icon` 路径仍需 Ionic |
| 依赖 | 冒烟 app 依赖 `happier-ui`（`file:../../packages/happier-ui` 或 workspace） |
| 验证 | `npm run build`（冒烟 app）通过；可选 `npx cap sync` 不强制出 APK |
| 禁止 | 把 Muses 业务/Cover/Player 拷进冒烟 app；本任务 npm publish |

## Background

- S0–S2 已完成：`--h-*`、纯 Vue 通用组件、仓内 monorepo `packages/happier-ui`
- 尚未证明「第二项目」能独立 import 并渲染
- Capacitor 本身无 UI；冒烟重点是 **Web 层组件 + token**，Capacitor 配置证明可进 WebView 交付链

## Scope

### In scope

| 交付 | 说明 |
|------|------|
| `apps/happier-ui-smoke` | 最小页面：EmptyState、ListRow、SettingRow、IconButton、tokens |
| workspaces | 根 `package.json` 纳入 `apps/*`（若需要） |
| 文档 | 包 README 或 `apps/happier-ui-smoke/README.md`：如何启动/构建 |
| 验证 | 冒烟 app lint/build（及根/包无回归） |

### Out of scope

- 上架 npm、改 scope 名
- 完整第二音乐产品、Play 商店包
- 强制 Android 真机安装（可选加分）
- 去掉 Muses 内 `ion-icon` 过渡（可另开）

## Open decisions（请确认）

- [x] 默认路径：`apps/happier-ui-smoke`（相对 `examples/` 更像可交付 app）
- [x] 默认不用完整 Ionic Tab 壳，降低噪声
- [ ] 是否本任务执行 `cap add android` + `cap sync`（建议：**做 web build + capacitor config**；android 目录可选精简）

## Acceptance Criteria

- [ ] 存在第二应用目录，**不** import Muses `@/` 业务
- [ ] 成功 `import 'happier-ui/tokens.css'` 与 `H*` 组件并渲染 ≥3 种组件
- [ ] 冒烟 app `build` 通过
- [ ] README 写明启动命令
- [ ] 根 Muses `lint/build` 或至少既有单测不因 workspaces 改动而挂（实现时跑）
- [ ] 无 npm publish

## Notes

- 复杂任务：design + implement，审阅后 `task.py start`
- 继承：`archive/.../07-22-happier-ui-monorepo`
