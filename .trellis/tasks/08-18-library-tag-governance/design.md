# 曲库 tag 治理（parent）——技术设计

## 1. 架构边界

本任务在主应用内新增「曲库治理」能力面，不引入新依赖、不动原生壳（Android/WebDAV 插件桥已有读/写能力）。模块按数据流纵向切片：

```
[入队入口]──┐  (child 2)
            ├→ [待刮削队列] ─→ [刮削流程页] ─→ [写回] 
[筛选入队]──┘     (child 1)       (child 3)       (child 3)
                                              ↑ 复用现有 writeMetadata 桥
[匹配质量]────────────────────────────────────┘  (child 4，评分/门槛)
```

依赖顺序：child1 → child2 → child3；child4 与 2/3 并行，但评分函数被 child3 消费，需先合入。

## 2. 数据模型（child 1）

### 2.1 字段来源追踪

`SongItem` 新增（`src/features/library/types.ts`）：

```ts
export type MetaFieldKey = 'title' | 'artist' | 'album' | 'cover'
export type FieldSource = 'embedded' | 'cloud' | 'manual'

// SongItem 上：
metaSources?: Partial<Record<MetaFieldKey, FieldSource>>
```

- `embedded`：值来自音频文件内置 tag（扫描/懒扫写入）。
- `cloud`：值来自在线补缺/刮削且**尚未写回文件**（或写回失败）。
- `manual`：值与 `userEditedFields` 同语义（用户手改），派生自 `userEditedFields`，不重复存储。
- 歌词沿用既有 `lyricsSource`，不新增。
- **不新增顶层「刮削来源」粒度**，避免过度建模；来源 = 最近一次写入方。

### 2.2 合并规则扩展（`storage.ts`）

- `upsertSong` 自动路径（扫描/懒扫/在线补缺）：
  - 扫描/懒扫（读文件内置）写入的字段 → 标记 `embedded`。
  - 在线补缺（prefetch / controller match\*）写入的字段 → 标记 `cloud`；受 `applyTagsRespectingUserEdits` 保护逻辑不变（manual 优先）。
  - **来源不写空**：旧字段保留原来源；仅写值且来源非空才更新对应 `metaSources[key]`。
- 手动编辑（`updateSongUserEdit`）字段 → 顺带从 `metaSources` 移除或标记 `manual`（与 `userEditedFields` 同步，防双写不一致；实做：移除该 key，UI 通过 userEditedFields 判定 manual）。

### 2.3 存量兼容（Q5，`metadataVersion` 4）

- `CURRENT_METADATA_VERSION` 3 → 4。
- 旧 SongItem 无 `metaSources` → 读取时默认视同全部 `embedded`（保守默认：内置权威），不写迁移脚本；懒扫时 `shouldRefreshMetadata` 因 version 变化触发重读，自然升级补齐来源。
- `isSongItem` 校验增加可选字段校验（缺省合法）。

### 2.4 待刮削队列

独立 localStorage key（与 songs 分离，避免污染曲库存量）：

```ts
const SCRAPE_QUEUE_KEY = 'muses:scrape-queue'
// 结构：{ version: 1, items: [{ songId, addedAt }] }
```

- 入队幂等：按 `songId` 去重（已存在则只更新时间）。
- 队列歌曲被从曲库移除（reconcile 删除）时，懒清理（读取时过滤不存在 songId，写回时清除）。
- API：`loadScrapeQueue` / `enqueueScrapeSongs(ids)` / `removeScrapeSongs(ids)` / `clearScrapeQueue` / `onScrapeQueueChanged`（事件广播，与 `SONGS_UPDATED_EVENT` 同模式）。

## 3. 入队入口（child 2）

- **播放页**（`PlayerPage.vue` 歌曲操作菜单, 现有 `m-actions`，约 L360）：新增「标记待刮削/取消标记」项；有标记状态时给出反馈（toast + 菜单项文案切换）。
- **歌曲列表**（`SongsPage.vue`）：长按项操作菜单新增「加入待刮削」；多选条（L167 起）新增「标记待刮削」按钮；详情/长按单个操作同。
- **筛选批量**（`SongsPage` 或设置页入口）：一键筛选可疑歌曲 → 确认框 → 批量入队。判定函数复用/扩展：
  - `title` 等于 `getTitleFromPath(path)`（文件名占位，`metadata/util.isWeakTitle` 已有近义判定需核对）
  - 缺 artist 或 album（`isBlank`）
  - 缺 cover 或 lyrics
  - 来源为 `cloud` 且歌词 `lyricsSource === 'online'` 视为低可信
  - 入队后 toast 显示数量，可跳转刮削页。
- 新页面路由（刮削中心）：`/scrape`（`src/router/index.ts`），tab 里不放主入口，先由入队反馈/设置页进入（或歌曲页顶部入口图标）。

## 4. 刮削流程页（child 3）

单页三态：**队列 → 匹配中 → 差异预览 → 应用**。

### 4.1 匹配编排

