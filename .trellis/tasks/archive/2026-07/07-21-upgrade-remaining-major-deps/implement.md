# 执行计划

1. 基线：记录 outdated 清单与当前 lint/build/unit 状态。
2. 升级 TanStack Virtual 等小版本并验证。
3. 迁移 ESLint flat config 并升级 ESLint 配置链，验证 lint。
4. 尝试 TypeScript 7 + vue-tsc 3，验证 build + unit。
5. 复核 Vue Router 5 与 `@ionic/vue-router` peer，决定升级或阻塞记录。
6. 最终 `npm ci`、lint、build、full unit、diff-check。
7. 更新 quality-guidelines，提交并关闭相关跟进说明。

## 风险

- flat config 迁移遗漏文件模式导致 lint 覆盖不全。
- TypeScript 7 与现有类型定义不兼容。
- Vue Router 5 破坏 Ionic 路由集成。

## 本次实施记录

### 已完成

- `@tanstack/vue-virtual`：`3.13.32` → `3.13.33`
- ESLint 配置链：
  - `eslint` `8.57.1` → `10.7.0`
  - `eslint-plugin-vue` `9.33.0` → `10.10.0`
  - `@vue/eslint-config-typescript` `12.0.0` → `14.9.0`
- 新增 `eslint.config.js` flat config（`withVueTs` + `vueTsConfigs.recommended`）
- 删除 `.eslintrc.cjs`、`.eslintignore`，将忽略规则迁入 flat config
- 保留原规则意图：生产 `no-console`/`no-debugger` warn、关闭 slot 弃用规则与 `no-explicit-any`

### 阻塞项（有证据）

1. **TypeScript 7 + vue-tsc 3**
   - 命令：`npm install -D typescript@^7.0.2 vue-tsc@^3.3.7 && npm run build`
   - 错误：`ERR_PACKAGE_PATH_NOT_EXPORTED: Package subpath './lib/tsc' is not defined by "exports" in typescript/package.json`
   - 结论：当前 `vue-tsc@3.3.7` 仍访问 TypeScript 未导出路径，回滚到 TypeScript 5.9.3 + vue-tsc 2.2.12

2. **Vue Router 5**
   - `@ionic/vue-router@8.8.14` 依赖 `vue-router@^4.5.0`
   - 结论：在 Ionic Vue Router 放宽 peer 前不可安全升级

### 验证结果

- `npm ci`：通过
- `npm run lint`：通过
- `npm run build`：通过
- `npm run test:unit -- --run`：通过（15 文件 / 261 用例）
- `git diff --check`：通过
- `npm outdated` 现仅剩 TypeScript / vue-tsc / Vue Router
