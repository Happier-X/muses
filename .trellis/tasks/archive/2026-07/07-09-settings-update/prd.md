# 设置页增加版本展示和更新功能

## 目标

在设置页中展示当前应用版本号，并提供检查更新功能：调用 GitHub Releases API 获取最新版本，如高于当前版本则提示用户并通过系统浏览器打开下载页。

## 背景与现状

- 设置页当前为空状态占位（"设置功能待开发"）
- 应用当前版本 0.0.2（记录在 `package.json`、`android/app/build.gradle`）
- 应用已接入 GitHub Release Action，tag `v*` 推送自动构建 APK 并发布
- `tsconfig.json` 已启用 `resolveJsonModule`，可直接 import `package.json`

## 需求

| ID | 需求 |
|----|------|
| R1 | 设置页首屏显示应用名称和当前版本号（从 `package.json` 读取 `version`） |
| R2 | 提供"检查更新"按钮，点击后调用 GitHub API `GET /repos/Happier-X/muses/releases/latest` 获取最新版本 |
| R3 | API 请求时显示加载状态（loading），请求失败显示 Toast 错误提示 |
| R4 | 最新版本等于当前版本 → Toast "已是最新版本" |
| R5 | 最新版本高于当前版本 → Toast "发现新版本 vX.X.X" + 通过 `window.open(url, '_system')` 在系统浏览器打开对应 Release 页面 |
| R6 | 替换设置页当前的"设置功能待开发"空状态 |

## 技术约束

- 不新增第三方依赖（仅用已有 Vite JSON import + `window.open('_system')`）
- 版本比较采用简单字符串对比（semver 风格：`eg. "0.0.2" < "0.0.3"`，不依赖 semver 库）
- 不修改原生代码
- GitHub API 无需认证（public repo，但有速率限制，60 req/h。产品可接受）

## 边界情况

| 场景 | 预期 |
|------|------|
| GitHub API 返回 403 (rate limit) | Toast "检查更新失败，请稍后重试" |
| 网络不通 | Toast "检查更新失败，请检查网络连接" |
| 最新版本 tag 格式异常（非 `vX.Y.Z`） | 忽略该版本，Toast 友好的失败提示 |
| 最新版本 tag 为 `v0.0.2`（同当前） | Toast "已是最新版本" |
| 最新版本 tag 为 `v0.1.0`（高于当前） | Toast "发现新版本 v0.1.0" + 打开 Release 页 |

## 验收标准

- [ ] 设置页显示"应用版本：0.0.2"（与 package.json 一致）
- [ ] 点击"检查更新"按钮，加载时显示 spinner 或 disabled 状态
- [ ] 当前已是最新版本时，Toast "已是最新版本"
- [ ] 有新版本时，Toast 提示版本号并打开系统浏览器跳转到 Release 页
- [ ] 网络异常时，Toast 显示友好错误信息
- [ ] `npm run build` 通过
- [ ] `npm run lint` 零错误

## 超出范围

- 应用内下载 APK 和自动安装（留作未来改进）
- 检查更新后静默下载
- 非 GitHub Release 源的更新检测