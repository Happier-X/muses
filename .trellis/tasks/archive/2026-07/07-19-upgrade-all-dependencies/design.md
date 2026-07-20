# 技术设计

## 升级分层

1. **安全小版本组**：先更新所有同主版本的 latest/wanted 依赖，包括 Capacitor 8、Ionic 8、Vue 3、AMLL 0.x/1.x、Capgo 8、TanStack、Terser 等。
2. **工具链主版本组**：按 Vite/plugin-vue/plugin-legacy、TypeScript/vue-tsc、Vitest/jsdom、ESLint 配置、Cypress 分组升级；每组独立验证并修复配置或类型问题。
3. **框架主版本组**：仅在官方兼容性允许且不需要业务架构迁移时升级 Vue Router、Ionicons 等；Vue/Ionic/Capacitor/Pixi/AMLL 组合必须保持兼容。

## 操作边界

- 仅通过 npm 更新 `package.json` 与 `package-lock.json`，不修改 node_modules。
- 优先使用 `npm-check-updates` 或 `npm install` 生成锁文件，避免手工篡改锁文件。
- 每组升级后运行 lint/build/unit；发现破坏性变更则回滚该组并在任务记录中说明。
- Android 相关升级需检查 `npx cap sync android`；本地无 Java 时由 GitHub Actions 负责最终 Android 验证。

## 兼容性风险

- Vite 8、Vitest 4、TypeScript 7、ESLint 10、Vue Router 5 可能要求配置和 API 迁移，不能与“只改版本号”混为一谈。
- `@vitejs/plugin-legacy`、Cypress、jsdom 可能受 Node 版本或浏览器目标影响。
- Capacitor 插件必须保持同一主版本，原生插件升级不得改变凭据、媒体通知和播放路径契约。

## 回滚

每组升级单独提交或保留可逆变更；任何一组导致业务测试或构建失败，都恢复该组 package 清单和锁文件，不回退已验证的前序组。
