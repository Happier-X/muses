# 发布设计

## 版本来源

以当前 `v0.1.7` 为基线，发布补丁版本 `v0.1.8`。前端 `package.json` 与 Android `versionName` 保持一致；`versionCode` 从 `17` 递增到 `18`。

## 变更范围（纳入 changelog）

| 提交 | 说明 |
|------|------|
| `640a91f` | 升级已验证的项目依赖 |
| `0acfa70` | 升级 ESLint 10 与剩余可验证依赖（flat config） |

任务归档与 journal 提交不写入用户可见变更日志。

## 发布流程

1. 修改版本文件和变更日志。
2. 执行本地质量检查。
3. 单独提交发布元数据（`chore(release): v0.1.8`）。
4. 创建并推送 `v0.1.8` 标签。
5. GitHub Actions 根据标签执行：前端构建 → Capacitor 同步 → 两个 applicationId 的签名 APK → 创建 Release 并上传资产。
6. 检查 Release 状态和两个 APK 资产。

## 风险与回滚

- 发布工作流依赖仓库签名密钥 secrets；本地不读取或生成密钥。
- 若 Actions 失败，先确认失败原因；修复后用新提交重新打标签。删除远端标签/Release 前需确认不会影响已下载用户。
- 版本提交只包含 `package.json`、`android/app/build.gradle`、`changelog/v0.1.8.md` 和任务相关文件，不包含其他未完成任务目录。
