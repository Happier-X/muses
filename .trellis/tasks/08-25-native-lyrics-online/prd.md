# 歌词在线搜索数据层（父任务 08-24-native-compose-rewrite 的子任务）

> 背景：M2 只交付了本地歌词解析与 AMLL WebView 渲染端；Web 层 `src/features/lyrics`（~2.5k 行 TS）的**在线歌词搜索链路**尚无原生对应物。本任务与其 Compose UI 会话并行推进：纯 Kotlin 数据层，零文件冲突。同时顺带修复 M3 遗留的 WebDAV username 持久化缺口。

## Goal

以 **Web 层 `src/features/lyrics` 源码为唯一规格书**，逐模块翻译为原生 Kotlin：

- 平台五源歌词 provider（kw/tx/wy/kg/mg），含网易云 eapi 加密（wyCrypto：MD5 + AES-128-ECB）、QRC 解密
- LRCLIB 公开 API provider（仅 syncedLyrics）
- AMLL TTML 聚合库查询（CDN jsonl 索引下载解析 + trigram 搜索索引 + findBestMatch 评分 + 双层负缓存）
- 匹配编排 `matchOnlineLyrics`（amll 优先 → 平台五源 → LRCLIB，任一命中即停）
- ~~翻译合并算法~~ **决策变更（2026-08-25）**：mergeTranslation 依赖 AMLL LyricLine 结构（在 AMLL WebView JS 侧解析产生），Kotlin 侧复制需连带移植 parseLrc/Yrc/TTML 解析器；该合并应留在 WebView JS 层用同一 npm 库执行，本任务不移植
- 接入 M3 预留的 `core.scrape.editmeta.LyricsSearchPort` 接口缝

## 核心方法论

1. **Web=规格书**：算法、常量（超时 20s/12s、负缓存 TTL 5min、歌曲缓存 256、时长容差 ±5s、评分权重）逐一翻译并注释来源 .ts
2. **存储介质替换**：内存 Map 缓存语义不变；无持久化需求不引入 DataStore
3. **分层铁律**：新代码全部落在新建 `core:lyrics` 模块（仅依赖 core:model + OkHttp + coroutines）；对 core:scrape 的反向依赖只允许「editmeta 的 LyricsSearchPort 由适配器实现」这一条
4. **与 UI 任务隔离**：不修改 `feature:*`、`app/**` 已有文件；L4 的 Room 迁移沿用既有模式（v4→v5 向前追加列）

## 范围分解（按依赖顺序分批）

| 批次 | 内容 | Web 基准 |
|---|---|---|
| L0 基建 | 新 Gradle 模块 `core:lyrics`；core:model 歌词领域模型（OnlineLyricsQuery/Format/Source/ProviderHit/MatchResult、Amll 三件套） | `types.ts`(43)、`providers/types.ts`(58) |
| L1 平台五源 | wyCrypto（RFC1321 MD5 + AES-ECB eapi 参数加密）→ wy → kg → kw → mg → tx(QRC) → platform 默认链 + util.pickBest | `wyCrypto.ts`(221)、`providers/{wy,kg,kw,mg,tx,qrc,util}.ts`(~800) |
| L2 LRCLIB + AMLL | lrclib provider（exact/get 检索）；AMLL TTML DB（jsonl 索引解析、exactTitles/titleTrigrams 搜索索引、score.ts 全套评分权重与 findBestMatch、TTML 下载、song/negative 双缓存） | `lrclib.ts`(149)、`amllTtmlDb.ts`(374)、`score.ts`(192)、`normalize.ts`(35) |
| L3 编排+接线 | matchOnlineLyrics 主流程（amll 优先/fallback 串行/任一命中即停/network-parse 区分）；mergeTranslation 双语合并；`LyricsSearchPort` 适配器 + Hilt 装配（core:scrape 增加 core:lyrics 实现依赖） | `match.ts`(102)、`mergeTranslation.ts`(442)、`display.ts`(13) |
| L4 username 补缺 | Source 领域模型/实体加 `username` 列（Room v4→v5，向前追加）；SourcesViewModel 表单写入处最小接线 | — |

## Acceptance Criteria

- [ ] `native/core/lyrics` 存在且仅依赖 core:model / OkHttp / kotlinx-serialization / coroutines，无 Compose/Room/Media3
- [ ] wyCrypto：MD5 实现对 RFC 1321 测试向量全过；eapi 加密结果可被 wy provider 往返验证（MockWebServer 回环）
- [ ] 五源链顺序 kw→tx→wy→kg→mg、fallback 末位 LRCLIB 与 Web 一致；AMLL 永远最优先
- [ ] AMLL：jsonl 索引行 metadata `[key, values[]]` 结构宽松解析；findBestMatch 评分权重/时长容差 ±5s/MIN_ACCEPT_SCORE 对齐 score.ts；负缓存 TTL 5min、歌曲缓存上限 256、索引/TTML 超时 20s/12s
- [ ] mergeTranslation 对齐 Web 合并规则（时间轴对齐、逐字→逐行降级等，以 .ts 为准）
- [ ] 单测覆盖：MD5 测试向量、各 provider JSON/QRC 解析（MockWebServer 固定样本）、findBestMatch 分级、mergeTranslation 关键分支、matchOnlineLyrics 四种结局
- [ ] `cd native && ./gradlew :app:assembleDebug :core:lyrics:lintDebug testDebugUnitTest` 全绿
- [ ] `git diff --stat` 不含 `feature/*` 与 `nativem1` 下已有文件的修改（新增文件除外）

## Out of Scope

- 任何 Compose 页面（歌词设置页、编辑页 UI 归 Salt UI 任务或后续 M3 UI 子任务）
- 在线歌词写入库的触发时机接线（controller 层职责，归后续）
- 歌词渲染端（M2 LyricsParser/AMLL WebView 已交付，不动）

## 关键决策

- **D1 独立模块**（2026-08-25）：歌词域自建 `core:lyrics`，不塞进 core:scrape；依赖方向 scrape→lyrics 仅用于 Port 适配绑定
- **D2 Web=规格书**（2026-08-25）：加密算法、常量、评分权重逐值翻译
- **D3 密码学用平台原语**（2026-08-25）：MD5 用 `java.security.MessageDigest`、AES-ECB 用 `javax.crypto`，不自研（Web 因浏览器环境缺原语才手写 MD5）
