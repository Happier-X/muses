# PRD：发布 v0.3.5

## 目标与用户价值

将 v0.3.4 之后已验证的改动发布为新版本 v0.3.5，通过 GitHub Actions 构建签名 APK 并创建 Release，供用户安装。

## 已确认事实（代码/流程证据）

- 发布流程（参考 v0.3.4，commit ff67e33）：同步 `package.json`/`package-lock.json` 与 `android/app/build.gradle`（versionCode/versionName）→ 撰写 `changelog/vX.Y.Z.md` → 提交 `chore(release): vX.Y.Z` → 推 tag `vX.Y.Z` → `.github/workflows/release.yml` 构建双包名签名 APK 并创建 GitHub Release
- 当前版本：`package.json`/`build.gradle` = 0.3.4 / versionCode 34
- v0.3.4 → HEAD 的用户可见改动：
  - 播放页：下滑回弹卡住修复（0528c8f）；顶部标题/艺术家固定复用 + 左右滑动只切换下方内容（246690c）；固定头部 z-index 修复（9b3eacd）
  - 侧边栏：椒盐美化系列——灰色悬浮卡片（3281f73）、分段卡片（750cc90）、卡片顶部避让间距（55dd816）、去按压背景（d2effe5）、去选中加粗（6cfcaee/6b039a9）、玻璃透明加重（c6890df）
  - navbar/工具条/MiniPlayer 磨砂玻璃透明加重（c6890df，alpha 0.8→0.65、0.6→0.45）

## 需求

1. 版本号 0.3.4 → **0.3.5**，versionCode 34 → **35**，三处同步：`package.json`、`package-lock.json`、`android/app/build.gradle`
2. 新建 `changelog/v0.3.5.md`，按历史 changelog 风格撰写（中文、按功能分节、要点式）
3. 提交 `chore(release): v0.3.5`（含 changelog）
4. 打 tag `v0.3.5` 并推送（触发 GitHub Actions）
5. 验证 CI Release 产物：双包名签名 APK 上传成功

## 验收标准

- A1 `package.json` / `package-lock.json` / `build.gradle` 三处版本号一致为 0.3.5/35
- A2 changelog/v0.3.5.md 存在且覆盖全部用户可见改动
- A3 本地 `npm run build` + `assembleRelease`（或 CI 前的最小验证）通过
- A4 tag v0.3.5 推送成功，GitHub Actions Release workflow 触发且构建成功
- A5 GitHub Release 页面出现签名 APK 附件

## 范围外

- 不改功能代码（纯发布流程）
- 不本地签名（签名由 CI secrets 完成）
- 不发布到应用商店

## 风险

- tag 若已存在需处理；CI 失败需查看日志重试（低风险，流程已跑通 35 次）
