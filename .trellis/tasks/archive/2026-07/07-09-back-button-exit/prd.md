# 修复安卓物理返回键无法退出应用

## 目标

在 Android 真机上，用户按系统物理返回键时，应用能按路由位置正确处理：全屏页（播放/队列）返回上一标签页，标签页直接退出应用。

## 背景与根因

- 项目使用 Ionic Vue + Capacitor，路由采用 `createWebHistory`
- 未在 App.ts / main.ts 中注册 Capacitor `App` 插件的 `backButton` 事件监听
- 因此安卓系统返回键事件完全无前端处理，WebView 面对空历史栈不做任何事

## 需求

| ID | 需求 |
|----|------|
| R1 | 应用启动后注册 `App.addListener('backButton', …)` 监听安卓物理返回键事件 |
| R2 | 当前路由在 `/player` 或 `/queue` 时，按下返回键导航至路由历史前一页（通常是回到上一个标签页） |
| R3 | 当前路由在标签页（`/tabs/*` 或 `/` 任意标签页）时，按下返回键直接调用 `App.exitApp()` 退出应用 |
| R4 | 监听在应用全局生命周期内持续有效（从 `App.vue onMounted` 注册），不随页面切换被销毁 |

## 技术约束

- 使用已安装的 `@capacitor/app` 插件，不做个性化原生修改
- 不修改 `MainActivity.kt` 或 `AndroidManifest.xml`
- 处理逻辑放在 `src/App.vue` 中，和现有的 `onMounted` 合并
- 不使用 toast 或额外提示

## 边界情况

| 场景 | 预期行为 |
|------|----------|
| 在 `/player` 页面按返回键 | `router.back()`（若无历史则不操作） |
| 在 `/queue` 页面按返回键 | `router.back()`（若无历史则不操作） |
| 在 `/tabs/songs`（首页）按返回键 | `App.exitApp()` 退出应用 |
| 在其他 `/tabs/*` 子页面按返回键 | `App.exitApp()` 退出应用 |
| 非 Android 平台（浏览器 / iOS） | `App.addListener('backButton')` 不触发，不影响功能 |
| 使用浏览器的 `window.history.back()` 触发（非硬件返回键） | 不受影响，路由正常运作 |

## 验收标准

- [ ] 在 Android 真机上，从任意 Tab 页按返回键 → 应用退出
- [ ] 在 `/player` 页面按返回键 → 回到进入 player 前所在的标签页
- [ ] 在 `/queue` 页面按返回键 → 回到进入 queue 前所在的标签页
- [ ] `npm run build` 通过（TypeScript 和构建无误）
- [ ] iOS / 浏览器环境下不影响正常路由导航

## 超出范围

- Toast 提示或其他确认 UI
- 修改原生 Android 代码
- 处理手势返回（仅处理物理返回键）