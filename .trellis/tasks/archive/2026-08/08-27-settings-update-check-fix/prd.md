# 检查更新不可用修复

> 背景：设置页“检查更新”点击后无任何反馈，表现为功能不可用。根因为 `checkLatestRelease` 返回 null 时 UI 赋 `toastMessage = null` 无提示，且请求未带 `User-Agent` 可能被 GitHub API 以 403 拒绝。

## Goal
点击“检查更新”在任何网络/解析场景下都有明确 Toast 反馈：已是最新 / 发现新版并跳转 / 检查失败请重试。

## 范围
1. 修复 `SettingsScreen.kt` 检查更新链路：补 `User-Agent` 与 `finally disconnect`，区分 `200/非200/异常` 三态
2. UI 层：`null` 时不再静默，统一提示“检查更新失败，请稍后重试”，`checking` 状态确保可重置
3. 保持现有版本比较与跳转逻辑不变

## 非范围
- 自动下载/静默更新
- 切换更新源（仍为 `api.github.com/repos/Happier-X/muses/releases/latest`）

## 验收标准
- [ ] 无网络/飞行模式：点击后提示“检查更新失败，请稍后重试”，不会卡在“正在检查更新…”
- [ ] 已是最新版（当前与远端 tag 一致）：提示“已是最新版本”
- [ ] 有新版：提示“发现新版本 vX.Y.Z”并跳转到 `html_url`
- [ ] GitHub 返回 403/非200：同样走失败提示，不静默
- [ ] 连续点击受 `checking` 保护，不会并发多次请求
