# 技术设计

## 边界

本任务修改播放器队列与控制器内部失败恢复协作：

- `queue.ts` 提供“失败恢复下一首”选择能力：忽略单曲循环，但继续使用当前 active order（普通队列或 `shuffleOrder`），并跳过本次恢复链已尝试的歌曲。
- `controller.ts` 在 `AudioPlayerNative.play` 确认失败且 generation 仍有效时启动或延续恢复链。
- 不修改公开队列配置、持久化格式、自然结束语义或原生插件。

## 失败恢复上下文

为一次自动恢复链维护内部上下文：

- `attemptedSongIds: Set<string>`：已经尝试过的歌曲。
- 上下文只通过 controller 内部调用传递，不写入 Vue state、localStorage 或日志。
- 用户直接调用 `playSong`、上一曲/下一曲、自然结束开始新的播放意图，不复用旧恢复上下文。

播放失败流程：

1. generation/songId 过期则直接返回，不推进。
2. 将当前歌曲加入 attempted 集合，并保留经过白名单过滤的安全错误文案。
3. 从当前 active order 中循环查找第一个未尝试候选；失败恢复选择忽略 `repeatMode='one'`。
4. 有候选则用同一恢复上下文调用内部播放；成功后状态为 playing、错误清空，恢复链自然结束。
5. 无候选则保持当前失败歌曲和安全 error 状态，清理缓冲与媒体会话，终止恢复。

## 队列契约

新增队列 helper（名称由实现确定）应：

- 使用 `shuffleOrder ?? items`，保证随机模式顺序一致。
- 从当前索引之后开始并允许回绕一次。
- 忽略 repeat-one 的“返回自身”语义。
- 跳过 attempted id；最多检查 active items 长度，绝不无限循环。
- 找到候选时更新 `currentIndex` 与响应式 queueState；找不到时返回 null。

## 并发与安全

- 继续以 `playGeneration` 和当前 songId 拦截快速手动切歌的旧失败。
- 自动尝试下一首会创建新 generation，使上一请求不能再写状态。
- 终止失败时才清媒体会话；继续恢复时避免异步 clear 与下一首 metadata 更新竞争。
- 原生异常文本仍只通过 `SAFE_PLAYBACK_ERRORS` 白名单进入 UI；恢复上下文不含 URL、header 或密码。

## 测试策略

- 第一首失败、第二首成功。
- 连续两首失败、第三首成功。
- 全部失败后终止，调用次数等于唯一候选数且错误安全。
- 单曲循环失败时跳过自身到下一首，同时配置仍为 one。
- 随机模式按 active shuffle order 推进。
- 旧 generation 失败不触发恢复。
- 现有自然结束、手动上下曲、预取和安全错误测试不回归。
