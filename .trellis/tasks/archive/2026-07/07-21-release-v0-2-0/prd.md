# 发布 v0.2.0

## Goal

将 v0.1.9 之后已合并的 UI 修复与 ReplayGain 音量均衡发布为 Android **v0.2.0**，通过 GitHub Actions 生成双包名签名 APK。

## Background

- 当前版本：`0.1.9`（tag `v0.1.9`，`versionCode 19`）。
- v0.1.9 之后已合并：
  - #43 跳转当前播放避让粘性顶栏（scroll-margin-top）
  - #45 播放主控 fill 图标
  - #44 队列/歌单同语义图标统一为 `list`
  - #46 ReplayGain 轻量音量均衡 + 设置开关
- 次版本 0.2.0：相对 0.1.x 增加可感知播放/设置能力（响度均衡），并含多项 UX 修复。

## Requirements

- `package.json` / `package-lock.json` 根 `version` → `0.2.0`
- Android `versionName` → `0.2.0`，`versionCode` → `20`
- 新增中文 `changelog/v0.2.0.md`，只写已合并内容
- 发布前：`npm run lint`、`npm run build`、`npm run test:unit -- --run`、`git diff --check`
- 提交 `chore(release): v0.2.0`，打 tag `v0.2.0` 并推送，触发 Release workflow
- 确认 GitHub Release 含 `muses-v0.2.0.apk` 与 `muses-v0.2.0-mi.apk`
- 遵守锁文件根 version 同步等发布约定

## Acceptance Criteria

- [x] 三处版本元数据一致为 0.2.0 / versionCode 20
- [x] changelog 存在且与提交一致
- [x] lint / build / unit / diff check 通过
- [x] tag `v0.2.0` 已推送且 Release 双 APK 成功

## Out of Scope

- 不再升级依赖或改业务
- 不改签名 / workflow 密钥

## Notes

- 轻量发布任务：PRD-only。
