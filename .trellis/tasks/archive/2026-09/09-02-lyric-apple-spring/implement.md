# 实施计划 — 歌词 Apple Music 弹簧精调

## 0. 前置
- 基线：`c41e6e9b` 已有轻弹簧，`assembleMusesDebug` 通过

## 1. 有序清单
### Step 1 — 新建 AppleSpringPlacement（手搓 Lookahead）
- [ ] 新建 `feature/player/src/main/kotlin/com/muses/player/feature/player/lyric/AppleSpringPlacement.kt`，复刻 `SpringPlacementModifier` 逻辑（`ApproachLayoutModifierNode + DeferredTargetAnimation + spring(dampingRatio=1.1, stiffness)`），`isManualScrolling` 时 `snap()`，`stiffness 170..220` 动态
- [ ] 文件头注释来源与参数对齐说明，不直接 import vendored 渲染层
- 风险：需 `LookaheadScope` 包裹，Compose 1.7 已支持

### Step 2 — 列表级 Lookahead 包裹
- [ ] `NativeLyricsPanel.kt` 外层用 `LookaheadScope { LazyColumn(...) }`，每行 `Box(Modifier.appleSpringPlacement(lookaheadScope, itemKey=idx, isManualScrolling=isManualScrolling, stiffness=...))` 包裹 `NativeKaraokeLine`
- [ ] 保留已有的 `animateItem` 作为降级或移除（若与 Lookahead 冲突则移除）
- [ ] 滚动两处 `animateScrollToItem` 显式 `spring(stiffness=195f, dampingRatio=1.1f)`（对应 Apple duration 0.45/bounce 0.15）
- 风险：`LookaheadScope` 需在 Compose 1.7+ 启用，测试 MuMu 为 1.7+ 已满足

### Step 3 — 行焦点弹簧精调
- [ ] `NativeKaraokeLine.kt` 将 `animateFloatAsState` 参数由 `350/0.82` 等精调为 `scale 320/0.78, alpha 280/0.92, blur 300/0.9`，带 `visibilityThreshold`
- [ ] 验证 `graphicsLayer` 与 `blur` 绑定动画值

### Step 4 — 词级微弹簧
- [ ] 在 `NativeKaraokeLine` 的长词字符拆分中叠加 `DipAndRise/Swell`：手搓 `CubicBezier(0.33,1,0.68,1)`，`offsetY = 4.dp * dipTransform` 与 `scale = 1 + 0.05*swellTransform`，仅当前行生效
- [ ] 若性能敏感，加 `if(distance!=0) skip` 守卫
- 风险：低

### Step 5 — 校验与交付
- [ ] `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest`
- [ ] `adb -s 127.0.0.1:7555 install -r app/build/outputs/apk/muses/debug/app-muses-debug.apk` 后 MuMu 对比 Apple Music 官方录屏，确认 200ms 回位、无振荡拖尾、当前行轻微 overshoot

## 2. 验证命令
```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest
adb -s 127.0.0.1:7555 install -r app/build/outputs/apk/muses/debug/app-muses-debug.apk
```

## 3. 回滚
- `git revert` 单次提交回退至轻弹簧状态

## 4. 复核门
- [ ] PRD/Design/Implement 已对齐，Q1 已决策“完全一致”
- [ ] MuMu 上与 Apple Music 官方效果对比通过后归档
