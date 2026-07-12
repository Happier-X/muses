# 修复 WebDAV 播放数秒后自动下一曲

## Goal

修复 WebDAV 歌曲播放数秒后因渐进文件临时 EOF 被误判为自然结束并自动跳到下一曲的严重回归，优先恢复稳定播放。

## Issue

- GitHub #14：`现在歌曲会在播放的时候播放几秒就播放下一曲了`

## Confirmed Facts

- 仅 WebDAV 音源发生；本地音源正常。
- v0.0.8 起，WebDAV 先渐进写入 `.partial` 文件，约 256KB 后把 `file://` 交给 NativeAudio，后台继续追加。
- NativeAudio 可在读到当前文件尾时发 `complete`；部分文件探测出的短 duration 又可能让前端“接近结尾”校验通过，触发自动下一曲。
- v0.0.7 之前 WebDAV 直接用远程 URL + Basic Auth headers 播放，未出现该临时 EOF 问题。
- NativeAudio 支持远程 URL 与自定义 HTTP headers。

## Decisions

| 决策 | 结论 |
|------|------|
| 修复策略 | WebDAV 回退远程直链流式播放（URL + Basic Auth headers） |
| 缓冲进度 | WebDAV `bufferedPosition = null`，不显示假缓冲层 |
| seek | 缓冲未知时沿用 duration clamp；不按伪缓冲限制 |
| 本地播放 | 保留本地 full buffer 与完整 seek |
| 原生渐进代码 | 可保留但播放链路不调用；避免本次扩大删除面 |

## Requirements

1. **R1** WebDAV 播放必须直接使用远程 URL + Authorization header，不使用增长中的本地 `.partial` 文件。
2. **R2** WebDAV 歌曲正常播放数秒不得收到临时 EOF 导致的自动下一曲。
3. **R3** 真正自然播完仍按既有 near-end + seek guard 规则自动下一曲。
4. **R4** WebDAV `bufferedPosition` 保持 `null`；UI 不画假缓冲条，seek 按 duration clamp。
5. **R5** 本地音源保持 full buffer、完整 seek 与既有播放行为。
6. **R6** 切歌/停止仍取消可能存在的旧 buffer session，兼容从旧 APK/旧会话升级。
7. **R7** 不通过扩大 finished 保护窗掩盖根因。
8. **R8** 补充回归测试：WebDAV resolve 走远程 URL/headers、不调用 `prepareWebDavAudioFile`、缓冲未知；本地行为不变。
9. **R9** 同步 frontend spec，撤销“WebDAV 渐进 file:// 起播”的现行约定。

## Acceptance Criteria

- [ ] AC1：WebDAV 播放选项传入 NativeAudio 的 `assetPath` 为远程 URL，并带 Basic Auth header。
- [ ] AC2：不调用 `prepareWebDavAudioFile`，不会播放增长中的 `.partial` 文件。
- [ ] AC3：WebDAV 播放状态 `bufferedPosition` 为 `null`，UI 无伪缓冲层。
- [ ] AC4：WebDAV 真正自然结束仍自动下一曲。
- [ ] AC5：本地播放/缓冲/seek 测试无回归。
- [ ] AC6：相关测试、lint、type-check 通过；spec 已同步。

## Out of Scope

- 自研原生流式数据源或替换 NativeAudio
- WebDAV 真实缓冲进度
- 删除全部渐进缓存原生代码
- 调整在线歌词匹配

## Task Type

Complex
