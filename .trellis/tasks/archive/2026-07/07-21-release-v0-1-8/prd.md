# 发布 v0.1.8

## 目标

将 v0.1.7 之后已合并的依赖与工具链升级发布为 Android v0.1.8，并通过 GitHub Actions 生成两个签名 APK Release 产物。

## 背景

- 当前线上/仓库版本为 `0.1.7`（tag `v0.1.7`，Android `versionCode 17`）。
- 自 v0.1.7 起已合并：
  - 已验证项目依赖升级（`640a91f`）
  - ESLint 10 flat config 与剩余可验证依赖升级（`0acfa70`）
  - 相关任务归档与 journal 记录
- 无待合并的业务功能改动；本次为补丁发布，主要交付工程稳定性与可维护性升级。

## 需求

- 将前端 `package.json` 版本从 `0.1.7` 更新为 `0.1.8`。
- 将 Android `versionName` 更新为 `0.1.8`，`versionCode` 递增为 `18`。
- 新增中文变更日志 `changelog/v0.1.8.md`，准确描述本轮已合并内容，不声称未完成的功能。
- 保持既有发布工作流：推送 `v0.1.8` 标签后由 GitHub Actions 构建并上传标准包与 MIUI 包。
- 发布前通过 lint、build、unit test 和 diff check。
- 发布提交只包含版本元数据与 changelog（及必要的任务归档），不混入未完成任务或其他无关改动。

## 验收标准

- [ ] `package.json` 版本为 `0.1.8`。
- [ ] Android `versionName` 为 `0.1.8`、`versionCode` 为 `18`。
- [ ] `changelog/v0.1.8.md` 存在且内容与实际提交一致。
- [ ] 发布前 `npm run lint`、`npm run build`、`npm run test:unit -- --run`、`git diff --check` 全部通过。
- [ ] 创建并推送 `v0.1.8` 标签，GitHub Release 成功创建并包含 `muses-v0.1.8.apk` 与 `muses-v0.1.8-mi.apk`。

## 范围外

- 不在本次发布中继续升级依赖或改业务功能。
- 不修改发布工作流的签名密钥或权限配置。
- 不修改 Capacitor / Android 应用 ID 策略。
