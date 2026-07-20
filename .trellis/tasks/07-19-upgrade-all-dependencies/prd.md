# 升级全部依赖

## 目标

在不破坏现有播放器、歌词、WebDAV、安全凭据、Android 构建和发布流程的前提下，将项目依赖更新到可用的最新版本，并记录必要的兼容性调整。

## 已确认事实

- GitHub issue #39 要求“依赖包全部更新到最新”。
- 当前项目使用 Vue 3、Ionic Vue 8、Capacitor 8、Vite 5、Vitest 0.34、TypeScript 5.9、Cypress 13 等技术栈。
- `npm outdated` 显示既有小版本更新，也有 Vite 8、Vitest 4、TypeScript 7、Vue Router 5、Cypress 15、ESLint 10 等跨主版本更新。
- Android 构建依赖 JDK/Gradle 环境；当前 shell 没有可用 `java` 命令，Android 本地构建需在 CI 或配置 JDK 后验证。
- 不修改 `node_modules`，依赖变更必须通过 `package.json` 和锁文件产生。

## 需求

- 更新项目直接依赖及开发依赖，并保持 package.json 与 package-lock.json 一致。
- 对主版本升级逐项检查迁移要求，修复必要的源码、配置和测试兼容问题。
- 保持 Capacitor 相关包的主版本一致，保持 Vue/Ionic/Vue Router 兼容组合，保持 Pixi/AMLL 运行时兼容。
- 保持现有 npm 脚本、Vite legacy 构建、异步播放器 overlay、单元测试和 Cypress 配置可用。
- 不泄露 WebDAV 密码，不将敏感信息写入依赖脚本、日志或持久化数据。
- 依赖升级完成后执行 lint、build、完整 unit test、git diff --check；若环境允许，再执行 Android/Capacitor 构建和 e2e。

## 验收标准

- [x] 直接依赖已更新到约定目标版本，锁文件可由 `npm ci` 干净安装。
- [x] lint、build、完整 unit test 和 diff check 通过。
- [x] 关键播放器、歌词、WebDAV 和 Android 兼容性没有回归。
- [x] 若存在无法在本地验证的 Android 环境限制，已记录 CI 验证结果或明确阻塞原因。
- [x] 更新内容按依赖类别记录，未混入业务功能改动。

## 范围外

- 不修改业务功能或重新设计播放器/歌词架构。
- 不修改 `node_modules`。
- 不在本任务中发布新版本；发布另行处理。

## 已确认产品决策

采用风险分层方案：先更新所有兼容的小版本，再逐项升级主版本并以测试/构建为门槛；若某个主版本需要超出本任务范围的大规模迁移，则记录阻塞证据，不强行破坏现有功能。
