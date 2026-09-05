# 发包流水线契约（release.yml）

## 1. Scope / Trigger

- 触发：push tag `v*`（一个 tag 只对应一次运行；需要重发时先删远端 tag 再重打到目标提交）
- 三 job 结构：`build-android`(ubuntu-latest) ∥ `build-windows`(windows-latest) → `release`(ubuntu-latest, `needs: [build-android, build-windows]`)
- 并行语义：一端构建失败不阻塞另一端构建，但 `release` job 被跳过（needs 全成功才发版），不会产生半成品 Release
- 权限最小化：构建 job `contents: read`；只有 `release` job `contents: write`

## 2. Signatures（构建命令与注入点）

### Android（build-android）

```bash
VERSION_CODE=$(git rev-list --count HEAD)
./gradlew :app:assembleMusesRelease :app:assembleMiuiRelease \
  -Pandroid.injected.signing.store.file=... -Pandroid.injected.signing.store.password=... \
  -Pandroid.injected.signing.key.alias=... -Pandroid.injected.signing.key.password=... \
  -Pandroid.injected.versionName=<去v版本> -Pandroid.injected.versionCode=$VERSION_CODE
```

- `fetch-depth: 0` 必须保留（versionCode 依赖完整提交历史）
- 产物：`muses-vX.Y.Z.apk` / `muses-vX.Y.Z-mi.apk`

### Windows MSI（build-windows）

```powershell
$env:MUSES_DESKTOP_JDK = $env:JAVA_HOME
./gradlew.bat :composeApp:packageMsi "-Pmuses.desktop.version=<去v版本>"
```

- 产物：`Muses-vX.Y.Z.msi`，jpackage 输出在 `composeApp/build/compose/binaries/main/msi/Muses-<去v版本>.msi`
- runner 的 Temurin JDK 21 自带 `jpackage.exe`；WiX 由 Compose 插件自动下载

## 3. Contracts（环境与属性）

| 键 | 传递方式 | 消费方 | 约束 |
|----|---------|--------|------|
| `-Pmuses.desktop.version` | Gradle property | `composeApp/build.gradle.kts` → `packageVersion` | 必须纯数字点分（如 `0.5.2`），MSI 不接受预发布后缀 |
| `MUSES_DESKTOP_JDK` | 环境变量（pwsh 内赋 `$env:JAVA_HOME`） | `compose.desktop.application.javaHome` | 必须是含 jpackage 的完整 JDK；不设时回落本机硬编码路径（仅本地有效） |
| `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` | GitHub secrets | 安卓签名 | 仅 android job 需要 |

- `packageVersion` 读取约定（composeApp/build.gradle.kts）：`findProperty("muses.desktop.version") ?: "1.0.0"`——本地开发不传 property 时回落 1.0.0，不得删掉回落分支。

## 4. 变量约定（防 `vv` 复发）

| 变量 | 值示例 | 允许用途 |
|------|--------|---------|
| `github.ref_name` | `v0.5.2` | Release 标题、tag_name——**自带 v，禁止再拼 v** |
| `steps.version.outputs.version` | `0.5.2` | 文件名拼接（`muses-v0.5.2.apk` / `Muses-v0.5.2.msi`）、`-P` 版本注入、body 清单——**无 v，拼文件名时手动加 v** |

### Wrong vs Correct

```yaml
# Wrong：标题变成 vv0.5.2
name: v${{ github.ref_name }}

# Correct
name: ${{ github.ref_name }}
```

## 5. 已踩坑与修复（v0.5.2 实测）

### Common Mistake 1：pwsh 吞 `-P` 参数

- **Symptom**：`Task '.desktop.version=0.5.2' not found in root project`，构建在配置阶段即失败
- **Cause**：pwsh 把 `-P` 开头的 token 当作 pwsh 自己的参数解析，`muses.desktop.version=0.5.2` 被拆碎，Gradle 收到 `.desktop.version=...` 并当成任务名
- **Fix / Prevention**：所有 `-P<key>=<value>` 参数在 pwsh 中必须整体加引号

```powershell
# Wrong
./gradlew.bat :composeApp:packageMsi -Pmuses.desktop.version=0.5.2
# Correct
./gradlew.bat :composeApp:packageMsi "-Pmuses.desktop.version=0.5.2"
```

### Common Mistake 2：MSI 输出路径写死 `main-release`

- **Symptom**：Rename 步骤 `Cannot find path '.../binaries/main-release/msi'`
- **Cause**：`packageMsi`（非 release 变体任务）实际输出在 `binaries/main/msi/`；`main-release` 是另一个变体目录，容易凭记忆写错
- **Fix / Prevention**：重命名用通配符同时覆盖两种变体，找不到显式失败

```powershell
$msi = Get-ChildItem "composeApp/build/compose/binaries/*/msi/*.msi" | Select-Object -First 1
if (-not $msi) { throw "未找到 MSI 产物" }
```

## 6. 失败排查矩阵

| 症状 | 根因 | 处置 |
|------|------|------|
| `Task '.xxx=...' not found` | pwsh 下 `-P` 参数未加引号 | 见 Common Mistake 1 |
| `Cannot find path .../binaries/main-release/msi` | 输出目录写死为 main-release | 见 Common Mistake 2 |
| Release 标题为 `vvX.Y.Z` | 标题模板手动拼了 v | 见 §4 变量约定 |
| `release` job 显示 skipped | 某构建 job 失败导致 needs 未满足 | 符合预期，去修失败的构建 job |
| 两侧构建都绿但 Release 附件缺失 | `fail_on_unmatched_files` 未触发 / glob 未命中 | 核对 artifact name 与 `files` glob 路径闭环 |

## 7. 发版操作步骤（标准流程）

1. 确认工作区干净、main 已 push（tag 指向的提交必须含最新 workflow）
2. 写 `changelog/vX.Y.Z.md` 并提交 push（缺失时 Release 说明回落"暂无更新日志"）
3. `git tag vX.Y.Z && git push origin vX.Y.Z`
4. `gh run watch` 观察；全部绿后 `gh release view vX.Y.Z` 核对附件（2 APK + 1 MSI）与标题
5. 需要重发时：`git tag -d vX.Y.Z && git push origin :refs/tags/vX.Y.Z`，修复后在目标提交上重打
