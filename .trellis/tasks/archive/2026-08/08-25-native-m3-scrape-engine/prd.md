# M3 刮削与元数据引擎（数据层）（父任务 08-24-native-compose-rewrite 的子任务）

> 背景：M3 里程碑定义为「元数据与长尾：批量刮削 + 写回 WebDAV、云元数据编辑、封面匹配」。当前另一会话正在执行 `08-25-native-salt-ui`（Compose UI 一比一复刻，P0~P5 批次），为提高迁移效率，本任务与其**并行**推进：只做刮削引擎的纯 Kotlin 数据/领域层，不碰任何 Compose UI 文件，保证零合并冲突。

## Goal

以 **Web 层 `src/features/scrape`、`src/features/metadata`、`src/features/cover`、`src/features/editMeta` 源码为规格书**（合计 ~3.0k 行 TS），逐模块翻译为原生 Kotlin 数据层，交付可被后续 UI 任务直接接线的刮削引擎：

- 文本元数据在线补缺（kw→tx→wy→kg→mg 五源链 + 匹配置信度 + 负缓存）
- 封面在线匹配（wy/tx/kg/kw/mg/itunes 六源）
- 写回编排：本地内嵌标签写入 + WebDAV 远端写回（下载→改标签→上传）+ 回滚 journal
- 待刮削队列、刮削历史、可疑文件检测
- 云元数据编辑搜索（editMeta 数据层）

## 核心方法论

1. **Web 源码 = 唯一规格书**：逐文件翻译算法与常量（匹配阈值、TTL、上限数量等一律对齐 Web 值），不自由发挥
2. **存储介质替换而非逻辑改动**：Web 的 localStorage key → DataStore Preferences / Room；事件广播 → StateFlow/SharedFlow
3. **分层铁律**（`.trellis/spec/android/index.md`）：全部落在 `core:*` 新模块，禁止依赖 Compose/Room 实现细节泄漏到接口层
4. **与 UI 任务隔离**：不修改 `feature:*`、`app/` 下任何现有文件；UI 接线留给后续任务

## 范围分解（按依赖顺序分批）

| 批次 | 内容 | Web 基准 |
|---|---|---|
| S0 基建 | 新 Gradle 模块 `core:scrape`；`core:media` 增加 TagWriter（jaudiotagger 写标签）；core:model 刮削领域模型 | — |
| S1 文本元数据 | 五源 provider + 匹配器 + 置信度分类 + 弱 title 判定 + 负缓存 | `metadata/providers/*`(5×~60行)、`match.ts`(94)、`util.ts`(213) |
| S2 封面匹配 | 六源封面 provider + cover http + 封面 match | `cover/providers/*`(6×~90行)、`http.ts`(91)、`match.ts`(86) |
| S3 写回编排 | 写前快照→写文件（本地并行/WebDAV 串行）→写库→逐行状态；回滚 journal（DataStore）；撤销恢复 | `scrape/writeback.ts`(425) |
| S4 队列与历史 | 待刮削队列（幂等入队、懒清理）、刮削历史（滚动 200 条、失败原因快照）、可疑检测 | `queue.ts`(132)、`history.ts`(140)、`suspicious.ts`(79)、`failure-copy.ts`(75) |
| S5 云元数据编辑 | 编辑云元数据的候选搜索数据层 | `editMeta/searchEditCloudMeta.ts`(422)、`types.ts`(61) |

## Acceptance Criteria

- [ ] `native/core/scrape` 模块存在且仅依赖 core:model / core:data 接口 / OkHttp / kotlinx.serialization / DataStore，无任何 Compose 依赖
- [ ] 文本五源链顺序 kw→tx→wy→kg→mg 与 Web 一致；置信度 high/low 分类规则逐条对齐 `classifyTextMetaConfidence`
- [ ] 写回编排满足：写前快照进回滚 journal（上限 200 条）、本地并行/WebDAV 串行、库更新来源标记 embedded/scrape、逐行返回 success/file-failed/failed
- [ ] 队列入队按 songId 幂等去重；历史滚动上限 200 条且带歌名快照
- [ ] 单元测试覆盖：matcher 置信度、needsOnlineTextMeta 缺口判定、writeback journal 快照/恢复、队列幂等（JUnit + 协程 test）
- [ ] `cd native && ./gradlew lint testDebugUnitTest :app:assembleDebug` 全绿
- [ ] `git diff --stat` 不含 `feature/*` 与 `app/src/main/kotlin/.../nativem1/` 下已有文件的修改（新增文件除外）

## Out of Scope

- 任何 Compose 页面/组件（ScrapePage、设置页 UI 等，归 Salt UI 任务 P5 或后续 M3 UI 子任务）
- 刮削触发时机接线（入库后自动补缺的后台调度，留接口给后续任务）
- 歌词在线搜索（M2 已有 LyricsParser/AMLL 链路，本任务不动）

## 关键决策

- **D1 并行隔离**（2026-08-25）：只新增 `core:scrape` 及 `core:media`/`core:model` 内的新文件，不改 UI 会话正在动的文件
- **D2 Web=规格书**（2026-08-25）：算法、常量、阈值逐一翻译；provider HTTP 端点与参数原样保留
- **D3 存储替换**（2026-08-25）：localStorage → DataStore Preferences（JSON 序列化 snapshot），保持版本化 schema 以便迁移
