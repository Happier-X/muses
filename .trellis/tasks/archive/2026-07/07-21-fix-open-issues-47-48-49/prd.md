# 修复 open issues #47 #48 #49

## Goal

关闭三个已确认的 open issues，恢复播放体验中的进度同步、沉浸式页信息噪音清理与冷启动播放记忆。

## Background

| Issue | 标题 | 优先级建议 |
|-------|------|------------|
| #47 | 进度条播放时不随时间填充；seek 后时间与进度条不动 | P0 |
| #48 | 沉浸式播放页不展示「歌曲信息补充中/失败」等提示 | P2 |
| #49 | 应用重新打开时应保留上次播放信息 | P1 |

现状要点（规划期代码勘察）：

- 队列列表已持久化（`muses:queue`），但 `currentIndex` **未**持久化，冷启动恒为 `-1`。
- 播放位置 / 当前曲 **未**持久化。
- 进度 UI 绑定 `playerState.position`；原生经 `@capgo/capacitor-native-audio` 的 `currentTime` 事件推进。
- 沉浸式页 `PlayerPage.vue` 在曲名下直接展示 `metadataStatus === scanning|failed` 文案。

## Child Task Map

| 子任务 | 目录 | 交付物 | 依赖 |
|--------|------|--------|------|
| #47 进度条同步 | `07-21-fix-progress-bar-sync` | 播放/seek 后进度与时间持续更新 | 无 |
| #48 隐藏补充提示 | `07-21-hide-immersive-enrichment-toasts` | 沉浸式页去掉元信息补充提示 | 无 |
| #49 恢复播放信息 | `07-21-restore-playback-on-reopen` | 冷启动恢复队列指针与上次曲目/进度 | 建议在 #47 后做（进度正确再持久化更稳） |

父任务本身不写业务代码；负责跨子任务验收与最终整合（关闭 issue、journal）。

## Cross-child Acceptance Criteria

- [ ] GitHub #47 / #48 / #49 均已修复并关闭
- [ ] 三子任务各自验收标准通过
- [ ] lint / unit / build 全绿
- [ ] 不回归：seek 缓冲上限、自然结束切歌、媒体通知、音量均衡

## Out of Scope（父级）

- 发版 / 升 version
- 修改 capgo 插件源码（`node_modules/@capgo/*`）
- 跨设备同步播放状态
