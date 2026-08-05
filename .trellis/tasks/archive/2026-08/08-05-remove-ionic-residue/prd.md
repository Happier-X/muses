# 清除 Ionic 脚手架与代码残留

## Goal

在业务已迁至 happier-ui + Capacitor 的前提下，清理仓库中仍可识别的 **Ionic UI 脚手架 / 死配置 / 误导性命名与注释**，避免后续误以为仍依赖 `@ionic/vue`。

## Decisions locked

| # | 决策 | 选择 |
|---|------|------|
| D1 | 清理深度 | **A 运行时/脚手架**：删 config、scripts、标题、vite 死分支；`src` + 根构建配置误导注释改中性；**不动** `changelog/**` |

## Background（仓库已核实）

### 已不存在（无需再卸依赖）

- `package.json` **无** `@ionic/vue` / `@ionic/core` / `ionicons` 直接依赖
- `src/main.ts` **无** `IonicVue` 注册
- `src` 内 **无** `<ion-*>` 组件模板、`IonPage` 等运行时引用
- 图标已为 `@lucide/vue` + `HIcon`

### 本任务清理清单（D1=A）

| 位置 | 动作 |
|------|------|
| `ionic.config.json` | 删除 |
| `package.json` `ionic:build` / `ionic:serve` | 删除 |
| `index.html` title / apple-mobile-web-app-title | 改为 `Muses` |
| `vite.config.ts` manualChunks `@ionic/*` / `ionicons` | 删除死分支；注释中性化 |
| `src/theme/tailwind.css`、`variables.css`、`PlayerPage.vue` 等注释 | 改写为中性说明（**保留 CSS 行为**） |
| `changelog/**` | **不改** |

### 明确保留

- **Capacitor** 全套（`@capacitor/*`、`capacitor.config.ts`、Android）
- 高度链 / body 字体等布局 CSS（只改注释）
- happier-ui 与 tokens

## Requirements

1. 删除 `ionic.config.json`。
2. 删除 `package.json` 中 `ionic:*` scripts。
3. `index.html` 应用展示名改为 `Muses`（`title` + `apple-mobile-web-app-title`）。
4. 清理 `vite.config.ts` 中 Ionic 相关 manualChunks 与过时注释；保持 build 可用。
5. 活跃源码与根构建配置中，将「仍依赖 Ionic 运行时 / 仍在用 ion-range」类误导注释改为中性表述；不改业务逻辑与样式行为。
6. **不**卸载 Capacitor；**不**改业务 UI/交互；**不**改 `changelog/**`。
7. `npm run lint` + `npm run build` 通过。

## Out of Scope

- 重写 `changelog/**` 历史发版说明
- 强制全仓（含 changelog）零 `ionic` 字符串
- Capacitor / Android 原生层迁移
- happier-ui / 设计 token 大改
- 改 `appId` / 包名

## Acceptance Criteria

- [x] AC1：仓库工作区无 `ionic.config.json`
- [x] AC2：`package.json` 无 `ionic:` 脚本；无 `@ionic/*` / `ionicons` 直接依赖
- [x] AC3：`index.html` 无 “Ionic App” 文案
- [x] AC4：`vite.config.ts` 无 `@ionic` / `ionicons` chunk 分支
- [x] AC5：`src/**` + 根构建配置无「仍依赖 Ionic 运行时」的误导表述（changelog 历史叙述允许）
- [x] AC6：`npm run lint` + `npm run build` 通过；`capacitor.config.ts` 仍在

## Notes

- 轻量任务：PRD + 简短 `implement.md` 即可；无需父子任务。
