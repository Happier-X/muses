# 实施计划 — 沉浸式播放页底部图标未随模式切换

## 前置检查

- [ ] `prd.md` / `design.md` 已评审，图标形态对齐 Compose 已确认
- [ ] 分支基于 `main` 最新，无未提交脏改

## 步骤

### 1. 资源与封装

- [ ] 1.1 `full-player.js` 顶部新增 `svgRepeatOne`（RepeatOne）与 `svgOrder`（FormatListBulleted 顺序）常量，与现有 `svgRepeat/svgShuffle` 并列，尺寸 20px `currentColor`
- [ ] 1.2 新增 `setRepeatIcon(mode)` 与 `setShuffleIcon(enabled)` 函数，遍历 `['btn-repeat','bottom-repeat']` / `['btn-shuffle','bottom-shuffle']` 替换 `innerHTML` 并 `toggle('active')`

### 2. 链路接入

- [ ] 2.1 `initDom` 末尾（`bindClick` 之后）初始化调用一次 `setRepeatIcon(state.repeatMode); setShuffleIcon(state.shuffleEnabled);`
- [ ] 2.2 `bindClick` 中 `toggleRepeat/toggleShuffle` 乐观分支改为先改 `state` 再调对应 `set*Icon`，移除原单 `toggle('active')`
- [ ] 2.3 `updateProgress` 中 `repeatMode/shuffleEnabled` 真值分支改为 `set*Icon` 覆验，移除原单 `toggle`；保留 `state` 同步

### 3. 联调与自测

- [ ] 3.1 日志验证：点击后 `btn click toggleRepeat/Shuffle` 与 `updateProgress repeatMode/shuffleEnabled` 回写一致，图标形状即时切换且不闪回
- [ ] 3.2 功能回归：横滑切面板、垂直下滑关闭（顶部可、底部不误关）、进度条、歌词 seek、播放/切歌、队列/更多
- [ ] 3.3 布局回归：手机竖屏/窄屏、平板横屏封面/进度/控制区无错位

### 4. 本地验证

- [ ] 4.1 编译：`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug`
- [ ] 4.2 Lint：`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:lintMusesDebug`
- [ ] 4.3 可选单测：`./gradlew :feature:player:testDebugUnitTest`（如受影响）

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug
```

## 回滚点

- 若图标切换引入闪回或路径错误，回退 `full-player.js` 至仅 `active` 切换，保留链路其余修复

## 产出

- 代码：`app/src/main/assets/amll/full-player.js`（必改），`full-player.css`（按需）
- 产物：`app-muses-debug.apk` 可装机验证
