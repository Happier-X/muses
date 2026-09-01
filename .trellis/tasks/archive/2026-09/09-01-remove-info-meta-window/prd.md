# 移除歌曲信息页五行歌词

## 目标

移除沉浸式信息页（`info-panel`）的五行歌词预览（`meta-window` 五窗）功能，仅保留右侧完整歌词面板；信息页仅保留封面、进度、控制与模式栏。

## 背景

- 现 `app/src/main/assets/amll/full-player.js:60,125-131,485,488-495` 实现 `renderMetaWindow` 与 `meta-window/meta-list` DOM，并由 `updateProgress/wrapLyrics` 驱动；`full-player.css:352-403` 提供 `meta-window` 样式；`feature/player/PlayerScreen.kt:729,816-895` 的 `InfoPanel -> MetaWindow` 与 `parsedLines` 驱动 Compose 五窗。
- 用户反馈该预览与完整歌词重复且占用信息页空间，要求删除。
- 关联契约：`features-lyrics-playlist.md §7` 信息页布局；`parsedLines` 仍由 `PlayerViewModel` 提供但仅用于该预览。

## 需求

### 功能需求

1. **移除预览**：沉浸式信息页不再渲染五行预览，手机与平板信息页均不显示 `meta-window`。
2. **保留完整歌词**：右侧 `lyric-panel` 的 `amll-lyric-player` 完整歌词不受影响。
3. **布局自适应**：移除后信息页封面与控制区垂直间距保持协调，无空白塌陷或溢出。

### 非功能 / 约束

- 不改 `PlayerViewModel.parsedLines` 数据产出（保留以备它用），仅移除 UI 消费。
- 保持 `full-player.js` 其余交互（`updateProgress` 进度、图标、横滑）与 `FullPlayerWebView` 32ms 回写不变。

## 验收标准

- [ ] 沉浸式信息页（手机与平板）不再出现五行预览，信息页仅封面+进度+控制+模式栏。
- [ ] 右侧完整歌词可正常渲染与滚动，`seek` 与翻译开关正常。
- [ ] `assembleMusesDebug`、`lintMusesDebug` 通过。

## 范围外

- 不改响度/播放列表/队列切歌逻辑。
