# 发包流程增加并行 Windows 端 MSI 构建

## Goal

release.yml 从纯安卓发包扩展为安卓+Windows 并行构建：推 v* tag 后 Windows runner 上用 Compose Desktop 打 MSI，与安卓 APK 一并汇入同一个 GitHub Release。

## Requirements

1. 推送 `v*` tag 后，Windows 端与安卓端**并行**构建，互不阻塞（一端失败不拖垮另一端的构建产物，但整体 Release 需两端都成功才发）。
2. Windows 端产物只发 **MSI**（用户已确认；EXE 格式本次不发）。
3. MSI 版本号跟随 tag（`v0.4.10` → `0.4.10`），替代 `composeApp/build.gradle.kts` 中写死的 `packageVersion = "1.0.0"`。
4. 最终 GitHub Release 只创建一次，同时包含：安卓双渠道 APK + Windows MSI，release 说明中的"构建产物"清单同步更新。

## Constraints

- 不改变安卓侧现有构建逻辑、签名注入方式与产物命名（`muses-vX.Y.Z.apk` / `muses-vX.Y.Z-mi.apk`）。
- 不改变触发方式（仍是 `v*` tag push）。
- Windows 构建不签名（暂无代码签名证书，jpackage 默认产物即可）。
- `build-test.yml`（手动测试构建）本次不动。

## Design Notes（轻量任务，方案并入 PRD）

- `release.yml` 拆为三个 job：
  - `build-android`（ubuntu-latest）：现逻辑原样迁移；
  - `build-windows`（windows-latest）：JDK 21 + `:composeApp:packageMsi`，MSI 版本经 Gradle property 从 tag 注入，产物上传 artifact；
  - `release`（ubuntu-latest，`needs` 两构建 job）：下载两侧 artifact，合并生成 release body，`softprops/action-gh-release@v2` 一次性创建 Release。
- `composeApp/build.gradle.kts`：`packageVersion` 改为读取 Gradle property `-Pmuses.desktop.version=<tag版本>`，未传时回落 `1.0.0`（保证本地开发不受影响）。
- `javaHome` 在 CI 上通过已有的 `MUSES_DESKTOP_JDK` 环境变量兜底指向 runner 的 JDK（setup-java 提供，含 jpackage.exe）；本机硬编码路径保留为回落值。

## Acceptance Criteria

- [ ] AC1：`release.yml` 语法有效，job 依赖关系为 `build-android`、`build-windows` 并行 → `release` 串行汇总。
- [ ] AC2：Windows job 在 windows-latest 上执行 `:composeApp:packageMsi`，产物重命名为 `Muses-v<tag>.msi` 上传 artifact。
- [ ] AC3：MSI 的 `packageVersion` 来自 tag（`v0.4.10` → `0.4.10`）；不传 property 时本地构建仍用 `1.0.0`，本地开发不受影响。
- [ ] AC4：`release` job 在两个构建 job 都成功后创建唯一 Release，附件含 2 个 APK + 1 个 MSI，说明中"构建产物"列出三种产物。
- [ ] AC5：安卓构建步骤与改造前语义一致（命令、签名注入、产物名不变）。
