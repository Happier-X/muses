# 执行清单 — 刮削限流与退避

> 验证：`./gradlew :core:scrape:testDebugUnitTest :app:assembleMusesDebug`

- [ ] 1. `ScrapeRateLimiter`（令牌桶 4 rps，协程 delay 限流）
- [ ] 2. `ScrapeHttp` 429 感知：Retry-After 解析 + 指数退避重试 1 次
- [ ] 3. 匹配器 429→NETWORK 归类验证，NegativeCache 行为确认
- [ ] 4. ScrapeScreen result/matching 退避提示与单曲重试入口
- [ ] 5. 单测：限流器节流、429 重试、二次 429 抛错；MuMu 20 首队列实测
- [ ] 6. 门禁全绿 + 装机
