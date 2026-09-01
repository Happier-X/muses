# 实施计划 — 沉浸式上一曲点击行为

## 前置检查

- [ ] 用户已确认期望语义（保留 3s 回零 vs 始终切上一曲）
- [ ] 分支基于 `main` 最新

## 步骤

### 1. 决策落地

- [ ] 1.1 若选 A（保留）：`PlayerConnection.kt` 保留阈值，仅补注释与日志；在 `FullPlayerWebView` 的 `previous` 分支日志带 `position` 与 `hasPrevious`
- [ ] 1.2 若选 B（始终切）：`PlayerConnection.kt` 移除 `>3000` 分支，改为 `hasPrevious -> seekToPrevious else seekTo(0)`

### 2. 联调

- [ ] 2.1 手机/平板各在 2s 与 5s 时点上一曲，验证 A/B 语义符合预期；下一曲不受影响
- [ ] 2.2 `adb logcat -s FullPlayer` 核验 `-> previous` 与 position 日志

### 3. 验证

- [ ] 3.1 `assembleMusesDebug`
- [ ] 3.2 `lintMusesDebug`

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug
```

## 回滚点

- 回退 `PlayerConnection.skipToPrevious` 至原 3s 实现

## 产出

- 代码：`core/media/PlayerConnection.kt`（按需）、`FullPlayerWebView.kt`（按需）
