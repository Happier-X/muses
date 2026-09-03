# 修复远程封面不显示：补 Coil 网络引擎依赖

## Goal

审核页封面候选与所有远程封面全灭，根因是只依赖了 `coil-compose` 而缺少 Coil 3 的网络引擎包，`AsyncImage` 加载一切 `https://` 远程图静默失败。补上 `coil-network-okhttp` 依赖并在 MuMu 模拟器真机验证。

## Background / Confirmed Facts

- 封面搜索本身正常：真实网络探测（临时单测，已删）显示 6 源中 5 源返回有效 https URL（仅咪咕返回 HTML 异常）。
- iTunes（mzstatic 苹果 CDN）图片在模拟器浏览器可打开，但应用内 `AsyncImage` 不显示 → 应用内加载链问题，非网络问题。
- 全仓仅依赖 `io.coil-kt.coil3:coil-compose:3.5.0`，无任何 `coil-network-*` 引擎；Coil 3 远程加载能力在独立 `coil-network-okhttp` 包（ServiceLoader 自动注册，零业务代码改动）。
- INTERNET 权限已声明（`app/src/main/AndroidManifest.xml:6`）。
- 影响面：`SaltCover`（歌曲列表/播放页本地封面不受影响，远程封面全灭）、审核页封面候选网格 + 大图预览。

## Requirements

- R1 `gradle/libs.versions.toml` 新增 `coil-network-okhttp`（跟随 `coil` 版本）。
- R2 `core:ui`（最底层，所有 `AsyncImage` 经它间接可用）`implementation(libs.coil.network.okhttp)`。
- R3 构建安装到 MuMu 模拟器，人工进入审核页确认封面候选显示。

## Acceptance Criteria

- [x] AC1 依赖补齐，`:app:installMusesDebug` 构建安装成功。
- [x] AC2 用户在模拟器审核页人工确认：封面候选可展示（2026-09-03 用户确认「可以展示了」）。
- [ ] AC3（后续）：咪咕源返回 HTML 异常（文本+封面均受影响），考虑降级或摘掉 —— 独立任务，不在本任务。

## Out of Scope

- 封面加载失败占位/重试 UI（体验增强，另起任务）。
- 咪咕源修复（见 AC3）。
