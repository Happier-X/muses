# 执行计划：Salt 侧滑导航

## 实施清单

1. 阅读 `src/views/TabsPage.vue`、`src/components/MiniPlayer.vue`、`src/theme/index.scss` 及一级页面 Navbar 结构，确定汉堡入口的最小插入位置。
2. 在 `TabsPage.vue` 移除移动端 `MTabbar` 使用，保留平板固定侧栏；新增移动端抽屉、遮罩、汉堡入口、菜单导航与键盘关闭行为。
3. 以 Pointer Events 实现整个移动页面任意位置右滑打开和抽屉区左滑关闭：区分横向与纵向移动，处理阈值、快速滑动、取消与回弹，使用 motion-v 完成状态动画。
4. 更新主题内容止位 token、MiniPlayer 底部定位和相关注释，删除 Tabbar 64px 的移动端布局假设；确认平板规则不变。
5. 更新 `component-guidelines.md`，记录移动端抽屉层级、手势边界、底部几何和可访问性契约。
6. 静态检查全仓库：TabsPage 不再渲染 MTabbar；不存在过时的移动端 Tabbar 止位或 MiniPlayer 偏移；路由与页面模板的业务逻辑未改变。
7. 运行构建、lint、diff check，并同步 Android；在 MuMu 验证汉堡、右滑打开、左滑关闭、遮罩关闭、四入口跳转、详情父菜单高亮、MiniPlayer 和播放器/队列弹层。

## 验证命令

```bash
npm run build
npm run lint
git diff --check
npx cap sync android
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.muses.player 1
adb logcat -d -t 1500
```

## 风险与检查点

- Pointer Events 不能抢占垂直列表滚动或 PlayerPage 既有手势；仅在 TabsPage 移动端路由生效。
- 抽屉的 z-index 必须低于 MPopup、MSheet、MDialog、MActions、MToast，以免覆盖关键弹层。
- 取消底栏后，必须同时核对 `--m-content-pb`、`html.muses-mini-visible` 和 MiniPlayer 的 `bottom`，避免出现空带或内容被遮挡。
- 汉堡入口若需要跨多个页面插入，应抽取最小布局组件，不能逐页复制导航业务。

## 回滚点

- TabsPage 抽屉提交可独立回滚，恢复移动端 MTabbar。
- theme/MiniPlayer 几何提交可独立回滚，但必须与导航容器保持配套，不能只回滚其中一项。
