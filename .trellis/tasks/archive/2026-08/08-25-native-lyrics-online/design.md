# 技术设计：歌词在线搜索数据层

## 1. 模块边界

```
native/core/lyrics（新建，Android Library）
  ├─ 依赖：core:model、OkHttp、kotlinx-serialization、coroutines
  ├─ 禁止依赖：Compose、Room、Media3
native/core/scrape（扩展）
  └─ build.gradle 增加 implementation(project(":core:lyrics"))；editmeta 包新增 LyricsSearchPortAdapter
native/core/model（扩展，仅新增文件）
  └─ lyrics/LyricsModels.kt：歌词领域模型 + AMLL 三件套（纯 Kotlin）
```

依赖方向：`scrape(编排) → lyrics(实现) → model`；无环。

## 2. 包结构（core:lyrics）

```
com.muses.player.core.lyrics/
├── crypto/WyCrypto.kt             # eapi 参数加密：MD5(MessageDigest) + AES-128-ECB(Cipher)；secret 'e82ckenh8dichen8'
├── model/(若 core:model 放置不便收于此)
├── http/LyricsHttp.kt            # 复用 scrape 的模式？否——独立实现避免反向依赖；OkHttp GET text/json + 超时参数化
├── provider/
│   ├── LyricsProviderUtil.kt      # pickBest 等（providers/util.ts）
│   ├── WyLyricsProvider.kt        # eapi song lyric + tlyric 译文（wyCrypto 加密 POST）
│   ├── KgLyricsProvider.kt / KwLyricsProvider.kt / MgLyricsProvider.kt
│   ├── TxLyricsProvider.kt        # 含 QRC 解密（qrc.ts：三重 DES? 以 .ts 为准翻译）
│   ├── QrcDecoder.kt              # qrc.ts 解密逻辑独立成对象
│   └── PlatformChain.kt           # 默认链 kw→tx→wy→kg→mg
├── lrclib/LrclibProvider.kt       # exact + 检索两段；仅 syncedLyrics 且 /\[\d{1,2}:\d{2}/ 校验；UA 'Muses/0.1.2 (...)'
├── amll/
│   ├── AmllIndex.kt               # jsonl 索引下载(20s 超时)+宽松解析(metadata [key,values[]])+exactTitles/titleTrigrams 索引
│   ├── AmllScore.kt               # score.ts 全套：SCORE_WEIGHTS/TITLE_CONTAINS 阈值/DURATION_TOLERANCE_SEC=5/MIN_ACCEPT_SCORE/classifyMatch/findBestMatch
│   └── AmllTtmlDbClient.kt        # matchAmllTtmlLyrics 主流程；TTML 下载 12s；song 缓存 256、负缓存 TTL 5min
├── merge/MergeTranslation.kt      # mergeTranslation.ts 双语合并算法
├── LyricsMatcher.kt               # matchOnlineLyrics 编排：amll 优先 → fallback 串行 → 任一命中即停；network/parse 区分
└── di/LyricsModule.kt             # Hilt @Provides；LyricsSearchPortAdapter 绑定（在 core:scrape 的 editmeta 包定义适配器）
```

## 3. 关键契约

### 3.1 领域模型（providers/types.ts 对齐）

```kotlin
enum class OnlineLyricsFormat(val wire: String) { TTML("ttml"), LRC("lrc"), YRC("yrc"), QRC("qrc") }
enum class OnlineLyricsSource(val wire: String) { AMLL, KW, TX, WY, KG, MG, LRCLIB }

data class OnlineLyricsQuery(songId, title, artist?, album?, durationSec?)

interface LyricsProvider {
    val id: OnlineLyricsSource          // 不含 AMLL
    suspend fun searchLyrics(query: OnlineLyricsQuery): ProviderHit?
}
data class ProviderHit(text, format, translationText?)   // translationText = timed LRC 译文

sealed interface OnlineLyricsMatchResult {
    data class Ok(text, format, source, translationText?, confidence: MatchConfidence? = null)
    data class Fail(reason: NO_MATCH | NETWORK | PARSE)
}
```

### 3.2 wyCrypto（wyCrypto.ts 对齐）

- eapi 请求体：`{"header":..., "url":"...", "params":...}` JSON → `AES-ECB-PKCS5(secret)` → hex 大写 → 表单 `encoderType=1&eapiParams=<hex>`（以 wy.ts 实际拼装为准）
- MD5 直接用 `MessageDigest.getInstance("MD5")`（D3 决策，不自研 RFC1321）；测试向量 `"" → d41d8cd98f00b204e9800998ecf8427e` 等校验
- 响应解密同 key AES-ECB 解 hex

### 3.3 AMLL（amllTtmlDb.ts 对齐）

- 索引 URL：`cdn.jsdelivr.net/gh/amll-dev/amll-ttml-db@main/metadata/raw-lyrics-index.jsonl`；TTML base `.../raw-lyrics/`
- jsonl 行结构 `{metadata: [[key, values[]], ...], rawLyricFile}`：musicName/artists/album/duration 宽松提取
- 搜索索引：exactTitles（normalize 后精确表）+ titleTrigrams（三元组倒排）；懒加载 + 单飞（indexPromise 语义 → Mutex + 缓存结果）
- findBestMatch：scoreEntry 打分 → MIN_ACCEPT_SCORE 过滤 → 最高分且 classifyMatch≥high 才返回 confidence=HIGH
- 双缓存：`ttmlBySongId`（queryKey 校验，容量 256）、`negativeBySongId`（TTL 5min）

### 3.4 matchOnlineLyrics（match.ts 对齐）

空 title/songId 早退 no-match → amll（Ok 即返 ttml+confidence）→ fallback 串行任一命中即停（带 translationText）→ sawNetwork/sawParse 区分三种 Fail。注意 Web 边界：`sawParse && fallbackProviders.length === 0` 才归 parse。

### 3.5 Port 适配接线

```kotlin
// core:scrape/editmeta/LyricsPortAdapter.kt（新文件）
class AmllFirstLyricsPort(private val matcher: LyricsMatcher, private val source: String) : LyricsSearchPort { ... }
```

editMeta 歌词维度的 ports 注入：amll 端口（id="amll"）+ 平台五源端口（id=各 wire）+ lrclib 端口——由 ScrapeModule/LyricsModule 协作提供。

## 4. L4 username 补缺

- `Source` 领域模型加 `username: String? = null`
- `SourceEntity` 加列、`MIGRATION_4_5`（ALTER TABLE sources ADD COLUMN username TEXT DEFAULT NULL）、DB version=5
- Mapper 双向映射；SourcesViewModel 表单保存处补一行写入（最小改动，若与 UI 会话冲突则只交付数据层并注明）

## 5. 并发与缓存

- AMLL 索引单飞加载：`Mutex` + `loaded: AmllSearchIndex?` + 失败不缓存（下次重试），对齐 indexPromise 语义
- 所有网络入口 `withContext(Dispatchers.IO)`
- 超时用 OkHttp per-call client（索引 20s、TTML/普通请求按 .ts 常量）

## 6. 测试策略

- MD5 标准测试向量 + eapi 加密往返（自加密自解密 + 已知向量如可得）
- 五源 provider：MockWebServer 固定 JSON/QRC 样本解析（复用 host 重写拦截器模式）
- AMLL：本地 jsonl 样本行解析、findBestMatch 各分级、缓存命中/过期
- mergeTranslation：时间轴对齐/降级分支关键样例
- matchOnlineLyrics：amll 命中/fallback 命中/全 miss/network/parse 四结局
