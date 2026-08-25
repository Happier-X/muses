# 技术设计：M3 刮削与元数据引擎（数据层）

## 1. 模块边界

```
native/core/scrape（新建，Android Library）
  ├─ 依赖：core:model、core:data（接口）、OkHttp、kotlinx-serialization、DataStore、coroutines
  ├─ 禁止依赖：Compose、Room 具体类、Media3
native/core/media（扩展，仅新增文件）
  └─ TagWriter.kt：jaudiotagger 写标签（与 TagReader 同库 net.jthink:jaudiotagger:3.0.1）
native/core/model（扩展，仅新增文件）
  └─ ScrapeModels.kt：刮削领域模型（纯 Kotlin）
```

调用方向（未来 UI 任务接线）：`feature:* → core:scrape 接口（Hilt 注入）→ providers/writeback`。

## 2. 包结构（core:scrape）

```
com.muses.player.core.scrape/
├── di/ScrapeModule.kt            # Hilt @Binds
├── model/                        # 领域模型（若 core:model 放置不便则收于此）
├── text/                         # S1 文本元数据
│   ├── TextMetaTypes.kt          # OnlineTextQuery / TextMetaHit / MatchResult（对齐 metadata/types.ts）
│   ├── TextMetaUtil.kt           # isWeakTitle / titlesRelated / buildKeyword / pickBestHit / needsOnlineTextMeta（util.ts）
│   ├── TextMetaConfidence.kt     # classifyTextMetaConfidence + 时长约束（R4-2 规则）
│   ├── NegativeCache.kt          # 歌曲级负缓存 TTL 45min、上限 256（match.ts 常量）
│   └── provider/KwProvider|TxProvider|WyProvider|KgProvider|MgProvider.kt
├── cover/                        # S2 封面匹配
│   ├── CoverHttp.kt              # OkHttp GET text/json（对齐 http.ts 语义：非 2xx 抛错，不重试）
│   ├── CoverTypes.kt / CoverMatch.kt
│   └── provider/Wy|Tx|Kg|Kw|Mg|ItunesCoverProvider.kt
├── writeback/                    # S3 写回编排
│   ├── WritebackOrchestrator.kt  # 快照→写文件→写库→逐行状态（对齐 writeback.ts 五步）
│   ├── RollbackJournal.kt        # DataStore 持久化，上限 200 条
│   └── UndoRestore.kt            # 撤销恢复库旧值
├── queue/                        # S4
│   ├── ScrapeQueueStore.kt       # 幂等入队 / 懒清理 / StateFlow 广播
│   ├── ScrapeHistoryStore.kt     # 滚动 200 条 / 歌名快照 / StateFlow 广播
│   └── SuspiciousDetector.kt     # suspicious.ts 规则
└── editmeta/                     # S5 云元数据编辑搜索
    └── EditCloudMetaSearch.kt    # searchEditCloudMeta.ts 翻译
```

## 3. 关键契约

### 3.1 文本元数据（S1）

```kotlin
interface TextMetaProvider { val id: OnlineTextSource; suspend fun search(query: OnlineTextQuery): TextMetaHit? }

class TextMetaMatcher(providers: List<TextMetaProvider>) {
    /** 默认链 kw→tx→wy→kg→mg；命中即返回并写负缓存反向记录 */
    suspend fun match(query: OnlineTextQuery): OnlineTextMatchResult
}
```

- `OnlineTextMatchResult` 密封类：`Ok(hit, confidence)` / `Fail(reason: no-match|network|not-needed)`
- 负缓存 key = `songId`，value 含 queryKey（title/artist/album JSON）+ 过期时间；TTL 45min、容量 256，超限淘汰最旧 —— 与 Web `match.ts` 一致
- HTTP 细节逐 provider 对齐 Web：kw 用 `search.kuwo.cn/r.s?client=kt...` 全参数串 + UA `Mozilla/5.0`；其余四源以各自 `.ts` 为准翻译，端点/参数/解析字段名不改

### 3.2 写回编排（S3）

```kotlin
class WritebackOrchestrator(
    private val tagWriter: TagWriter,          // core:media
    private val webDav: WebDavClient,          // core:webdav 已有 put/get
    private val journal: RollbackJournal,
    private val history: ScrapeHistoryStore,
) {
    suspend fun writeback(batch: List<WritebackItem>, journalId: String): List<WritebackResult>
}
```

流程对齐 `writeback.ts` 头注释五步：

1. 写前快照旧值（title/artist/album/coverUri/lyrics/lyricsFormat/lyricsSource/metaSources）进回滚 journal（DataStore，上限 200 条）
2. 写文件：**本地并行（协程并发）/ WebDAV 串行**（下载到 cache 临时文件 → TagWriter 写入 → `put` 上传）
3. 写库：upsertSong，来源按文件结果标记 embedded/scrape
4. 逐行返回 `success | file-failed | failed`
5. `undoRestore(journalId)`：仅恢复库旧值（文件不可逆），并追加历史

### 3.3 TagWriter（core:media 新增）

```kotlin
object TagWriter {
    data class WriteResult(...)  // 对齐 Web WriteMetadataResult 字段
    fun write(file: File, tags: TrackTags, cover: ByteArray? = null, lyrics: LyricsPayload? = null): WriteResult
}
```

用 jaudiotagger `AudioFileIO.write`；格式兼容性失败按 Web 语义归入 file-failed。

## 4. 存储介质映射

| Web（localStorage） | Native |
|---|---|
| `muses:scrape-rollback` | DataStore Preferences key `scrape_rollback_journal`（JSON snapshot v1，上限 200） |
| `muses:scrape-queue` | DataStore Preferences key `scrape_queue`（JSON snapshot v1） |
| `muses:scrape-history` | DataStore Preferences key `scrape_history`（JSON snapshot v1，滚动 200） |
| `window` 事件广播 | `MutableStateFlow` / `SharedFlow` 暴露给未来 UI |

schema 全部带 `version` 字段 + 宽松解析（坏数据回退空表），与 Web 的 isRecord 校验风格一致。

## 5. 并发与错误策略

- Provider 网络层：OkHttp 单例、连接/读超时与 CapacitorHttp 缺省行为对齐即可（不额外加重试——Web 无重试）
- 批量写回：本地源 `coroutineScope { map { async } }`；WebDAV 源单协程顺序执行；任一文件失败不影响其余条目（逐行状态）
- 所有 suspend 入口切 `Dispatchers.IO`

## 6. 测试策略

- 纯 JVM 单测：matcher 置信度、needsOnlineTextMeta、pickBestHit、负缓存淘汰、journal 快照/恢复、队列幂等、suspicious 规则（MockWebServer 可选覆盖 provider 解析）
- 不做实网测试（Web 层同样规避实网污染）

## 7. 权衡与风险

- **provider 端点为第三方非官方接口**，可能随时间失效：与 Web 行为一致即可，不做加固（对齐 D2）
- **jaudiotagger 写标签对部分格式有损**：沿用 Web 层已有的 file-failed 分类语义，不引入新库
- **DataStore 存 JSON snapshot** 而非 Room 表：数据量小（≤200 条）、结构与 Web 对齐优先，避免过度设计
