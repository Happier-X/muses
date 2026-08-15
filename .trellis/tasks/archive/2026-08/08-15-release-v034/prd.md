# 发布 v0.3.4

## Goal

版本号 + changelog + tag，GitHub Actions 构建双 APK（与 v0.3.3 发布流程一致）。

## Requirements

- 自 v0.3.3 起累计变更（8 个提交）：
  - 液态玻璃全面化（播放条/FAB/navbar/工具条）+ 深色覆盖修复
  - 工具条/搜索栏并入 navbar 同一块玻璃，消除交界分界
  - 玻璃透明度统一为 `--m-glass-bg` token（0.7 → 0.6）
  - 修复切歌时三行歌词误触发滚动动画导致首行被裁切
- 版本号三处同步：`package.json` / `package-lock.json`（0.3.3 → 0.3.4）、`android/app/build.gradle`（versionCode 33 → 34，versionName 0.3.4）
- 新增 `changelog/v0.3.4.md`，风格对齐前序版本
- 提交 + 打 tag `v0.3.4` + push 触发 GitHub Actions（.github/workflows/release.yml 自动构建签名 APK）

## Acceptance Criteria

- [ ] 版本号三处一致为 0.3.4（versionCode 34）
- [ ] changelog/v0.3.4.md 内容完整、格式与 v0.3.3.md 一致
- [ ] tag v0.3.4 已推送，Actions 构建触发（构建结果由 Actions 侧确认）
- [ ] 工作区干净

## Notes

- 轻量任务，PRD-only。
- 参考：`.trellis/tasks/archive/2026-08/08-15-release-v033`（流程相同）；v0.2.9 发布记录见 journal（版本号 package.json/lock + build.gradle versionCode）。
