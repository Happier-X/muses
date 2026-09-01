# 沉浸式图标点击仍无切换排障

## 目标

在 MuMu 上二次复现沉浸式底部循环/随机图标点击仍未切换的问题，通过日志定界手势/Bridge/图标替换链路，二次修复使四枚按钮图标可与真值同步切换且不回归。

## 背景

- 前序已完成：`PlayerViewModel.toggle*`、`PlayerScreen` 底部 180dp 排除、`FullPlayerWebView` DOWN 默认放行、`full-player.js` `isInNoSwipeZone` 与 `svgRepeatOne/svgOrder + setRepeatIcon/setShuffleIcon`（`aad86018`）。编译/lint 通过并已归档。
- 现状：用户在 MuMu 上实测“点击图标没有切换呢”，未明确是否已安装含 `aad86018` 的最新 `app-muses-debug.apk`，也未提供 `adb logcat -s FullPlayer` 日志。
- 关联契约：`features-lyrics-playlist.md §7`；32ms 轮询回写；WebViewAssetLoader 缓存可能导致旧 `full-player.js` 仍被加载。

## 需求

### 功能需求

1. **定界**：区分三分支
   - 无 `btn click` → 触摸仍被 Compose/`OnTouchListener` 或 `panels` 横滑拦截
   - 有 `btn click` 无 `-> toggle*` → Bridge `Android.onAction` 未到达 Kotlin
   - 有 `-> toggle*` 但图标/真值不变 → JS 替换链路或 `PlayerConnection` 真值未变
2. **二次修复**：按定界结果最小改动修复，确保手机 `mode-bar` 与平板 `bottom-bar` 四枚按钮图标在 `Repeat↔RepeatOne`、`FormatListBulleted↔Shuffle` 间可切换且与 `repeatMode/shuffleEnabled` 真值一致。
3. **不回归**：横滑、垂直下滑、进度、歌词 seek、队列/更多保持可用。

### 非功能 / 约束

- 不改 `PlayerConnection/MediaController` 契约，仅前端/桥接展示层按需修复。
- 提示用户安装最新构建并清除 WebView 缓存（如 `adb shell pm clear` 或重装），排除旧资源缓存误判。

## 验收标准

- [ ] 提供 `adb logcat -s FullPlayer` 定界日志或复现录屏，明确落入三分支之一。
- [ ] 按分支二次修复后，手机与平板四枚按钮均可切换且不闪回；`-> toggle*` 后 `updateProgress` 回写与图标一致。
- [ ] 门禁 `assembleMusesDebug`、`lintMusesDebug` 通过。

## 范围外

- 不新增循环 `REPEAT_MODE_OFF`；不引新依赖。

## 已确认事实与决策

- 2026-09-01 MuMu 实测“有颜色变但是图标没换”：`active` 高亮切换证明点击链路与 Bridge 已通，定界为 JS 图标形状替换未生效（非手势/Bridge阻断）。
- 前序 `aad86018` 已补 `svgRepeatOne/svgOrder+set*Icon`，但 `svgRepeatOne` 小 path(2×6px)与 `svgOrder` 小圆点(r=1.5)在 20px 下与原图几乎无差，视觉上仍判为未换；且缺版本日志无法确认 APK 已更新。
- 决策：放大“1”与圆点并加 v6/切换日志以同时解决可视性与可观测性，无需改手势或 Media3。

