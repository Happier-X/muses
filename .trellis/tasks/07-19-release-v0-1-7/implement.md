# 发布执行计划

1. 更新 package.json、android/app/build.gradle，新增 changelog/v0.1.7.md。
2. 运行 `npm run lint`、`npm run build`、`npm run test:unit -- --run`、`git diff --check`。
3. 检查 staged 文件，提交 `chore(release): v0.1.7`。
4. 创建并推送 `v0.1.7` 标签。
5. 轮询 GitHub Actions 和 Release，确认两个 APK 资产。
6. 归档发布任务并提交任务归档记录。

## 验证重点

- 版本号三处一致。
- 变更日志不声称未完成的依赖升级已发布。
- 标签指向发布提交。
- 工作流使用现有签名 secrets，不暴露敏感信息。
