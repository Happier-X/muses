# 实施计划：GitHub Release 发布与双包名构建

## 实施顺序

按依赖关系排列，每步独立可验证。

### Step 1: 修改 android/app/build.gradle 支持动态 applicationId

**文件**：`android/app/build.gradle`

**修改**：
- `defaultConfig.applicationId` 改为 `project.hasProperty('appId') ? project.appId : "com.muses.player"`

**验证**：
```bash
# 默认构建（不传 appId property）应用默认包名
cd android && ./gradlew assembleRelease
# 指定 miui 包名构建
cd android && ./gradlew assembleRelease -PappId=com.miui.player
```

应看到 `com.miui.player` 的 APK 生成。

### Step 2: 修改 capacitor.config.ts 支持环境变量切换 appId

**文件**：`capacitor.config.ts`

**修改**：appId 读取环境变量 `CAPACITOR_APP_ID`，默认 `com.muses.player`

**验证**：
```bash
# 默认 sync
npx cap sync android    # 应在 MainActivity 路径看到 com/muses/player
# miui 包名 sync
CAPACITOR_APP_ID=com.miui.player npx cap sync android  # 应在 MainActivity 路径看到 com/miui/player
```

### Step 3 (跳过): 更新日志

changelog 由 AI 自动生成，开发者发版前手动创建 `changelog/v{version}.md` 并更新 `package.json` version。

### Step 4: 创建 GitHub Actions Release 工作流

**新建**：`.github/workflows/release.yml`

**验证**：推送 tag 后自动触发（需实际推送到 GitHub 验证）

### Step 5 (已完成): 签名 keystore 配置

**状态**：已完成

- keystore 已生成：`muses-release.keystore`（本地，已加入 `.gitignore`）
- GitHub Secrets 已配置：
  - `KEYSTORE_BASE64`：keystore 文件的 Base64 编码
  - `KEYSTORE_PASSWORD`：keystore 密码
  - `KEY_ALIAS`：密钥别名 `muses`
  - `KEY_PASSWORD`：密钥密码

workflow 中将解码 keystore 并配置 signingConfig。

## 实施前检查清单

- [ ] 读取 `android/app/build.gradle` 当前内容
- [ ] 读取 `capacitor.config.ts` 当前内容
- changelog 由 AI 自动生成（无需脚本）

## 回滚点

- 每个 step 独立，出错只需回退该 step 的文件
- Gradle 改动可撤回到硬编码 applicationId
- capacitor.config.ts 环境变量方案兼容现有硬编码默认值

## 关键文件

| 文件 | 风险等级 | 说明 |
|------|----------|------|
| `android/app/build.gradle` | 中 | 构建配置核心，错误会导致无法构建 |
| `capacitor.config.ts` | 中 | npm sync 和行为依赖该文件 |
| `.github/workflows/release.yml` | 低 | 新文件，不影响现有功能 |