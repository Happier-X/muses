# 执行清单 — WebDAV 播放 429

> 验证：`./gradlew :core:webdav:testDebugUnitTest :core:scrape:testDebugUnitTest :app:assembleMusesDebug`

- [ ] 1. 复现：抓 `ErrorLogStore.dump()` 与 OkHttp 日志，确认 429 来源与 Retry-After
- [ ] 2. `ScrapeRateLimiter` 共享化（提升至 `core:webdav` 或经 Hilt 共享）
- [ ] 3. `WebDavClient` 429 感知：Retry-After 解析 + 退避重试 1 次 + ErrorLog 埋点
- [ ] 4. `CacheDataSource` Range 合并/限流验证
- [ ] 5. 播放页限流文案与重试入口
- [ ] 6. 单测：限流、429 重试、二次抛错；MuMu 单首点播实测
- [ ] 7. 门禁全绿 + 装机
