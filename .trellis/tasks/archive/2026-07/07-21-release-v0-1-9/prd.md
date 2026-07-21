# 发布 v0.1.9

## Goal

将 v0.1.8 之后已合并的播放器 UI、图标、列表跳转与歌词翻译修复发布为 Android **v0.1.9**，通过 GitHub Actions 生成双包名签名 APK。

## Background

- 当前版本：`0.1.8`（tag `v0.1.8`，`versionCode 18`）。
- v0.1.8 之后已合并（含待 push 的 3 个 commit）：
  - 播放页进度条改用 `ion-range`、隐藏圆点（#41）
  - 业务 `ion-icon` 图标改用 Lucide（#42）
  - SongsPage 跳转当前播放滚动到顶部
  - 双语歌词主副行与活跃窗口修复
  - 歌词翻译管线与 AMLL 职责边界对齐
- 发布前需先将本地领先 `origin/main` 的提交推送到远程。

## Requirements

- `package.json` / `package-lock.json` 根 `version` → `0.1.9`
- Android `versionName` → `0.1.9`，`versionCode` → `19`
- 新增中文 `changelog/v0.1.9.md`，只写已合并内容
- 发布前：`npm run lint`、`npm run build`、`npm run test:unit -- --run`、`git diff --check`
- 提交 `chore(release): v0.1.9`，打 tag `v0.1.9` 并推送，触发 Release workflow
- 确认 GitHub Release 含 `muses-v0.1.9.apk` 与 `muses-v0.1.9-mi.apk`
- 遵守既有约定：锁文件根 version 同步、不混入未完成功能

## Acceptance Criteria

- [ ] 三处版本元数据一致为 0.1.9 / versionCode 19
- [ ] changelog 存在且与提交一致
- [ ] lint / build / unit / diff check 通过
- [ ] tag `v0.1.9` 已推送且 Release 双 APK 成功

## Out of Scope

- 不再升级依赖或改业务
- 不改签名 / workflow 密钥
- 不改 applicationId 策略

## Notes

- 轻量发布任务：PRD-only。
- 参考：`quality-guidelines.md` 发布约定、`07-21-release-v0-1-8`。
