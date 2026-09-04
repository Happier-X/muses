# P2c 技术设计

## 1. 依赖（BOM 统一）

```toml
[versions] ktor = "3.5.x（S0 定精确小版本）"
[libraries] ktor-bom = { module = "io.ktor:ktor-bom", version.ref = "ktor" }
  ktor-client-core / ktor-client-cio / ktor-client-content-negotiation /
  ktor-client-logging / ktor-client-mock（test）/ kotlinx-io / xmlutil（S0 定）
```

- commonMain：client-core + cio + logging（Headers/超时验证用）；JSON 沿用现有 kotlinx-serialization（provider 侧 `Json.parseToJsonElement` 不动，不引入 ContentNegotiation 做强类型映射）。
- jvmMain/androidMain：CIO 同引擎（双端一致，无需 expect）。
- Media3 的 `media3-datasource-okhttp` 保留，注释标明 P2c 豁免。

## 2. 传输替换映射（OkHttp → Ktor CIO）

| OkHttp 现状 | Ktor 写法 | 冻结点 |
|---|---|---|
| `OkHttpClient{connectTimeout,readTimeout}` | `HttpClient(CIO){ engine{requestTimeout?}; install(HttpTimeout){request/connect/socket} }` | AMLL 20s/TTML 12s per-call 超时逐个对齐 |
| `newCall.execute().use{}` | `client.get/post { headers{}; setBody() }.bodyAsText()` | 非 2xx（`!status.isSuccess()`）抛 `kotlinx.io.IOException("http ${code}")` |
| `response.body.bytes()` | `bodyAsChannel().toByteArray()` / `body<ByteArray>()` | 空体回退空数组语义保留 |
| `response.header("Retry-After")` | `response.headers["Retry-After"]` | 429 骨架（acquire→执行→解析→delay(min,8s)→重试1次→再429抛）逐行平移 |
| `Authenticator/Credentials.basic`（AuthRegistry） | 纯函数拼 `Basic base64(user:pass)` header（kotlinx.io Base64 / expect） | 每请求 `effectiveAuthHeader(url)` 语义不变 |
| PROPFIND + XmlPullParser | Ktor 自定义 METHOD + xmlutil 解析 multistatus | S0 spike 定 xmlutil；href/404/鉴权失败语义冻结 |

## 3. File→Path API 迁移（WebDavClient）

- `get(url, dest: File): File` → `get(url, dest: Path): Path`；`put` 同理；内部流拷贝走 okio `Source.buffer().readAll(sink)`。
- `WebDavAudioCache`（留守）调用点同步改；`File` 仅保留在 cache/Media3 侧。

## 4. 验证矩阵

| 门禁 | 期望 |
|---|---|
| AC1 grep | webdav/lyrics/scrape 主源码零 okhttp3 |
| 429 单测（MockEngine 短路 429→200） | 重试 1 次成功；二次 429 抛 `http 429` |
| 超时单测 | per-call 超时触发（MockEngine delay 模拟） |
| 回归 | `assembleMusesDebug + testDebugUnitTest` |
| 实测 | MuMu：WebDAV 建库/扫库/流播 + 五源搜歌/封面/在线歌词 |

## 5. 回滚

- 单提交；`git revert`。网络层回滚红线：无 schema/存储变更，revert 安全。
