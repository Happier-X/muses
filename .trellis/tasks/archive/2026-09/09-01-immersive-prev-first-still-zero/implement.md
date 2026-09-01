# 实施计划 — 沉浸式上一曲首次仍回零

## 前置检查

- [ ] `prd/design` 已评审，明确 B 方案上叠加队首循环兜底

## 步骤

### 1. 代码

- [ ] 1.1 `PlayerConnection.kt` 重写 `skipToPrevious` 为 hasPrev -> seekToPrevious；else if repeatMode==ALL && count>1 -> seekTo(last,0)；else seekTo(0) 并加 Log
- [ ] 1.2 `FullPlayerWebView.kt` 的 `previous` 分支日志带 position/hasPrev/index（可选）

### 2. 联调

- [ ] 2.1 队列 3 首 REPEAT_ALL，队首首次点上一曲切队尾；REPEAT_OFF 队首回零；队中均切前一首
- [ ] 2.2 `adb logcat -s PlayerConnection:V -s FullPlayer:V` 核验分支

### 3. 验证

- [ ] 3.1 `assembleMusesDebug`
- [ ] 3.2 `lintMusesDebug`

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug
```

## 回滚点

- 回退 `PlayerConnection` 至 fca50e7d 版

## 产出

- 代码：`PlayerConnection.kt`、（可选）`FullPlayerWebView.kt`
