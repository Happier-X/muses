# 发布 Muses v0.2.1

## Goal

将 `v0.2.0` 之后已合并到本地 `main` 的改动发布为 Muses **v0.2.1**，生成双包 APK 的 GitHub Release，并保证版本、Changelog、Tag 与构建产物一致。

## 已核实事实

- 当前版本：
  - `package.json`: `0.2.0`
  - `package-lock.json`: `0.2.0`
  - `android/app/build.gradle`: `versionName "0.2.0"`、`versionCode 20`
- 最新 tag / Release：`v0.2.0`，GitHub Release 已于 2026-07-21 发布
- 发布 workflow：`.github/workflows/release.yml`，push `v*` tag 后：
  1. Ubuntu + Node 22 `npm ci`
  2. 前端 build
  3. JDK 21 构建两个签名包：`com.muses.player` 与 `com.miui.player`
  4. 读取 `changelog/v<version>.md`
  5. 创建 GitHub Release 并附上两个 APK
- 本地 `main` 比 `origin/main` 多 4 个提交；自 `v0.2.0` 到当前 HEAD 有大量用户可感知改动
- 远端尚无 `v0.2.1` tag（发布前再确认一次）

## 发布范围（v0.2.0 → 当前 HEAD）

Changelog 面向用户，按主题归纳，不列 task/spec 内部提交：

- **播放**：进度条同步、冷启动恢复上次曲目与进度、响度均衡 +6 dB 预增益、沉浸页隐藏元信息补充提示等
- **性能**：歌曲/队列/歌单虚拟化、扫描批量持久化、队列解析优化、AMLL 索引与运行时缓存治理、隐藏播放器降载
- **UI / 体验**：设计系统与 HeroUI primary、语义 token、`happier-ui@0.0.1` npm 接入、`@lucide/vue` + `HIcon`、歌词浮动控件按需显示、图标语义统一等

## Requirements

### R1 — 版本一致性（已决：v0.2.1）

- `package.json` → `0.2.1`
- `package-lock.json` 根版本及根 package 版本 → `0.2.1`
- `android/app/build.gradle` → `versionName "0.2.1"`、`versionCode 21`

### R2 — 更新日志

- 新增 `changelog/v0.2.1.md`（简体中文）
- 只写已合并、用户可感知内容

### R3 — 发布前验证

```bash
npm ci
npm run lint
npm run build
npm run test:unit -- --run
git diff --check
npx cap sync android
```

- Windows Vitest fork 偶发问题时，可补跑线程池并记录；不得掩盖失败
- APK 以 GitHub Actions JDK 21 为准

### R4 — Git 与 Release

1. 提交 `chore(release): v0.2.1`（仅版本 + changelog + 必要 lock）
2. `git push origin main`
3. 创建 annotated tag `v0.2.1` 并 `git push origin v0.2.1`
4. 监控 Release workflow，验收两个 APK

## Acceptance Criteria

- [x] 用户确认发布版本号 v0.2.1
- [x] 三处版本与 Android versionCode 一致更新
- [x] `changelog/v0.2.1.md` 完成
- [x] npm ci / lint / build / unit / diff-check / cap sync 通过或有明确可接受的环境说明
- [x] `chore(release): v0.2.1` 已提交
- [x] 本地 main 已推送到 origin/main
- [x] annotated tag `v0.2.1` 已推送
- [x] GitHub Actions Release workflow 成功
- [x] GitHub Release 附含 `muses-v0.2.1.apk` 与 `muses-v0.2.1-mi.apk`

### 发布结果

- Commit: `dd36902` `chore(release): v0.2.1`
- Tag: `v0.2.1`
- Actions run: `30052389072` success
- Release: https://github.com/Happier-X/muses/releases/tag/v0.2.1
- Assets: `muses-v0.2.1.apk`, `muses-v0.2.1-mi.apk`

## 风险与回滚

- Tag 推送前失败：修复后重验，不打 tag
- Tag 已推送但 CI 失败：修 commit 后按规范移动 tag 重推；不保留错误 Release
- 远端已有 `v0.2.1`：立即停止并改选版本

## 已决

| 项 | 决定 |
|------|------|
| 版本号 | **v0.2.1**，`versionCode 21` |

## 待决

无。

## Notes

- push/tag 为外部副作用，仅在 `task.py start` 后执行
- 任务产物：`design.md`、`implement.md`、jsonl 就绪后请用户审阅再 start
