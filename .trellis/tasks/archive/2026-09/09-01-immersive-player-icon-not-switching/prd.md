# 沉浸式播放页底部图标未随模式切换

## 目标

修复沉浸式播放页（单一 WebView 整页 `FullPlayerWebView + full-player.js/css`）底部循环/随机按钮点击后图标未在对应形态间切换的问题，使手机 `mode-bar`（`#btn-repeat/#btn-shuffle`）与平板 `bottom-bar`（`#bottom-repeat/#bottom-shuffle`）四枚按钮点击后图标与播放器真值一致且可通过 32ms 轮询回写保持。

## 背景

- 现行已修复手势拦截与闭包陈旧值：`PlayerViewModel.toggleRepeat/toggleShuffle` 无参版、`PlayerScreen` 底部 180dp 排除、`FullPlayerWebView` DOWN 默认 `requestDisallow(true)`、`full-player.js` `isInNoSwipeZone`，点击链路经 `adb logcat -s FullPlayer` 已可见 `bindClick ok / btn click / -> toggle*`。
- 仍现缺陷：点击后图标未切换为单曲循环/顺序播放形态。代码勘查锚点 `app/src/main/assets/amll/full-player.js:26-27,147-173,190-210,360-375`：仅定义 `svgRepeat/svgShuffle`，缺 `svgRepeatOne`（单曲循环带“1”）与顺序播放 `svgOrder`（对齐 Compose `FormatListBulleted`）；`bindClick` 与 `updateProgress` 仅 `classList.toggle('active')` 未替换 `innerHTML`；Compose 侧 `feature/player/PlayerScreen.kt:1119/1125,1193/1199` 已以 `repeatMode==ONE -> RepeatOne else Repeat` 与 `shuffleEnabled ? Shuffle : FormatListBulleted` 切图标，WebView 未对齐该契约。
- 关联契约：`features-lyrics-playlist.md §7` 沉浸式单一 WebView 整页；WebView 32ms 轮询 `updateProgress({repeatMode, shuffleEnabled})` 回写；乐观切换后由 Kotlin 真值覆写需一致。

## 需求

### 功能需求

1. **图标随状态切**：
   - 循环：`REPEAT_MODE_ALL(0)` 显示 `Repeat`，`REPEAT_MODE_ONE(1)` 显示 `RepeatOne`（带“1”）。
   - 随机：`shuffleEnabled=true` 显示 `Shuffle`，`false` 显示 `FormatListBulleted`（三线+圆点列表，语义为顺序播放）。
   - 四枚按钮（手机 `mode-bar` 2 + 平板 `bottom-bar` 2）均同步生效，任一触发另一布局对应按钮同步。
2. **状态与视觉一致**：点击后立即乐观替换图标与 `active`，32ms 轮询经 `updateProgress` 真值回写后不闪回；真值与图标一致。
3. **跨布局一致**：手机与平板（`≥768dp && landscape`）均满足 1、2；窄屏/竖屏不回归。
4. **不破坏既有交互**：单击不触发横滑/下滑关闭/进度拖拽；其它 `onAction`（播放/切歌/队列/更多、seek）保持可用。
5. **日志可诊断**：保留 `bindClick ok / btn click / -> toggle*` 日志，回写后 `repeatMode/shuffleEnabled` 变化可追踪。

### 非功能 / 约束

- 不改 `PlayerConnection/MediaController` 契约，仅前端 JS/CSS 与桥接展示层。
- 保持 `WebViewAssetLoader + HARDWARE + TRANSPARENT + isPageReady` 闸门与 32ms 轮询节流。
- 新增 SVG 体积增量 <2KB，复用 `currentColor` 与 `20px` 尺寸。

## 验收标准

- [ ] 手机竖屏：点击 `mode-bar` 循环，图标在 `Repeat ↔ RepeatOne` 间切换，`active` 与真值同步；再次点击回切，重启沉浸页后保持。
- [ ] 手机竖屏：点击 `mode-bar` 随机，图标在 `FormatListBulleted(顺序) ↔ Shuffle(随机)` 间切换，`active` 同步；再次点击回切。
- [ ] 平板横屏：`bottom-bar` 循环/随机同上可切换且与手机侧同步。
- [ ] 快速连点以最后一次真值为准，不闪回；`adb logcat -s FullPlayer` 可见 `btn click toggleRepeat/Shuffle` 与 `-> toggle*` 及回写值。
- [ ] 横滑切面板、垂直下滑关闭、进度条拖拽、歌词 seek 无回归。
- [ ] 门禁：`assembleMusesDebug`、`lintMusesDebug` 通过。

## 范围外

- 不新增循环 `REPEAT_MODE_OFF` 第三态；不引入新 WebView 引擎或依赖。
- 不改服务端响度/队列/歌词解析链路。

## 已确认事实与决策

- 已确认：严格对齐 Compose 形态（`Repeat ↔ RepeatOne`，`Shuffle ↔ FormatListBulleted`），用户于 2026-09-01 确认“是”。
- 已确认：`full-player.js` 缺少 `RepeatOne` 与顺序图标且未在 `bindClick/updateProgress` 替换 `innerHTML`（见行号锚点）。
- 已确认：触摸分流与闭包已在前序任务修复，点击链路已可达 Kotlin。

## 技术备注

- 在 `full-player.js` 新增 `svgRepeatOne / svgOrder` 常量，抽 `setRepeatIcon(mode)/setShuffleIcon(enabled)` 复用；在 `bindClick` 乐观分支与 `updateProgress` 真值分支均调用。
