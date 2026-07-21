# 技术设计

## 升级顺序

1. **小版本**：`@tanstack/vue-virtual` 等到 latest。
2. **ESLint 组**：安装 ESLint 10 + eslint-plugin-vue 10 + `@vue/eslint-config-typescript` 14，新增 `eslint.config.js` flat config，删除或停用 `.eslintrc.cjs`，调整 `npm run lint`。
3. **TypeScript 组**：在 ESLint 组稳定后升级 TypeScript 7 + vue-tsc 3，修复构建/类型问题；失败则回滚本组。
4. **Vue Router 5**：先检查 `@ionic/vue-router` 是否支持 `vue-router@5`。当前 8.8.14 依赖 `vue-router@^4.5.0`，默认阻塞，除非官方新版本放宽 peer。

## 配置迁移要点

### ESLint flat config

- 使用 `@vue/eslint-config-typescript` 14 导出的 flat 预设。
- 保留现有规则意图：
  - 生产环境 `no-console` / `no-debugger` warn
  - 关闭 `vue/no-deprecated-slot-attribute`
  - 关闭 `@typescript-eslint/no-explicit-any`
- 确保 `.vue`、`.ts`、测试文件可被 lint。

### TypeScript / vue-tsc

- `package.json` build 脚本继续 `vue-tsc && vite build`。
- 若 vue-tsc 3 需要额外 CLI 参数或 tsconfig 调整，最小化修改。
- 不得为通过构建而禁用严格类型检查。

## 回滚

每组独立；失败仅回滚该组 `package.json` / lockfile / 配置文件，不回退已验证组。
