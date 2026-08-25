# 执行计划：M3 刮削与元数据引擎（数据层）

> 前置：每批次开工前读对应 Web 基准文件全文；批次完成即提交一次（独立回滚点）。
> 全程禁止修改 `native/feature/*`、`native/app/src/main/kotlin/.../nativem1/` 下已有文件。

## S0 基建

- [x] `native/core/scrape` 新建 Android Library 模块，注册进 `settings.gradle.kts` 与 version catalog（如需新依赖：okhttp、kotlinx-serialization、datastore 已有则复用）
- [x] `core/model` 新增 `ScrapeModels.kt`（OnlineTextSource、TextMetaHit、WritebackStatus 等纯 Kotlin 模型）
- [x] `core/media` 新增 `TagWriter.kt` + 单测（临时文件构造 m4a/mp3 写读回验证）
- 验证：`cd native && ./gradlew :core:scrape:assembleDebug :core:media:test`

## S1 文本元数据

- [x] 读 `src/features/metadata/types.ts`、`util.ts`、`match.ts` 全文
- [x] 翻译 `text/TextMetaTypes.kt`、`text/TextMetaUtil.kt`（isWeakTitle/titlesRelated/buildKeyword/pickBestHit/needsOnlineTextMeta）
- [x] 翻译 `text/TextMetaConfidence.kt`（classifyTextMetaConfidence + cloud 来源再补约束 R4-2）
- [x] 实现 `NegativeCache.kt`（TTL 45min / 容量 256）
- [x] 五源 provider 逐个翻译：kw → tx → wy → kg → mg（对照各自 providers/*.ts 的端点、请求头、字段解析）
- [x] 组装 `TextMetaMatcher`（链序 kw→tx→wy→kg→mg）
- 验证：单测覆盖 needsOnlineTextMeta 各缺口分支、置信度分类、负缓存淘汰

## S2 封面匹配

- [x] 读 `src/features/cover/http.ts`、`match.ts`、`providers/*.ts` 全文
- [x] `CoverHttp.kt`（OkHttp；非 2xx 抛错不重试）
- [x] 六源封面 provider：wy / tx / kg / kw / mg / itunes
- [x] `CoverMatch.kt` 对齐 cover/match.ts
- 验证：provider 解析单测（固定 JSON 样本）

## S3 写回编排

- [x] 读 `src/features/scrape/writeback.ts` 全文
- [x] `RollbackJournal.kt`：DataStore JSON snapshot v1、上限 200、坏数据宽松回退
- [x] `WritebackOrchestrator.kt`：五步流程；本地并行 / WebDAV 串行；逐行 success/file-failed/failed
- [x] `UndoRestore.kt`：仅恢复库旧值
- [x] 接入 `ScrapeHistoryStore` 记录（依赖 S4 先行可先留接口，S4 完成后补线）
- 验证：journal 快照/恢复单测；orchestrator 用 fake WebDavClient/TagWriter 测三分支状态

## S4 队列与历史

- [x] 翻译 `queue.ts` → `ScrapeQueueStore.kt`（songId 幂等去重、懒清理已删歌曲、StateFlow 广播）
- [x] 翻译 `history.ts` → `ScrapeHistoryStore.kt`（滚动 200 条、歌名快照、journalId 关联）
- [x] 翻译 `suspicious.ts` → `SuspiciousDetector.kt`；`failure-copy.ts` 文案映射表
- 验证：幂等入队、懒清理、滚动清理单测

## S5 云元数据编辑搜索

- [x] 翻译 `editMeta/searchEditCloudMeta.ts` + `types.ts` → `editmeta/EditCloudMetaSearch.kt`
- 验证：候选搜索解析与排序单测

## 收尾

- [x] Hilt `di/ScrapeModule.kt` @Binds 装配（不接线 UI）
- [x] 全量验证：`cd native && ./gradlew lint testDebugUnitTest :app:assembleDebug`
- [x] 冲突自检：`git diff --stat main` 确认未触碰 feature:* / nativem1 已有文件
- [x] 更新 `.trellis/spec/android/`（新增 features-scrape-engine.md 特征规范）
- [x] 提交（每批次已有独立提交，收尾为 spec + 收尾项）

## 回滚点

每个 S 批次一个 commit；任一批次失败 `git revert` 该批次即可，不影响其他批次与 UI 任务分支。
