# GitHub Release 发布与双包名构建

## Goal

实现 GitHub Release 自动发布流程：打 tag 时触发 GitHub Actions 构建两个包名的 Android APK，自动创建 Release 并上传产物；同时提供发版前手动生成更新日志的脚本。

## 背景

当前项目为 Ionic + Capacitor + Vue 音乐播放器（muses），Android 包名为 `com.muses.player`。为兼容 CarWith（小米车机），需要同时发布另一个包名 `com.miui.player` 的版本。两个包名的产物功能完全相同，仅 `applicationId` 不同。

## 已确认事实（从代码库探查）

1. **项目结构**：前端 `src/` → Vite 构建 → `dist/` → Capacitor 同步到 `android/`
2. **Android 构建**：Gradle，`android/app/build.gradle` 中硬编码 `namespace = "com.muses.player"` 和 `applicationId "com.muses.player"`
3. **AndroidManifest.xml**：使用 `${applicationId}` 占位符做 FileProvider authority，包名变更会自动适配
4. **capacitor.config.ts**：`appId: 'com.muses.player'`
5. **当前无 GitHub Actions**：`.github/` 目录不存在
6. **当前无 changelog 目录**，无版本标签
7. **产物输出**：Android 构建默认输出 `android/app/build/outputs/apk/release/app-release.apk`
8. **Apache-2.0 许可证**：`LICENSE` 文件存在

## 需求

### R1: GitHub Actions Release 工作流
- 仅当推送 `v*` 格式的 tag（如 `v1.0.0`）时触发
- 从 tag 提取版本号（去 `v` 前缀），产物命名规则如下
- 自动构建两个包名产物
- 自动创建 GitHub Release，上传两个 APK 作为附件
- Release body 包含 tag 对应的 changelog 文件内容

### R2: 双包名 Android 构建
- 产物 1：`com.muses.player`，文件名 `muses-v{version}.apk`
- 产物 2：`com.miui.player`，文件名 `muses-v{version}-mi.apk`
- 修改 `build.gradle` 支持动态切换 `applicationId`（通过 Gradle property）
- 修改 `capacitor.config.ts` 支持构建时切换 `appId`（通过环境变量）
- 两个包名使用相同的应用名称和图标，不区分显示

### R3: 更新日志
- changelog 文件放在 `changelog/` 目录，按版本命名（如 `changelog/v1.0.0.md`）
- 由开发者用 AI 自动生成，格式为按 conventional commits 分类的 markdown

### R4: 发版操作流程
- 开发者 AI 生成 changelog 并更新 package.json version
- 确认 changelog 内容后 commit
- 打 `v*` 格式 tag 并推送 → 自动触发 Release 构建

## 验收标准

- [ ] 推送 `v1.0.0` tag → GitHub Actions 自动运行 → Release 创建成功，包含两个 APK
- [ ] `muses-v1.0.0.apk` 包名为 `com.muses.player`
- [ ] `muses-v1.0.0-mi.apk` 包名为 `com.miui.player`
- [ ] Release body 包含对应 `changelog/v1.0.0.md` 的内容
- [ ] 可以直接运行 `npm run changelog` 在 `changelog/` 目录生成当前版本的更新日志

## 技术决策

- **版本号来源**：git tag（`v` 前缀剥离后即为版本号），发版前手动更新 `package.json` 的 `version`
- **包名显示**：两个包名使用相同的 app_name 和图标，不区分显示
- **changelog 格式**：按 conventional commits 分类（feat/fix/chore 等），由 AI 自动生成

## 范围外

- 不涉及 iOS 构建
- 不涉及 Google Play / 应用商店发布
- 不涉及代码签名（APK 签名按现有 Gradle 配置处理）