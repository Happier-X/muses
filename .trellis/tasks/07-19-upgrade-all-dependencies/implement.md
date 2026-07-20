# 执行计划

1. 记录当前依赖清单、Node/npm 版本和基线测试结果。
2. 更新同主版本依赖到 latest，执行 npm install/ci 验证。
3. 分组尝试工具链主版本：Vite、TypeScript、Vitest、ESLint、Cypress；每组完成配置迁移和回归检查。
4. 分析并尝试 Vue Router、Ionicons 等框架周边主版本；仅保留通过兼容性验证的升级。
5. 检查 Capacitor/Pixi/AMLL 组合和 Android 同步状态；不改业务功能。
6. 运行 `npm ci`、`npm run lint`、`npm run build`、`npm run test:unit -- --run`、`git diff --check`，环境可用时运行 e2e 和 Android 构建。
7. 更新任务记录与必要的前端规范，提交依赖升级。
8. 关闭 GitHub #39、归档任务；若主版本因环境或迁移范围阻塞，记录证据并保留已验证的小版本升级。

## 检查点

- package.json 与 lockfile 版本一致。
- 不存在 node_modules 变更。
- 测试输出不包含敏感凭据。
- 与播放器、歌词、WebDAV 相关的既有测试全部通过。

## 本次实施记录

### 已完成的安全升级

- AMLL：`core` 0.5.2、`lyric` 1.0.2、`vue` 0.5.2。
- Capacitor 8：Android 8.4.2、CLI 8.4.2、Core 8.4.2、App 8.1.1、Haptics 8.0.2、Keyboard 8.0.5、Status Bar 8.0.3；保持 Capacitor 主版本一致。
- Capgo：媒体会话 8.0.29、原生音频 8.4.17。
- Ionic Vue 与路由 8.8.14；Vue 3.5.40、Vue Router 4.6.4；TanStack Vue Virtual 3.13.32。
- Vite 工具链：Vite 8.1.5、`@vitejs/plugin-vue` 6.0.8、`@vitejs/plugin-legacy` 8.2.1；Terser 5.49.0。
- Pixi 仍保持 7.x，AMLL 运行时组合未切换主版本；Ionicons 升级至 8.0.13，与 `@ionic/core@8.8.14` 声明的 `ionicons ^8.0.13` 保持一致。

### 主版本验证与保留项

- Vite 8 通过现有 legacy 配置、类型检查和生产构建；仅产生上游 Ionic CSS 的 `host-context` Lightning CSS 警告及既有大分块警告，无业务源码改动。
- TypeScript 7 与 vue-tsc 3.3.7 已试升级，但 vue-tsc 在当前 TypeScript 7 上访问未导出的 `typescript/lib/tsc`，导致 build 失败；同时旧 ESLint 配置链与 TypeScript 7 出现 `Intrinsic` 兼容错误。已恢复 TypeScript 5.9.3 与 vue-tsc 2.2.12，保留可验证组合。
- Vitest 已升级至 4.1.10，jsdom 已升级至 29.1.1；完整单元测试通过。Cypress 已升级至 15.18.1；其声明的 Node 要求为 `^20.1.0 || ^22.0.0 || >=24.0.0`，本地 Node 24.15.0 与发布 CI Node 22 均满足。
- Ionicons 已升级至 8.0.13。独立检查确认 `@ionic/core@8.8.14` 本身依赖 `ionicons ^8.0.13`，因此保留 7.x 反而会造成同一项目安装两套图标运行时；升级后 lint、build、完整 unit 均通过。
- ESLint 10、Vue Router 5 未强行升级：前者要求从当前 `.eslintrc.cjs` 旧配置链迁移到 flat config，并与 `@vue/eslint-config-typescript` / eslint-plugin-vue 联动升级；后者是面向 Vue 3.5 的新主版本路由迁移，需单独验证 Ionic Router 集成。当前仅保留已验证的 ESLint 8 / Vue Router 4 组合。
- `@vue/eslint-config-typescript`、eslint-plugin-vue、ESLint 仍保留旧组合，以维持现有 `.eslintrc.cjs` 配置；未引入业务代码迁移。
- `npm outdated` 现仅剩 ESLint 配置链、TypeScript/vue-tsc 和 Vue Router 这些已记录阻塞的联动主版本，不再包含 Ionicons、Vitest、jsdom 或 Cypress。

### 验证结果

- `npm ci`：通过，锁文件可干净安装；独立检查升级 Ionicons/Vitest/jsdom/Cypress 后再次执行通过。
- `npm run lint`：通过。
- `npm run build`：通过；Vite 8 输出上游 CSS 伪类和分块大小警告。
- `npm run test:unit -- --run`：通过，Vitest 4.1.10 下 15 个测试文件、261 个测试通过；仅有 Ionic sourcemap 指向缺失上游源码的提示，不含凭据。
- `npx cap sync android`：通过，识别 8 个 Android 插件并完成同步。
- `npm audit`：由升级前 14 项（含 Vitest critical）降至 6 项 high；剩余项来自暂缓迁移的 ESLint 旧配置链。
- `git diff --check`：通过。
- `npx cypress verify`：通过，Cypress 15.18.1 本地二进制可启动；未运行完整 e2e，因为当前检查未启动 dev server。
- Android APK 编译：未执行；当前环境 Node 24.15.0 / npm 11.16.0，但没有 `java` 命令。发布工作流使用 Node 22 与 JDK 21，依赖声明均兼容，应由 CI 完成最终 Android 验证。
