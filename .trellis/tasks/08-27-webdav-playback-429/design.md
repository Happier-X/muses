# 技术设计 — WebDAV 播放 429

## 瓶颈
`WebDavClient` 直连 `openlist.happierx.xyz` 的 PROPFIND/GET 未限流；`PlaybackService` 的 `CacheDataSource` 对未知时长 mp3/flac 会发探测性 Range 切片（2~4 次），与刮削并发叠加后触发 Cloudflare 429。`ErrorLogStore` 未埋点，无法区分是 Cloudflare 还是源站限流。

## 方案
### 1. 共享限流
- `ScrapeRateLimiter` 提升为 `core:webdav` 共享单例（或 `core:scrape` 的实例经 Hilt 共享给 `WebDavClient`），`WebDavClient` 每次 `execute` 前 `acquire()`
- `CacheDataSource` 的 Range 探测复用同一限流器，或改为单次 GET 缓存后播（已验证可防风暴）

### 2. 429 感知
- 复用 `ScrapeHttp.parseRetryAfterMs` 逻辑，`WebDavClient` 命中 429 时 `delay(min(Retry-After,8s))` 重试 1 次，二次 429 抛 `IOException("http 429")`
- 抛错前 `errorLogStore.log(WARN, "WebDavClient", "http 429 url=...", e)`，`PlaybackService` 经 `playbackError` 展示

### 3. 可观测
- 播放页错误文案白名单新增「触发限流，稍后重试」
- 设置页反馈 `dump()` 可直接看到限流埋点

## 风险
- 限流过严导致首播延迟：首包 250ms 可接受
- 单例限流跨播放/刮削共享可能互相阻塞：评估分桶（读/写分离）或保持共享（全局 4 rps 足够）

## 回滚
移除限流器与重试分支即回退。
