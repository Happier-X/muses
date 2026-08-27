# 技术设计 — 刮削限流与退避

## 瓶颈
`ScrapeHttp` 为裸 OkHttp 透传，`TextMetaMatcher`（5 源）与 `CoverMatcher`（6 源）逐 provider 串行 `http.get`，无间隔。队列 10 首 ≈ 50+ 请求瞬发。

## 方案

### 1. ScrapeHttp 限流层
- 新增 `ScrapeRateLimiter`（core:scrape/http）：令牌桶，默认 4 rps，`acquire()` 在 `ScrapeHttp.execute` 入口阻塞（协程 `delay` 非阻塞线程）
- 配置注入：`ScrapeModule` 提供单例，测试可替换为 0 限流

### 2. 429 退避
- `ScrapeHttp`: 捕获 429 响应，解析 `Retry-After`（秒/HTTP-date），`delay(min(computed, 8s))` 后重试 1 次；二次 429 抛 `IOException("http 429")` 交上层归为 `NETWORK`
- `TextMetaMatcher`/`CoverMatcher` 已有 `NETWORK` 失败分支：429 最终归入该分支，触发 `FailureCopy` 网络文案与 NegativeCache 短期熔断（可选）

### 3. UI 可观察
- `WritebackResult`/`MatchResult` 的 429 最终态在 ScrapeScreen result 行展示为「限流，稍后重试」并提供单曲重试入口
- matching 态进度行在退避时显示「等待限流恢复…」

## 风险
- 限流过严导致刮削变慢：默认 4 rps 为经验值，DataStore 可配，首版固定即可
- 重试延长单首耗时：队列级退避可接受，单首最多 +8s

## 回滚
删除限流器与重试分支即回退为裸直通。
