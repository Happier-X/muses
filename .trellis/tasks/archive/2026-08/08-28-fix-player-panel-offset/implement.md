# 实施计划 — 沉浸式面板半宽偏移修复

## 清单

1. **复现确认**
   - [x] 已复现：`0321 - space x` 截图与 `uiautomator` `480px` 半宽

2. **代码修复（单文件）**
   - [ ] `PlayerScreen.kt`：`BoxWithConstraints` → 以 `LocalConfiguration.screenWidthDp.dp` 为 `screenWidth` 基线
   - [ ] `Row(requiredWidth = screenWidth*2f)`、`Box(width = screenWidth)`、`offset = (panelOffset * screenWidthPx *2).roundToInt()`
   - [ ] 保留 `isLyricPanelActive`/`canSeek`/`navigationBarsPadding` 等 1:1 补漏

3. **构建与真机**
   - [ ] `./gradlew :feature:player:assembleDebug :app:assembleMusesDebug`
   - [ ] `MuMu 1080x1920`：`0321` 空态与 `FINE AS HELL` 有词各验证单屏居中、横滑 0.22s 无重叠、`uiautomator` 宽 `~1080px`
   - [ ] 下滑守卫与进度禁用回归

4. **收尾**
   - [ ] `prd` 验收打勾，`task.py archive`（`--skip-branch-validation --no-commit`）

## 验证命令

```bash
./gradlew :feature:player:assembleDebug :app:assembleMusesDebug
adb -s emulator-5556 install -r app/build/outputs/apk/muses/debug/app-muses-debug.apk
adb -s emulator-5556 shell uiautomator dump /sdcard/dump.xml
# 预期 ScrollView 宽 960-1080px 而非 480px
```

## 风险与回滚

- 单文件改动，`git restore` 即回滚；不改 `TabsLayout`，不影响抽屉
