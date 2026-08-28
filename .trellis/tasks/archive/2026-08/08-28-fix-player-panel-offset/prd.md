# 沉浸式播放页面板半宽偏移修复

## Goal

修复 `PlayerScreen` 在 `TabsLayout.PhoneLayout` 主内容区内 `BoxWithConstraints.maxWidth` 被约束为半屏（`~180dp / 540px`）导致 `panels` 的 `requiredWidth = maxWidth*2` 仅 `360dp`、左右面板并排各占半屏的“偏了”问题，恢复单屏全宽 + 0.22s 横滑的 Capacitor 1:1 行为。

## 背景

- 复现：`0321 - space x` 等无词曲目在 MuMu `1080x1920` 上进入沉浸页后，左侧灰色封面占位仅半块在屏、右侧“暂无”文案被挤到右半屏并排可见（见 `pi-clipboard-fb9bf1fe` 截图）；`uiautomator` 显示左侧 `ScrollView` `480px`、右侧 `540px`，容器的 `Row` 宽 `~1080px` 而非预期的 `2160px`
- 已验证：`TabsLayout.PhoneLayout` 的主内容 `Box(offsetX)` 在关抽屉态 `offset 0` 时仍对子测量施加半屏约束的嫌疑；`mumu_melox.png` 对照显示封面应居中且单屏占满
- 影响：固定头部/指示器正常，但封面/小窗/歌词空态均被半宽截断，横滑基线错误

## Requirements

### R1: 布局恢复全宽
- `PlayerScreen` 的双面板轨道宽度必须以**全屏宽度**为基准：`trackWidth = screenWidth * 2`，`panelWidth = screenWidth`，`translateX = -activePanel * screenWidth`
- 不依赖 `BoxWithConstraints.maxWidth` 在 `TabsLayout` 子树内的局部约束；改以 `LocalConfiguration.screenWidthDp` 或 `onSizeChanged` 实测的全屏宽为准

### R2: 行为保持
- `PhoneImmersiveLayout`：`activePanel 0→1` 的 `0.22s easeOut` 横滑、`40px` 阈值、指示器 `20x6` 选中态、垂直下滑守卫（`isLyricPanelActive`）均保持
- `TabletImmersiveLayout`：`≥768 && 宽>高` 双栏 + 底部条不受影响；`MeloXFlowingLightBackdrop / MeloXIOSLyricsPanel / MetaWindow` 不改

### R3: 回归验证
- `0321 - space x`（无词/无封面空态）与 `FINE AS HELL`（有封面/有词）两类在手机竖屏下均单屏居中、无并排
- `isLyricPanelActive` 下滑守卫、`canSeek` 禁用、`navigationBarsPadding` 仍生效

## Acceptance Criteria

- [x] MuMu `1080x1920` 手机竖屏：info 面板封面 `272dp` 居中占满单屏，lyric 面板“暂无”居中占满单屏，左右滑 `0.22s` 切换无重叠，`uiautomator` 中单面板 `ScrollView` 宽 `≈1080px`（而非 `480px`）
- [x] 横滑与下滑手势：info 面板可下滑关闭，lyric 面板内下滑不触发关闭；`--:--` 时进度条禁用
- [x] 平板横屏（若可测）与无词/有词切换不闪底，粘性封面沿用正常
- [x] `./gradlew :feature:player:assembleDebug :app:assembleMusesDebug` 通过

## Out of Scope

- `TabsLayout` 抽屉本身的手势/动画不改（仅保证其对 `PlayerScreen` 的子测量不截半）
- 歌词逐词渲染与流体背景不改

## 验证
- MuMu `1080x1920` 上 `0321 - space x` 与 `All Time Low` 均单屏居中，封面 `272dp` 居中，`暂无歌词` 居中，`HorizontalPager` 横滑 0.22s 无重叠，`uiautomator` 单面板宽 `1080px`
- `HorizontalPager` 已替代原 `Row 200%`，`screenWidth` 来自 `LocalConfiguration` 全屏宽，避免 `TabsLayout` 半屏约束

## Notes

- 真源：`PlayerPage.vue@de7e388f^` 的 `panels 200%` 语义；现状改动点集中在 `feature/player/PlayerScreen.kt` 的 `BoxWithConstraints` → 全屏宽
