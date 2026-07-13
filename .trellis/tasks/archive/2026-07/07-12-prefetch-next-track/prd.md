# 预取下一首歌曲

## Goal

当前歌曲播放期间后台预取队列中的下一首 WebDAV 歌曲；切到下一首时若完整缓存已就绪，则直接播放本地完整文件，缩短切歌等待并提升连续播放体验。

## Issue

- GitHub #16：`增加预取下一首的功能，这样能提高播放体验`

## Background / Confirmed Facts

### 队列

- `src/features/player/queue.ts` 维护顺序/随机后的活动队列与 `currentIndex`。
- 仅有会改变索引的 `advanceToNext()` / `advanceToPrevious()`，预取必须新增无副作用的 `peekNext()`。
- 单曲循环下一首是自身；列表循环到末尾回绕到首曲；随机模式使用 `shuffleOrder`。

### WebDAV 播放与缓存

- WebDAV 当前播放必须远程 URL + Basic Auth headers，禁止增长中的 `.partial` 文件。
- `WebDavAudioCache` 已有：
  - `getCachedFile(url)`：命中完整缓存
  - `getOrDownload(url, username, password)`：同步完整下载
  - `downloadInBackground(...)`：后台完整下载
  - 缓存目录与大小上限淘汰
- 安全原则：**后台完整下载；仅完整缓存可用于本地播放；未完成则远程直链**。

## Decisions

| 决策 | 结论 |
|------|------|
| 网络范围 | 所有网络允许预取 |
| 预取数量 | 只预取下一首 |
| 旧预取处理 | 旧预取继续完成；新下一首可并行启动；同 URL 复用缓存会话 |
| 单曲循环 | 不预取自身 |
| 用户设置 | 首版默认开启，无开关 |

## Requirements

1. **R1** 新增无副作用 `peekNext()`：按当前 repeat/shuffle 规则返回下一首，不修改 `currentIndex`。
2. **R2** 当前歌曲进入 `playing` 后触发预取调度；仅 WebDAV 下一首参与预取。
3. **R3** 单曲循环、下一首为空、下一首为本地时不启动预取。
4. **R4** 预取使用完整文件后台下载；密码仅在原生下载边界使用，不写日志/存储/状态。
5. **R5** 播放 WebDAV 时优先检查完整缓存：命中则用完整 `file://` 播放并视为 full buffer；未命中/非完整则远程直链，`bufferedPosition = null`。
6. **R6** 禁止播放增长中的 partial 文件；不得恢复渐进播放链路。
7. **R7** 预取失败/未完成不得阻塞当前播放或切歌。
8. **R8** 队列变更、随机切换、手动切歌后重新解析“下一首”；旧下载继续完成，新目标可并行启动，同 URL 不重复下载。
9. **R9** 本地歌曲不预取；已完整缓存的 WebDAV 下一首可跳过下载。
10. **R10** 预取遵守现有缓存上限与淘汰。
11. **R11** 补充单测：peekNext、触发时机、本地跳过、单曲循环跳过、缓存命中播放、未缓存回退远程、失败静默。
12. **R12** 同步 frontend/player 相关 spec。

## Acceptance Criteria

- [ ] AC1：列表循环/随机模式下 `peekNext()` 与 `advanceToNext()` 目标一致，但不改变索引。
- [ ] AC2：播放 WebDAV 当前曲后，会为下一首 WebDAV 启动后台完整下载。
- [ ] AC3：单曲循环、本地下一首、空队列不预取。
- [ ] AC4：完整缓存命中时切歌使用 `file://` 完整文件，`bufferedPosition` 视为满。
- [ ] AC5：无完整缓存时切歌仍远程直链，不阻塞。
- [ ] AC6：不播放 partial 文件，不调用渐进 prepare 作为播放路径。
- [ ] AC7：密码不进入 player state / localStorage / 日志。
- [ ] AC8：测试、lint、type-check 通过；spec 已同步。

## Out of Scope

- 多首预取
- Wi-Fi 限制 / 用户设置开关
- 自研原生流式播放器
- 预取上一首
- 删除渐进缓存原生代码
- 在线歌词预取

## Task Type

Complex
