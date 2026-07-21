# 升级剩余主版本依赖

## 目标

在不破坏现有构建、测试和 Ionic/Capacitor 兼容的前提下，尽量升级上一轮遗留的主版本依赖，并同步可安全更新的小版本。

## 已确认事实

- 当前 `npm outdated` 主要剩余：
  - ESLint 8 → 10，以及 `@vue/eslint-config-typescript` 12 → 14、`eslint-plugin-vue` 9 → 10
  - TypeScript 5.9 → 7，以及 `vue-tsc` 2 → 3
  - Vue Router 4 → 5
  - `@tanstack/vue-virtual` 3.13.32 → 3.13.33（小版本）
- 路由使用 `@ionic/vue-router` 的 `createRouter`，业务页使用 `useRouter` / `useRoute`。
- `@ionic/vue-router@8.8.14` 锁定依赖 `vue-router@^4.5.0`，因此 **Vue Router 5 目前不可安全升级**。
- ESLint 当前为 `.eslintrc.cjs`，升到 ESLint 9/10 与 `@vue/eslint-config-typescript@14` 需要 flat config。
- 上一轮 TypeScript 7 + vue-tsc 3 曾因 `typescript/lib/tsc` 导出与 ESLint 配置链失败；需重新在隔离组验证最新组合。
- 不修改 `node_modules` 源码，不改业务功能。

## 需求

- 尝试并尽可能完成：
  1. ESLint flat config 迁移 + ESLint 10 配置链
  2. TypeScript 7 + vue-tsc 3（若构建与 lint 可通）
  3. 安全小版本（如 TanStack Virtual）
- Vue Router 5 若被 Ionic 锁死，则明确记录为阻塞，不强行破坏 Ionic 路由。
- 每组升级后运行 `npm ci`、`npm run lint`、`npm run build`、`npm run test:unit -- --run`、`git diff --check`。
- 失败组回滚并写入证据；成功组保留。

## 验收标准

- [x] 所有能通过兼容验证的剩余主版本已升级。
- [x] 无法升级的主版本有 peer/引擎/迁移证据。
- [x] lint、build、完整 unit test、diff check 通过。
- [x] 无业务功能改动，无敏感信息泄露。

## 范围外

- 不升级 Capacitor 到下一主版本。
- 不发布新版本。
- 不重写路由架构或业务页面。
