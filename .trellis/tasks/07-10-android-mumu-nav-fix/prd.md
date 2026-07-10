# 记录 Android MuMu 导航修复

## 背景

Ionic Vue Capacitor 应用在 Android / MuMu 模拟器中出现运行与布局问题：

- `npx cap run android` / `native-run` 无法稳定连接 MuMu，报错或设备离线。
- 应用在 Android WebView 中曾出现白屏。
- 平板宽度下导航布局异常，曾出现两个左侧导航栏。
- 窄屏需要保留底部 tab，平板宽度需要显示左侧导航栏。

## 目标

记录并验证已完成的修复：

- 使用 MuMuManager 替代不稳定的 ADB / native-run 安装启动流程。
- 避免 `ion-split-pane` / `ion-menu` 在 MuMu WebView 下导致白屏。
- 修复 `TabsPage.vue` 中父子路由布局叠加导致的双左侧导航。
- 保持响应式导航：窄屏底部 tab，平板左侧导航 + 右侧内容。

## 范围

包含：

- `src/views/TabsPage.vue` 的布局修复记录。
- Android 构建、复制、打包、安装、启动验证记录。
- MuMu 安装启动命令记录。

不包含：

- unrelated `.pi` / Trellis 文档变更整理。
- 代码提交或远端推送。
- 额外功能开发。

## 已实施方案

- 将 `TabsPage.vue` 从 Ionic `ion-page` 父页面改为普通布局 shell，避免 Ionic 路由页面缓存/叠层导致父子布局重复渲染。
- 子页面继续由路由渲染，保留各自页面结构。
- 平板宽度使用固定左侧导航与右侧内容区。
- 窄屏使用底部 `ion-tab-bar`。
- 移除临时诊断标识 `UNIQUE-SIDEBAR-LEFT`。
- 避免使用 `ion-split-pane` / `ion-menu`，因为它们在当前 MuMu WebView 环境中触发白屏。

## 验证方式

已执行：

```bash
npm run build
npx cap copy android
cd android
./gradlew.bat assembleDebug
"C:\Program Files\Netease\MuMu\nx_main\MuMuManager.exe" sh -v 1 -c "pm uninstall com.muses.player"
"C:\Program Files\Netease\MuMu\nx_main\MuMuManager.exe" control -v 1 app install --apk "C:\code\muses\android\app\build\outputs\apk\debug\app-debug.apk"
"C:\Program Files\Netease\MuMu\nx_main\MuMuManager.exe" control -v 1 app launch --package com.muses.player
```

结果：

- Web 构建成功。
- Capacitor Android assets copy 成功。
- Android debug APK 构建成功。
- MuMu 卸载旧包、安装新包、启动应用成功。
- 用户确认平板布局和窄屏布局均正常。

## 验收标准

- [x] 应用在 MuMu 中不再白屏。
- [x] 平板宽度只显示一个左侧导航栏。
- [x] 平板宽度右侧内容正常显示。
- [x] 窄屏底部 tab 正常显示。
- [x] 调试标识已移除。
- [x] 最新 APK 已安装并启动验证。
