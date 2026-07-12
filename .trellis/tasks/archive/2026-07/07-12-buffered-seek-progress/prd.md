# 进度条显示已缓冲区间并限制 seek

## Goal

播放时进度条展示「已缓冲」区域（类似视频播放器）；用户只能在已缓冲区间内拖动进度或点击歌词跳转，从源头避免 seek 到未缓冲区间导致的异常切歌，并给出清晰可预期的交互反馈。

## User Intent

- 第一次播放尚未完整缓存时，进度条应有已缓冲区域。
- 只能在已缓冲段来回调节。
- 歌词点击跳转遵守同一缓冲边界。
- **体验优先**：要边播边缓冲、启动尽量快，而不是「整首下完才能播」。

## Decision

**选定方案 A（体验最优）：真实/准真实缓冲进度 + 限制 seek。**

- 本地就绪文件：缓冲 = 全长，可任意 seek。
- 远程/WebDAV：边播边增长 `bufferedPosition`，UI 显示缓冲条，seek/歌词跳转 clamp 到已缓冲终点。
- 不改 `node_modules/@capgo/*`；远程缓冲上报走**项目自有原生能力**（见 `design.md`）。
- 保留既有 seek 保护窗 + 自然结尾判定作为第二道防线。

## Background / Confirmed Facts

- 进度条：`PlayerPage` `<input type="range">`，仅 `--progress`，无缓冲层。
- `seekPlayback` 只按 `duration` clamp，不读缓冲。
- `AudioPlayerNativeState.bufferedPosition` 类型已预留，未接线。
- capgo native-audio 公开 API **无** buffered 上报；远程为 ExoPlayer `RemoteAudioAsset`。
- 本地 `content://` 经 `prepareLocalAudioFile` 拷贝后可视为全长可 seek。
- WebDAV 当前远程 URL + Basic Auth 流式播放；`WebDavAudioCache` 整文件下载未与播放缓冲 UI 联动。

## Requirements

1. **R1** 进度条三层视觉：未缓冲底轨 / 已缓冲 / 已播放（已播放 ≤ 已缓冲 ≤ 总时长）。
2. **R2** 拖动 seek：目标 > `bufferedPosition` 时 clamp 到 `bufferedPosition`（不得落到未缓冲区）。
3. **R3** 歌词点击：目标 > `bufferedPosition` 时不 seek；可轻提示「缓冲中」（文案可定，默认不阻塞 UI）。
4. **R4** 本地就绪：`bufferedPosition = duration`，缓冲条铺满。
5. **R5** 远程/WebDAV：开播后 `bufferedPosition` 从近 0 随缓冲增长至 `duration`；UI 与限制实时更新。
6. **R6** 缓冲未知时：不画假缓冲条；seek 退化为 duration clamp + 既有 finished 防护（并在日志/注释标明降级）。
7. **R7** 切歌 / stop / 播放失败时重置 `bufferedPosition`，禁止串曲。
8. **R8** 开播尽量快：远程不得强制「整首下载完成才开始播放」（体验最优约束）。
9. **R9** 单测：clamp、歌词拒绝、本地全缓冲、切歌重置、缓冲增长驱动 UI 状态。
10. **R10** 同步 `features-player.md` / `component-guidelines.md` / `state-management.md`。

## Acceptance Criteria

- [ ] AC1：有缓冲数据时进度条可见已缓冲区域，且已播放不超过已缓冲视觉终点。
- [ ] AC2：拖动到未缓冲区时，实际 `seekPlayback` 参数 ≤ `bufferedPosition`。
- [ ] AC3：歌词点击未缓冲时间码不改变有效播放位置（不发起越界 seek）。
- [ ] AC4：本地就绪曲目缓冲满条，可全长 seek。
- [ ] AC5：WebDAV/远程开播后缓冲条可增长；增长过程中 seek 上限跟随增长。
- [ ] AC6：切歌后新曲缓冲从该曲自己的状态开始，不继承上一首。
- [ ] AC7：远程首包可播后应能开始播放（非整文件下完才播）。
- [ ] AC8：单测 + lint + type-check 通过；spec 已更新。

## Out of Scope

- 修改 `node_modules/@capgo/*` 源码。
- 多段缓冲空洞（只建模「从 0 连续到 bufferedPosition」）。
- 缓存管理设置页 / 清理 UI。
- 改队列循环随机语义。
- iOS（当前以 Android 为主；Web 降级可本地全缓冲或未知）。

## Task Type

Complex：必须有 `design.md` + `implement.md`。
