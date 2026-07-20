# 发布设计

## 版本来源

以当前 `v0.1.6` 为基线，发布补丁版本 `v0.1.7`。前端 package.json 与 Android versionName 保持一致；versionCode 从 16 递增到 17。

## 发布流程

1. 修改版本文件和变更日志。
2. 执行本地质量检查。
3. 单独提交发布元数据。
4. 创建并推送 `v0.1.7` 标签。
5. GitHub Actions 根据标签执行前端构建、Capacitor 同步、两个 applicationId 的签名 APK 构建并创建 Release。
6. 检查 Release 状态和两个 APK 资产。

## 风险与回滚

- 发布工作流依赖仓库签名密钥 secrets；本地不读取或生成密钥。
- 若 Actions 失败，删除远端标签/Release 前先确认失败原因；修复后使用新的提交重新打标签。
- 版本提交只包含 package.json、android/app/build.gradle、changelog/v0.1.7.md 和任务归档文件，不包含其他任务目录。
