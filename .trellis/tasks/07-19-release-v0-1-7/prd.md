# 发布 v0.1.7

## 目标

将已完成的音源列表、歌词和播放器修复发布为 Android v0.1.7，并通过 GitHub Actions 生成两个签名 APK Release 产物。

## 需求

- 将前端包版本从 `0.1.6` 更新为 `0.1.7`。
- 将 Android `versionName` 更新为 `0.1.7`，`versionCode` 递增为 17。
- 新增中文变更日志，覆盖本轮已合并的功能修复。
- 保持既有发布工作流：推送 `v0.1.7` 标签后由 GitHub Actions 构建并上传标准包与 MIUI 包。
- 发布前通过 lint、build、unit test 和 diff check。
- 不把其他未完成任务目录或依赖升级混入发布提交。

## 验收标准

- [ ] package.json 版本为 0.1.7。
- [ ] Android versionName 为 0.1.7、versionCode 为 17。
- [ ] changelog/v0.1.7.md 存在且内容准确。
- [ ] 发布前质量检查全部通过。
- [ ] 创建并推送 v0.1.7 标签，GitHub Release 成功创建并包含两个 APK。

## 范围外

- 不在本次发布中升级依赖。
- 不修改未完成的 Trellis 任务。
- 不修改发布工作流的签名密钥或权限配置。
