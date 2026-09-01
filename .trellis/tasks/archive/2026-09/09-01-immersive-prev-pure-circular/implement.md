# 实施计划 — 纯队列循环

## 前置检查

- [ ] 用户已确认纯循环语义

## 步骤

### 1. 代码

- [ ] 1.1 重写 `PlayerConnection.skipToPrevious/skipToNext` 为环形索引 + 日志
- [ ] 1.2 同步 `spec §16` 为纯循环契约

### 2. 验证

- [ ] 2.1 `assembleMusesDebug`
- [ ] 2.2 `lintMusesDebug`
- [ ] 2.3 手工：2分钟与0s时分别点上一曲/下一曲均按队列前后循环

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug
```

## 产出

- `PlayerConnection.kt`