- 输入：队列 songId 列表（resolve 到最新 `SongItem`）。
- 每首歌并行执行三路匹配（复用 `searchEditCloudMeta` 层，`src/features/editMeta/searchEditCloudMeta.ts`，它已封装文本/封面/歌词候选 + `EditCloudMetaResult`）：
  - 文本：`matchOnlineTextMeta`（多源候选）
  - 封面：`matchOnlineCoverRemote`
  - 歌词：`matchOnlineLyrics`
- 匹配结果**不写库**，只展示（与现有在线补缺区分开）。

### 4.2 差异预览

每行歌曲展示：
- 当前曲库值 vs 云端候选（title/artist/album/cover 有图预览/歌词格式）
- 匹配置信度徽标：高（exact title + artist 命中 + 时长偏差 < 阈值）/ 低（title 仅 contains 或 artist 缺失）
- 默认勾选策略：高置信默认勾选写回；低置信默认不勾选、可点开候选列表手动选（复用编辑页候选 UI 思路）。
- 操作：全部勾选/全部取消/逐行切换/点行看候选列表。

### 4.3 写回（R7 核心，`features/scrape/writeback.ts`）

确认后批量写回，逐曲独立结果：

1. **写文件**：本地 → `writeLocalAudioMetadata`；WebDAV → `writeWebDavAudioMetadata`（密码从 sources storage 取，缺密码该行失败）。payload 仅含用户勾选字段；cover 需先 `cacheRemoteCover` 落本地 file://（复用现有）。
2. **写库**：`upsertSong` 带新值 + `metaSources[key]='embedded'`（文件写成功）/ `'cloud'`（文件写失败，值仍入库但来源 cloud，供重试与 UI 识别），并作废旧在线补缺 token（防并发覆盖）。
3. **回滚 journal**：写前把每首歌旧值快照存 localStorage key `muses:scrape-rollback`（上限如 200 条，按 songId 覆盖）；页面提供一次性「撤销本次刮削」恢复旧值（写库回滚；文件不可逆——已写入文件的值无法恢复，仅警告，不承诺文件级回滚）。
4. 每行结果状态：成功 / 文件失败(已入库) / 失败，可重试（重试 = 重新仅执行失败行的写回）。
5. WebDAV 写回必须串行或限并发（避免单连接压力），本地可并行。

### 4.4 在线补缺语义微调（防再次「乱」）

- 现有「播放时在线补缺自动写库」**保留**（避免行为回退影响听歌体验），但写库字段带 `cloud` 来源标记，UI 可识别；歌词继续受 `shouldPersistOnlineLyrics` 门槛。
- 刮削确认写回后的字段来源变 `embedded`，后续扫描不再回退。

## 5. 匹配质量升级（child 4，`features/lyrics/score.ts` + `metadata/util.ts`）

- 采纳门槛：`MIN_ACCEPT_SCORE` 从 60（title contains）抬升——要求 **title exact(100) 才可无 artist 采纳；title contains(60) 必须 artist 命中(≥25)；有 duration 时偏差 ≤ 阈值(如 5s) 加分/强制约束**。
- `shouldPersistOnlineLyrics`：仅当命中高置信（exact + artist use + 时长符合）才可覆盖 lrc 现有词；ttml/yrc/qrc 质量升级保留但同样加 duration/artist 校验。
- 文本补缺 `needsOnlineTextMeta` 增加「cloud 来源字段需时长/歌手齐备才补」约束。
- 候选呈现：低置信/多候选时返回 `items[]`（现有 `EditDimResult` 结构）供刮削页候选选择；自动播放补缺路径只保留高置信自动写。

## 6. 兼容性与风险

- 数据模型向后兼容（可选新字段）；`metadataVersion` bump 触发懒扫自然升级。
- 不回退既有扫描/懒扫/在线补缺/手动编辑行为；只新增来源标记。
- 写文件不可逆：回滚承诺仅限曲库存量；UI 明示。
- WebDAV 写回失败：值入库 + `cloud` 来源 + 可重试，不污染播放。
- 匹配质量模块影响播放时在线歌词自动写库行为——需在验收时专门回归。

## 7. 关键文件清单

| 文件 | 变更 |
|------|------|
| `src/features/library/types.ts` | metaSources / 队列类型 |
| `src/features/library/storage.ts` | 来源写入规则、version 4、队列存取 |
| `src/features/library/scanner.ts` | 扫描写入标 embedded |
| `src/features/player/controller.ts` | 在线补缺标 cloud、token 作废 |
| `src/features/player/prefetchMetadata.ts` | 预取标 cloud |
| `src/views/PlayerPage.vue` | 标记入队入口 |
| `src/views/SongsPage.vue` | 多选/长按/筛选入队 |
| `src/features/scrape/*`（新） | 队列、写回、流程页 composable |
| `src/views/ScrapePage.vue`（新） | 刮削中心页 |
| `src/features/lyrics/score.ts` / `metadata/util.ts` | 门槛与约束 |
| `src/features/editMeta/searchEditCloudMeta.ts` | 复用入口（可能加多候选聚合） |