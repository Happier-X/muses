# 特征·刮削模块 — 开发规范

> 本项目的曲库治理（刮削）能力在 `src/features/scrape/` 内实现。队列管理、匹配聚合、写回编排、可疑歌曲判定四个子模块分工明确。

---

## 范围/触发条件

涉及待刮削队列、云端匹配、差异预览、写回/回滚、可疑歌曲筛选的任何改动，都应在本规范约束下进行。

---

## 模块结构

```
src/features/scrape/
├── queue.ts        # 队列存储 + 事件广播
├── matcher.ts      # 匹配聚合编排
├── writeback.ts    # 写回 + 回滚 journal
└── suspicious.ts   # 可疑歌曲判定
```

---

## 1. 队列存储（queue.ts）

### 存储 Key

```
muses:scrape-queue
```

### 数据结构

```typescript
interface ScrapeQueueItem {
  songId: string
  addedAt: string  // ISO 8601
}

interface ScrapeQueueSnapshot {
  version: 1
  items: ScrapeQueueItem[]
}
```

### API

| 函数 | 签名 | 说明 |
|------|------|------|
| `loadScrapeQueue` | `() → ScrapeQueueSnapshot` | 读取队列（懒清理已删歌曲） |
| `enqueueScrapeSongs` | `(ids: string[]) → { added: number }` | 批量入队（幂等） |
| `removeScrapeSongs` | `(ids: string[]) → { removed: number }` | 批量移除 |
| `clearScrapeQueue` | `() → void` | 清空队列 |
| `isInScrapeQueue` | `(songId: string) → boolean` | 检查是否在队列中 |
| `onScrapeQueueChanged` | `(handler) → unsubscribe` | 订阅队列变化事件 |

### 事件广播

```
muses:scrape-queue-updated
```

### 设计决策：幂等性

`enqueueScrapeSongs` 按 `songId` 去重，重复入队只更新 `addedAt`。这保证播放页标记/列表多选/筛选批量三个入口不会重复入队。

### 设计决策：懒清理

`loadScrapeQueue` 在读取时过滤已从曲库删除的 `songId`，并写回一次保持存储干净。这避免 reconcile 时需要同步清理队列。

---

## 2. 匹配聚合（matcher.ts）

### 核心类型

```typescript
interface ScrapeCandidate {
  songId: string
  song: SongItem
  text: { current, candidates, defaultIndex }
  cover: { currentUri, candidates, defaultIndex }
  lyrics: { currentText, currentFormat, candidates, defaultIndex }
  overallConfidence: 'high' | 'low'
  defaultChecked: boolean  // 高置信默认勾选
}
```

### 置信度分级

- **高置信**：文本命中（exact+artist）或封面有候选或歌词有候选
- **低置信**：三维度均无候选
- 高置信 → `defaultChecked = true`，低置信 → `defaultChecked = false`

### 并发控制

`runWithConcurrency(tasks, limit=3)` 限制同时进行的网络请求数，避免大量歌曲同时匹配时的网络压力。

### 依赖

- `searchEditCloudMeta`（editMeta 层，不写库）
- `classifyTextMetaConfidence`（metadata/util.ts，判定文本命中置信度）

---

## 3. 写回编排（writeback.ts）

### 核心类型

```typescript
type WritebackStatus = 'success' | 'file-failed' | 'failed'

interface WritebackResult {
  songId: string
  status: WritebackStatus
  fileResult: WriteMetadataResult
  libraryUpdated: boolean
  error?: string
}

interface ScrapeChanges {
  title?: string
  artist?: string
  album?: string
  coverUri?: string
  coverRemoteUrl?: string
  lyrics?: string
  lyricsFormat?: string
}
```

### 写回流程

1. **写前快照**：旧值 → `muses:scrape-rollback`（回滚 journal，上限 200 条）
2. **写文件**：本地并行 / WebDAV 串行（避免单连接压力）
3. **写库**：`upsertSong` + `metaSources[key]` 按文件结果标记 `'embedded'`（成功）或 `'cloud'`（文件失败）
4. **作废 token**：调用 `invalidateOnlineTokens()` 防止在线补缺并发覆盖

### 回滚 journal

```
muses:scrape-rollback
```

```typescript
interface RollbackJournal {
  version: 1
  journalId: string
  entries: RollbackEntry[]
}

interface RollbackEntry {
  songId: string
  songBefore: Pick<SongItem, 'title' | 'artist' | 'album' | 'coverUri' | 'lyrics' | 'lyricsFormat' | 'lyricsSource' | 'metaSources'>
  createdAt: string
}
```

### 撤销语义

- `revertScrapeJournal(journalId)`：恢复曲库旧值
- **文件不可逆**：已写入音频文件的值无法恢复，UI 必须明示

### 设计决策：文件失败仍入库

当文件写入失败（如 WebDAV 密码缺失）时，值仍写入曲库（来源标记 `'cloud'`），保证播放器可读。失败行可后续重试。

---

## 4. 可疑歌曲判定（suspicious.ts）

### 规则

| 规则 | 说明 |
|------|------|
| artist/album/coverUri/lyrics 缺失 | 补缺基本字段 |
| 弱标题 + 缺 cover/lyrics | 文件名占位 + 其他弱信号 |
| lyricsSource=online | 在线歌词低可信 |
| metaSources.*=cloud | 历史低质量补缺 |

### 注意事项

- 弱标题（title=filename）**单独不触发**，需配合其他可疑信号（避免库中常态误报）
- `includeCloudSources=false` 可关闭来源相关规则

---

## 5. 在线补缺联动

### 联动机制

1. 刮削写回后调用 `invalidateOnlineTokens()` 作废在线补缺 token
2. 在线补缺写库字段已带 `'cloud'` 来源标记（child1 实现）
3. 播放时在线歌词自动写库受 `shouldPersistOnlineLyrics` 门槛（child4 实现）

### 来源追踪完整链路

```
扫描/懒扫 → metaSources[key] = 'embedded'
在线补缺 → metaSources[key] = 'cloud'
刮削写回(文件成功) → metaSources[key] = 'embedded'
刮削写回(文件失败) → metaSources[key] = 'cloud'
用户手改 → metaSources[key] = 'manual'（由 userEditedFields 派生）
```

---

## 常见错误

### Don't: 用 reactive() 管理队列状态

```typescript
// ❌ ESLint 禁止 reactive()
const state = reactive({ queue: [] })

// ✅ 用 ref
const queue = ref<ScrapeQueueItem[]>([])
```

### Don't: 在写回时忽略 token 作废

刮削写回后必须调用 `invalidateOnlineTokens()`，否则在线补缺会在下次播放时覆盖刮削值。

### Gotcha: WebDAV 写回需要串行

WebDAV 单连接，多并发写入会超时。writeback.ts 中 WebDAV 写回用 `for...of` 串行，本地写回用 `Promise.all` 并行。
