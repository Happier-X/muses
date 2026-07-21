# 重启后恢复上次播放信息 (#49)

## Goal

应用进程被杀或用户完全退出后再次打开，应恢复上次的播放上下文（队列位置、当前曲、进度等），而不是回到「无当前曲 / 空播放态」。

## Background

已有持久化：

- 队列：`muses:queue`（`items` / `originalOrder` / `shuffleOrder`）
- 播放配置：`muses:player-config`（repeat / shuffle / loudness）

**未**持久化（代码现状）：

- `currentIndex`（内存，启动为 `-1`）
- 当前曲 id、播放进度 `position`、是否曾在播放

因此冷启动即使队列有歌，也不会自动成为「当前曲」，用户需重新点播。

## Requirements（草案，待确认 R3）

### R1. 恢复队列指针
- 持久化并恢复 `currentIndex`（或等价的 `currentSongId` + 索引解析）。
- 启动时若队列非空且上次曲目仍在曲库中，则 `queueState.currentIndex` 指向该曲。

### R2. 恢复当前曲展示与进度
- 冷启动后应能展示上次当前曲（沉浸式可打开 / MiniPlayer 有内容）。
- 恢复上次 `position`（在合法 duration 范围内 clamp；未知 duration 时按 0 或已存值合理处理）。
- 曲目已从曲库删除：跳过该曲，回退到队列下一首可用项或清空当前曲（不得崩溃）。

### R3. 恢复后的播放态（已确认：A）
- 冷启动一律恢复为 **暂停** 在上次进度；用户手动点播放再出声。
- 不自动续播（避免 Android 无用户手势时的音频焦点/自动播放限制）。

### R4. 持久化时机与隐私
- 在切歌、seek 成功、暂停/播放状态变化、队列索引变化时更新快照；可用节流避免高频写 `position`。
- 仅本地 `localStorage`（与现有 queue/config 一致）；不上传。
- `stopPlayback` 清空当前曲时，应清除或标记无效快照，避免「停止后重开仍显示已停止曲」的歧义（若产品希望 stop 后仍保留，可在实现前再确认；默认 **stop 清空当前播放记忆，仅保留队列**）。

### Out of Scope
- iCloud / 多端同步
- 恢复在线歌词匹配中间态、缓冲会话
- 修改 capgo 插件
- 发版

## Acceptance Criteria

- [ ] 有队列且有上次当前曲时，杀进程再开可看到同一当前曲与接近的进度
- [ ] 队列列表本身仍按现有逻辑恢复
- [ ] 曲目缺失时安全降级
- [ ] 相关单测覆盖序列化/恢复；lint / build 通过
- [ ] 关闭 GitHub #49

## Ordering Note

建议在 #47（进度同步）合并后再实现本任务，避免把错误进度写入持久化。
