# 实施计划 — 歌词弹簧动画补齐

## 0. 前置
- 基线：已完成原生重构 `17009f3e`，`assembleMusesDebug` 通过

## 1. 清单
### Step 1 — 滚动弹簧
- [ ] `NativeLyricsPanel.kt` 将两处 `animateScrollToItem(currentIndex)` 改为 `animateScrollToItem(currentIndex, animationSpec = spring(stiffness=400f, dampingRatio=0.82f))`
- [ ] 保留 `isScrollInProgress` 防抖与 3s 回中同样用 spring
- 风险：无

### Step 2 — 行 placement 弹簧
- [ ] `LazyColumn` 的 `itemsIndexed` 为每行外层 `Box/Column` 添加 `Modifier.animateItem(placementSpec = spring(stiffness=300f, dampingRatio=0.75f))`
- [ ] 确认 `key = idx` 稳定
- 风险：需 Compose 1.7+，已满足

### Step 3 — 行焦点弹簧
- [ ] `NativeKaraokeLine.kt` 将 `lineAlpha/lineScale/blurRadius` 的静态计算改为 `animateFloatAsState(targetValue=..., animationSpec=spring(...))` 与 `animateDpAsState`
- [ ] 绑定 `graphicsLayer(scale/alpha)` 与 `blur` 到动画值
- 风险：`blur` 动画需 API 31+，低版本无动画但不崩

### Step 4 — 词级微弹跳（可选）
- [ ] 若实现：在 `NativeKaraokeLine` 的词级 `fraction` 上叠加 `floatOffset`/`scale` 的 `DipAndRise` 变换（reuse `CubicBezier` 近似），首版可简化为仅对当前词 `scale 1.03`
- [ ] 若跳过：留 TODO 注释
- 风险：低

### Step 5 — 校验
- [ ] `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest`
- [ ] `adb -s 127.0.0.1:7555 install -r app/build/outputs/apk/muses/debug/app-muses-debug.apk` 后 MuMu 手动验收弹簧手感

## 2. 验证命令
```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest
adb -s 127.0.0.1:7555 install -r app/build/outputs/apk/muses/debug/app-muses-debug.apk
```

## 3. 回滚
- `git revert` 单次提交即可回退弹簧参数至 tween/瞬切

## 4. 复核门
- [ ] PRD/Design/Implement 已对齐，无 Open Questions
- [ ] 用户在 MuMu 上确认手感后归档
