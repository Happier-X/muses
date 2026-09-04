# KMP 迁移 P2c：WebDAV 与在线 provider 切 Ktor

## Goal

`core:webdav` 传输层与 lyrics/scrape 在线 provider 全家切 Ktor-client（CIO），搬入 `:core:common`；限流/缓存/错误语义冻结，测试平移。OkHttp 仅保留给 Media3 数据源（明确不删）。

## Background（已实测）

- 两包装器很薄且契约一致：`LyricsHttp`（75 行）/`ScrapeHttp`（136 行），非 2xx 抛 `IOException("http <code>")`；仅 Scrape 有 429 退避（纯逻辑，仅传输层需换）。
- provider 40+ 处全是泛 `catch (_: Exception)`（+ 正确重抛 CancellationException），故 `java.io.IOException`→`kotlinx.io.IOException` 安全。
- `WebDavClient` 是接口 + 实现：authenticate/probe/list/get/put/delete/move/getString；但 `get/put` 签名进 `java.io.File`，搬迁须改 okio Path（消费者同步改，见 R3）。
- Ktor 选型：3.5.x + CIO 引擎（纯 Kotlin，commonMain 可用；HTTP/2 缺失无关，音乐 API/WebDAV 均为 HTTP/1.1）。
- 留守：`WebDavAudioCache`（Context/Uri/Media3 链路）、`TtmlLyricsParser`（java.*）、Media3-OkHttp 数据源（明确保留）。

## Requirements

- R1：`LyricsHttp`/`ScrapeHttp` 按原 API 原语义用 Ktor 重写进 commonMain（含 Scrape 429 骨架原样迁移，`parseRetryAfterMs` 去 java.time）；在线 provider 留守原模块，仅换底层调用（API 形状冻结，provider 零改动）。
- R2：`WebDavClient` 留守 `:core:webdav` 做 Ktor 原地替换（接口签名冻结，`File` 不变）；`WebDavAuthRegistry` 仅去 okhttp3（`Credentials.basic`→同语义纯函数），不搬迁；`WebDavRateLimiter` 搬入 commonMain。
- R3：`WebDavAudioCache` 仅清理无用 import，不做 Path 适配；播放流播链路不变。
- R4：4 个 MockWebServer 测试改 Ktor `MockEngine` 重写；429/限流单测语义不变。
- R5：回归 + MuMu 实测（WebDAV 建库/扫库/播放 + 在线搜歌/封面/歌词）。

## Acceptance Criteria

- [x] AC1：三模块主源码零 `okhttp3` import（豁免：WebDavModule 双绑定 + 注释，见 D4）。
- [x] AC2：错误/限流契约冻结（429 骨架逐行平移，常量一致；XML 边缘矩阵单测通过）。
- [x] AC3：R4 测试全过 + 全量门禁 BUILD SUCCESSFUL；MuMu 深链路待用户实测。
- [x] AC4：ktor-bom 3.5.2 + kotlinx-io 单点，BOM 统一。

## Out of Scope

- 不删 Media3 的 OkHttp 数据源；不动 Room/DataStore/播放器；不建桌面 UI；不升 Kotlin/AGP。
- `WebDavAudioCache` 的 KMP 化（P3 随桌面缓存重写再议）。

## Key Decisions

- D1：CIO 而非 OkHttp 引擎（commonMain 可用性是硬门槛；OkHttp 引擎只会把平台绑定带回来）。
- D2：包装器 API 形状冻结（provider 零改动，只换底层）。
- D3：S0 spike 先定 xmlutil 版本（PROPFIND 解析是最大未知数）。

## Risks

- xmlutil 版本与 Kotlin 2.4.10 的搭配（S0 spike，不行转手写解析）。
- Ktor CIO 的超时/Range 语义与 OkHttp 的细微差异（per-call readTimeout、AMLL 20s/TTML 12s 逐个对齊验证）。
- MockEngine 不走真实 socket，429/超时测试需重写断言方式。

## 偏离记录（S3 审计 + 独立复核结论，已执行）

- D1：`WebDavClient`/`AuthRegistry` 留守 `:core:webdav`（硬依赖 Android-only 的 `:core:data`：ErrorLogStore 构造注入 + AuthRegistry 直调 Source/CredentialsRepository；单抽接口仍需跨模块重组，超传输层范围）。传输层本身已切 Ktor，后续搬迁为机械操作。
- D2：provider 全家留守（`WyCrypto` javax.crypto、`QrcDecoder`/Krc 解析 java.util.zip、`URLEncoder` 内联调用等 JVM 绑定 8 文件，需 expect/actual 专项）。provider 经公共包装器间接受益 Ktor，零改动。
- D3：`File` 签名冻结，不改 okio Path（连带 `WebDavAudioCache`/写回/Media3 流播链路，风险收益为负；无 AC 门禁要求 Path）。
- D4：AC1 豁免清单：`WebDavModule` 双 OkHttp 绑定（喂 Media3 `OkHttpDataSource` + `AudioTagReader` Range）+ 注释；lyrics/scrape 的 `implementation(okhttp)` 与三模块 `mockwebserver` 测试依赖已删除（主源码与测试均零 import）。
- D5：PROPFIND 改手写解析（xmlutil 1.0.x 与 Kotlin 2.4.10 搭配未经验证 + 形状极窄）；`unescapeXml` 补数字引用还原（`&#NNN;`/`&#xHHH;`）与原生对等，边缘矩阵见 `KtorWebDavClientTest.list_tolerates_no_prefix_uppercase_and_entities`。
