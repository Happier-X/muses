# 执行清单 — WebDAV 播放 429（简化方案）

> 验证：`gradlew :core:webdav:compileDebugKotlin :core:media:compileDebugKotlin :app:assembleMusesDebug`
> 思路：保持流式播放 + 边播边缓存，消除突发 burst，而非整文件下载。

## 已实施
- [x] 1. `WebDavModule` 新增 `@StreamingOkHttp` 限定符 client（只 auth、不限流）
- [x] 2. `PlaybackService` 注入 `@StreamingOkHttp okHttpClient` 供 `OkHttpDataSource` 流播；原限流 client 仍供 `WebDavClient`（扫描/预取）
- [x] 3. 移除 media3 1.11 不可用的 `setPreloadItems`（默认不预加载相邻 item）

## 待实测（按需）
- [ ] 4. 若恢复队列（465 首）冷启动仍 429：改 `applyPlayback`/`restoreFromSnapshot` 为「只 prepare 当前曲 + 下一首」分批加载 + `PlayerConnection` 维护 UI 队列副本（解耦 ExoPlayer 队列）
- [ ] 5. 抓 `ErrorLogStore.dump()` 确认 Cloudflare `/dav/` 阈值；按需调限流阈值 / 播放优先错峰
- [ ] 6. `lazyScanTags`（若实现）改读本地缓存文件，禁对 WebDAV URL 发请求

## 门禁
- [x] :core:webdav + :core:media compileDebugKotlin 通过
- [ ] assembleMusesDebug 装机：单首点播不 429 + 恢复队列不 429 + 429 可自愈
