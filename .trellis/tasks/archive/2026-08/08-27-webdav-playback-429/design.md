# 技术设计 — WebDAV 播放 429（简化方案 · 已实施）

> 结论：保持 ExoPlayer 直连 WebDAV 流式播放 + CacheDataSource 边播边缓存（现状已满足），
> tag 由 ExoPlayer 流播时自己解析（现状已满足）。不需要「整文件下载 + file:// 播」那套复杂机制。
> 真正导致 429 的是「流播 Range 被 4 rps 限流饿死」+（可能）「恢复队列全量 prepare 发全列请求」。

## 一、现状（已具备，无需改动）
- `PlaybackService` 用 `OkHttpDataSource(okHttpClient)` + `CacheDataSource`：ExoPlayer 直连 WebDAV URL 流式播放，边播边把数据缓存到 SimpleCache；重复播放命中本地，不发网络。
- 流播时 ExoPlayer 自行解析容器内 ID3 tag 显示标题/时长，无需额外网络请求。

## 二、根因（为什么还 429）
1. **流播 Range 被 4 rps 限流饿死**（主因，已修复）：原 `okHttpClient` 套了 `acquireBlocking` 限流拦截器，流播的 Range 请求与扫描/预取共享同一 4 rps 桶，流播被饿死或超时重试 → 叠加请求 → 429。流播是单连接持续读取，本不应被节流。
2. **恢复队列全量 prepare**（次因，按需）：media3 1.11 无相邻预加载 API（`setPreloadItems` 在 1.13+），默认不会预加载整队列；但若 `setMediaItems(465首)` + `prepare()` 在 1.11 仍对每个 period 发初始请求，则会打爆服务端。待实测确认。

## 三、已实施方案（2026-08-27）
### 1. 流播 client 剥离 4 rps 限流（核心止血）
- `WebDavModule` 新增 `@StreamingOkHttp` 限定符 client：只注入 Basic Auth、不施加 4 rps 限流，专供 `OkHttpDataSource` 流播。
- `PlaybackService` 注入 `@StreamingOkHttp okHttpClient`；原带限流的 client 仍供 `WebDavClient`（扫描/预取）使用。
- 限流只作用于扫描/批量预取等可能并发突发的链路，流播单连接读取不受限。

### 2. 移除不可用的 setPreloadItems
- media3 1.11.0 的 `Player`/`ExoPlayer` 均无 `setPreloadItems`（javap 已确认），编译失败已移除；默认不预加载相邻 item。

## 四、待实测验证（按需）
- 若实测恢复队列（465 首）冷启动仍 429，说明 `setMediaItems(全量)` + `prepare()` 仍发全列请求 → 改为「只 prepare 当前曲 + 下一首」分批加载，并让 `PlayerConnection` 维护完整 songs 列表对 UI 暴露（解耦 UI 队列与 ExoPlayer 队列）。
- 抓 `ErrorLogStore.dump()` 确认 Cloudflare `/dav/` 真实阈值；若阈值确实低，调整限流阈值 / 让播放优先于后台扫描错峰。
- `lazyScanTags`（若后续实现）必须读本地缓存文件，不再对 WebDAV URL 发网络请求。

## 五、风险与权衡
- 流播完全不限流：单设备单用户同时仅 1 个流播（1.11 无相邻预加载），请求量极小，正常不会触发 Cloudflare；真正 burst（多首并发/扫描叠加）已通过「扫描/预取限流 + 流播隔离」消除。
- 回退：移除 `@StreamingOkHttp` 注入、改回原 `okHttpClient` 即回旧限流行为（core:webdav 编译已验证）。

## 六、验收
- [ ] 单首 WebDAV 点播：直接流播出声，不再稳定 429
- [ ] 465 首恢复队列启动不触发 429（默认行为或分批 prepare）
- [ ] 已 429 时进入冷却，冷却后重试可恢复（自愈）
- [ ] testDebugUnitTest 全绿，ErrorLogStore 可查限流/冷却埋点
