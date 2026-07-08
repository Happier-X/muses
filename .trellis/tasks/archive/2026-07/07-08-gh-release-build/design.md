# 技术设计：GitHub Release 发布与双包名构建

## 架构概览

```
发版前              CI 触发后
────────           ─────────
npm run changelog  git push --tags
  │                   │
  ├─ 生成 changelog/v{version}.md    ├─ GitHub Actions checkout tag
  ├─ 更新 package.json version       ├─ 构建 com.muses.player 版
  └─ git commit + push               ├─ 构建 com.miui.player 版
                                      ├─ 签名两个 APK
                                      ├─ 读取 changelog 内容
                                      └─ 创建 Release + 上传产物
```

## 关键设计决策

### 1. 双包名构建方案

**方案选择**：Gradle property 动态切换 + Capacitor 环境变量

在 `build.gradle` 中，将硬编码的 `applicationId` 改为从 Gradle property 读取，有默认值：

```groovy
defaultConfig {
    applicationId project.hasProperty('appId') ? project.appId : "com.muses.player"
}
```

构建时通过 `-PappId=com.miui.player` 切换。

同时需要修改 `capacitor.config.ts` 支持环境变量读取 `CAPACITOR_APP_ID`，因为 Capacitor sync 时需要正确的 `appId` 去配置 Android 插件路径。如果只用 Gradle property 改 `applicationId` 但不改 Capacitor 的 `appId`，`MainActivity` 的包路径会不匹配。

**cap.config.ts 修改方式**：运行时读取环境变量

```ts
const appId = process.env.CAPACITOR_APP_ID || 'com.muses.player';
const config: CapacitorConfig = {
  appId,
  // ...
};
```

### 2. GitHub Actions 工作流设计

**触发**：`on.push.tags: ['v*']`

**Job 步骤**：

1. checkout（fetch-depth: 0 以获取 changelog）
2. 设置 JDK + Node.js
3. npm ci
4. 构建前端：`npm run build`
5. 构建 APK 1：`npx cap sync android` + gradle assembleRelease
6. 构建 APK 2：设置 CAPACITOR_APP_ID=com.miui.player → `npx cap sync` + gradle assembleRelease -PappId=com.miui.player
7. 重命名产物到规范文件名
8. 签名：从 GitHub Secret 解码 keystore → Gradle signingConfig → 签名两个 APK
9. 创建 Release（softprops/action-gh-release）

### 3. 合成 NuGet 发布

**R4**：两个 APK 分别上传到 GitHub Release。

**Release body 生成**：从 `changelog/v{version}.md` 读取内容，附加构建信息。

### 4. Changelog 生成脚本

**实现方式**：Node.js 脚本（项目已有 Node 环境），注册为 `npm run changelog`。

**参数**：`new-version`（要生成的版本号）

**逻辑**：
1. 找到最近的 version tag（`git describe --tags --abbrev=0`）
2. 如果没有找到 tag，从第一个 commit 开始
3. 收集从上一个 tag 到 HEAD 之间的所有 commit
4. 解析 commit message 前缀（feat/fix/chore/docs/revert/test/refactor/perf/style）
5. 过滤掉 `chore: record journal`、`chore(task): archive` 等自动生成的无意义 commit
6. 按分类组织，生成 markdown 文件到 `changelog/v{version}.md`
7. 更新 `package.json` 中的 `version` 字段

**过滤规则**：
- 排除以 `chore: record journal` 开头的
- 排除以 `chore(task): archive` 开头的
- 排除 merge commit（以 `Merge` 开头）

### 5. 文件结构

```
.github/
  workflows/
    release.yml          # Release 工作流

changelog/
  v1.0.0.md              # 更新日志文件

scripts/
  changelog.mjs          # changelog 生成脚本
```

### 修改文件清单

| 文件 | 修改内容 |
|------|----------|
| `android/app/build.gradle` | applicationId 改为从 Gradle property 读取 |
| `capacitor.config.ts` | appId 支持环境变量 `CAPACITOR_APP_ID` |
| `package.json` | 新增 `npm run changelog` 命令 |
| 新增 `.github/workflows/release.yml` | GitHub Actions 工作流 |
| 新增 `scripts/changelog.mjs` | changelog 生成脚本 |

## 风险与注意事项

1. **Capacitor sync 必须跑两次**：两个包名需要不同的 Capacitor 配置，每次构建前都要 sync
2. **构建产物会互相覆盖**：第一次构建的 APK 必须在第二次构建前移走或重命名
3. **签名配置**：keystore 已生成（`muses-release.keystore`，本地保留不入库）并配置为 GitHub Secrets（`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`），workflow 中解码还原后签名
4. **Gradle 构建时间**：两次完整 Android 构建可能耗时 10-20 分钟
5. **无需修改 AndroidManifest**：FileProvider 已使用 `${applicationId}` 占位符

## 范围外

- 不涉及 iOS
- 不涉及应用商店发布
- 不涉及代码签名 key 管理（keystore 已生成并配置到 GitHub Secrets）