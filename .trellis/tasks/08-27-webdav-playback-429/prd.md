# WebDAV 播放 429 持续拦截排查与修复

> 背景：刮削 429 已通过 08-27-scrape-throttle-429 限流治好；但 WebDAV 歌曲「点播即 429」且过夜不恢复，说明是播放链路 `WebDavClient`/`CacheDataSource` 的持续限流，非瞬时 burst。

## Goal
定位并解除 WebDAV 播放的持续 429：单次点播不再触发限流，已限流时可自愈，日志可观测。

## 范围
1. 复现与归因：抓 `ErrorLogStore.dump()` 与 `OkHttp` 日志，区分是 Cloudflare WAF/Rate Limit 还是源站 OpenList 限流；确认是 Range 切片风暴还是单 GET 即 429
2. 客户端限流：`WebDavClient` 共享 `ScrapeRateLimiter` 或独立播放限流（≤4 rps），探测性 Range 请求合并/复用缓存
3. 退避与可观测：429 时解析 `Retry-After` 退避并 `ErrorLogStore.log(WARN)`，播放页消费 `playbackError` 展示「触发限流，稍后重试」
4. 服务端指引：给出 Cloudflare 对 `/dav/*` 跳过限流/WAF 的配置清单（用户自建站自助）

## 非范围
- 刮削链路（已治）
- 服务端阈值调大以外的业务改动

## 验收标准
- [ ] 本地歌曲点播不受影响
- [ ] 单首 WebDAV 歌曲点播不再稳定 429（或 429 后有退避提示且重试可恢复）
- [ ] `testDebugUnitTest` 全绿，`ErrorLogStore` 可查到限流埋点
