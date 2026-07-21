# 发布执行计划

1. 更新 `package.json` 版本为 `0.1.8`。
2. 更新 `android/app/build.gradle`：`versionCode 18`，`versionName "0.1.8"`。
3. 新增 `changelog/v0.1.8.md`（依赖与工具链升级摘要）。
4. 运行质量检查：
   - `npm run lint`
   - `npm run build`
   - `npm run test:unit -- --run`
   - `git diff --check`
5. 检查 staged 文件范围后提交 `chore(release): v0.1.8`。
6. 创建并推送 `v0.1.8` 标签（`git tag v0.1.8 && git push origin main && git push origin v0.1.8`，以实际分支为准）。
7. 轮询 GitHub Actions 与 Release，确认两个 APK 资产：
   - `muses-v0.1.8.apk`
   - `muses-v0.1.8-mi.apk`
8. 归档发布任务并提交任务归档记录。

## 验证重点

- 版本号三处一致：`package.json`、`versionName`、changelog 文件名/标题。
- `versionCode` 严格递增为 18。
- 变更日志不声称未完成的功能。
- 标签指向发布提交。
- 工作流使用现有签名 secrets，不暴露敏感信息。
