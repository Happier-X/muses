# 设计 — 移除歌曲信息页五行歌词

## 1. 背景

- 需移除 `full-player.js` 的 `renderMetaWindow/findCurrentIndex/meta-window` DOM 与调用、`full-player.css` 的 `.meta-window` 样式、`PlayerScreen.kt` 的 `InfoPanel->MetaWindow` 调用与 `MetaWindow` 私有组件。

## 2. 方案

- **JS**：删除 `renderMetaWindow`、`findCurrentIndex`（若仅为该预览服务）、`initDom` 中 `metaWindow/metaViewport/metaList` 创建、`updateProgress` 末尾 `renderMetaWindow()`、`wrapLyrics` 中 `state.lines` 同步与 `renderMetaWindow()`；保留 `state.lines` 仅作无害占位或一并清理。
- **CSS**：删除 `/* 五行小窗 */` 段 `meta-window/meta-viewport/meta-list/meta-row` 及其两处媒体查询覆盖。
- **Kotlin**：`PlayerScreen.kt` `InfoPanel` 中移除 `MetaWindow(...)` 调用与 `Spacer` 紧随的 `innerGap+18.dp` 的预览相关间距；保留 `parsedLines` 收集但移除传参，或保留收集以备 `FlowingLightBackdrop` 的 `hasLyric` 判断（该判断可改用 `syncedLyrics != null`，本任务暂不改逻辑，仅移除 UI）。
- `MetaWindow` 私有函数（`@Composable private fun MetaWindow`）整段删除，相关 `animateFloatAsState` 与 `computeCurrentIndex` 若仅为该组件服务亦可清理。

## 3. 涉及文件

- `app/src/main/assets/amll/full-player.js`
- `app/src/main/assets/amll/full-player.css`
- `feature/player/src/main/kotlin/com/muses/player/feature/player/PlayerScreen.kt`
- `spec/android/features-lyrics-playlist.md §7`（移除 `info-panel/meta-window` 行描述）

## 4. 验证

- 真机/模拟器上沉浸式信息页无五行预览，封面居中与控制区无重叠；右侧歌词正常。
- 编译与 lint 通过。

