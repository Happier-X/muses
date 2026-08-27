# WebDAV 播放 429 服务端自检清单（Cloudflare + OpenList）

> 针对 `openlist.happierx.xyz` 类自建 WebDAV 被 Cloudflare / OpenList 持续 429 的复现归因与自助放行指引。

## 一、如何用本机日志区分限流源

1. **抓 `ErrorLogStore.dump()`**：设置页 → 反馈 → 复制全文
   - 本次修复后 `WebDavClient` 对每次 429 均埋点：`WARN WebDavClient http 429 url=...` + `Retry-After`
   - `Playback` 对流播 429 埋点：`WARN Playback 触发限流 429 url=...`
2. **抓 OkHttp 日志（adb）**：
   ```bash
   adb logcat | grep -i "retry-after\|429\|cf-ray"
   ```
3. **判定**：
   - 响应头含 `cf-ray` / `cf-cache-status` / `cf-mitigated: rate-limit` → **Cloudflare Rate Limiting / WAF** 拦截
   - 响应头含 `x-openlist-*` 或无 `cf-ray` 但仍 429 → **OpenList/源站**阈值（OpenList 默认无全局限流，多为反代侧如 Nginx/Cloudflare）
   - 请求频率：单首点播触发 ≥2-4 次 Range 探测（ExoPlayer 对未知时长 mp3/flac），与刮削并发叠加即达阈值

## 二、Cloudflare 对 `/dav/*` 放行清单（用户自建站自助）

> 路径匹配以下均以 **通配符** `/dav/*`（与 `/dav*` 两条）为准，按需替换为实际前缀如 `/dav`、`/webdav`。

### 1) Rate Limiting → 跳过

- **Cloudflare Dashboard** → 你的域名 → **Security** → **Rate limiting rules** → **Create rule**
  - 字段：`URI Path` 包含 `/dav/`
  - 动作：**Bypass**（或 **Skip** rate limiting），或将阈值调至 `Requests > 20 / 10s` 以上
  - 已有全站 4-5 rps 规则者：新增一条 **更高优先级**的 Bypass 规则，URL 包含 `/dav`，强制置顶
  - 校验：触发后响应头不应再出现 `cf-mitigated: rate-limit`

### 2) WAF → 跳过

- **Security** → **WAF** → **Custom rules** / **Managed rules**
  - 新增：`URI Path contains /dav/` → **Skip**：勾选 `All remaining custom rules` + `All rate limiting rules` + `All managed rules`
  - Managed Rules 中若误拦 `PROPFIND` / `MOVE`：确认为假阳后跳过 `Cloudflare Managed Ruleset`

### 3) Caching → 绕过

- **Rules** → **Cache Rules** → **Create rule**
  - URI Path contains `/dav/` → **Bypass cache** + **Disable performance features**
  - 避免 WebDAV 的 `PROPFIND` / 带鉴权 GET 被边缘缓存污染

### 4) Configuration Rules → 可选

- `Automatic HTTPS Rewrites` / `Polish` 对 `/dav/*` 保持关闭
- **Network** → `HTTP/2` 保持开启；`gRPC` / `WebSockets` 若无用可关

## 三、OpenList / Nginx 侧建议

- OpenList 无内置全局限流；若通过 Nginx 反代，检查 `limit_req_zone` / `limit_req` 是否对 `/dav` 生效，必要时对该 location `limit_req off;`
- 保持 4 rps 客户端限流（本任务客户端已做全局共享 4 rps + Retry-After 退避），服务端无需再降阈值

## 四、客户端已落地修复（本任务）

- `WebDavRateLimiter` 共享单例（播放 + 刮削 4 rps，`X-Muses-Rate-Limited` 标记避免双限）
- `WebDavClient` 每次请求前 `acquire()`，429 时解析 `Retry-After`（秒/HTTP-date）→ `delay(min(...,8s))` 重试 1 次，二次 429 抛 `IOException("http 429")` 并 `ErrorLogStore.log(WARN, "WebDavClient", ...)`
- `OkHttp` 层对 ExoPlayer Range 探测同样限流（`acquireBlocking`），`CacheDataSource` 命中本地不再发网
- 播放页 `playbackError` 消费文案「触发限流，稍后重试」+ 重试按钮，设置页 `dump()` 可直接看到限流埋点

> 验证：单首 WebDAV 歌曲点播不再稳定 429；429 后可见提示，重试可自愈；本地播放无影响；`testDebugUnitTest` 全绿。
