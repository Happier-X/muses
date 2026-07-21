# 设计：#50 应用卡顿

## 已识别热点（代码审查）

| 热点 | 证据 | 本轮策略 |
|------|------|----------|
| 进度轮询 250ms + bridge + 全量 emit | `native.ts` POSITION_POLL；`applyNativeState` 每次写 position 并 `syncMediaSessionState` | 降频至 500ms；position 变化 &lt; 0.05s 不 emit；media position 单独节流 1s |
| console.info 诊断日志 | `logNativeAudio` 始终 info | 默认静默；`localStorage muses:debug-native-audio=1` 才打 |
| Songs 全量 v-for | `SongsPage` 无 virtual | 对齐 Sources，用 `@tanstack/vue-virtual` |
| AMLL | 沉浸式必需 | 本轮不拆；仅避免进度 tick 放大连带渲染 |

## 不回归

- #47：进度条/时间仍随播放更新（500ms 对 UI 足够；事件路径仍在）
- #49：会话节流写盘逻辑保留
- 媒体通知进度：1s 更新仍可用

## 回滚

还原 poll 间隔、日志、media 节流、Songs 虚拟列表即可。
