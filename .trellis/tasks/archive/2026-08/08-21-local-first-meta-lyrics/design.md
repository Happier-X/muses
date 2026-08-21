# 技术设计：歌曲信息与歌词本地来源化

## 总体思路

三层改动：

1. **移除**播放器链路的自动在线匹配（controller.ts / prefetchMetadata.ts）；
2. **扩展**来源标记（`scrape`）并调整刮削写回标记；
3. **一次性迁移**清理存量 `online`/`cloud` 数据。

## 1. 播放器移除自动在线匹配

### controller.ts

- 删除 `matchOnlineLyricsForSong` 函数及调用点（约 398、1215 行）。切歌后歌词直接取库内值：
  - `song.lyrics?.trim()` 有值 → `state.lyrics = song.lyrics`，`onlineLyricsStatus = 'ready'`；
  - 无值 → `state.lyrics = null`，`onlineLyricsStatus = 'miss'`。
- 删除 `matchOnlineTextMetaForSong`、`matchOnlineCoverForSong` 及调度点（约 986-994、1034-1041 行）；相关 token（`onlineCoverToken`/`onlineTextToken`）一并删除。
- `syncDisplayStateFromSong` 中「库内词质量更优才覆盖」逻辑（`shouldApplyStoredLyricsOverRuntime`）简化：本地词是唯一来源，直接以库内值为准；用户手改强制覆盖分支保留。
- `prefetchNextTrack` 只保留 WebDAV 音频预取（`prefetchWebDavAudioFile`），移除 `prefetchNextMetadata` 调度与 `metadataPrefetchToken`。

### prefetchMetadata.ts

- 整文件删除（三路预取全部是在线能力）；`shouldPrefetchNextMetadata` 若仅剩音频预取需要，则内联进 controller，否则随文件一起删。

### player/types.ts

- `shouldPersistOnlineLyrics`、`shouldApplyStoredLyricsOverRuntime` 失去调用方后删除；`OnlineLyricsStatus` 收敛为 `'idle' | 'ready' | 'miss'`（去掉 `'matching'`/`'error'`）。

### PlayerPage.vue

- 空态文案（约 2140-2157 行）：去掉 matching/error 分支，miss 文案改为「未找到内嵌歌词或同目录同名 .lrc 文件，可在刮削页获取」。
- 歌词来源徽标等 UI 若引用 `'online'` 来源需兼容新枚举。

### 保留不动

- `src/features/lyrics/*`（providers/match/amllTtmlDb）：刮削页 `editMeta/searchEditCloudMeta.ts` 仍在使用。
- `src/features/metadata/*`、`src/features/cover/*`：刮削页仍在使用。

## 2. 来源标记扩展

- `library/types.ts`：`LyricsSource` 加 `'scrape'`；`FieldSource` 加 `'scrape'`；注释同步更新。
- `library/storage.ts`：
  - `sanitizeSongForStorage` / 校验函数接受 `'scrape'`；
  - `isUserEditedField` 等保护逻辑不变（manual 永远优先）。
- `scrape/writeback.ts`：写回失败分支 `metaSources.* = 'cloud'` → `'scrape'`；歌词 `lyricsSource: 'online'` → `'scrape'`（writeback.ts:193-213）。
- 关联判定检查：
  - `scrape/suspicious.ts` `isSuspiciousSongForScrape`：目前判 `cloud`，需把 `scrape` 也视为可疑（未写入文件的值仍值得重刮）；
  - `metadata/util.ts` 弱 title 再补缺门槛中 `cloud` 判定：自动补缺已删，该逻辑仅剩刮削页消费，确认无残留引用即可；
  - 全局搜索 `'cloud'` 与 `'online'` 字面量，逐一确认归属。

## 3. 存量清理迁移

- 位置：`library/storage.ts` `loadSongs()` 内做惰性迁移（读入后清洗 + 写回 + 打标），或独立 `runLocalFirstMigration()` 在 loadSongs 首次调用时执行。
- 迁移标识：`localStorage['muses:migration:local-first-v1'] = 'done'`，只执行一次。
- 清洗规则（对每条 SongItem）：
  - `lyricsSource === 'online'` → 删除 `lyrics`、`lyricsFormat`、`lyricsSource`；
  - `metaSources[field] === 'cloud'`（field ∈ title/artist/album/cover）→ 删除该字段值与标记（cover 同时清 `coverUri`）；
  - `userEditedFields` 含对应字段时跳过（manual 保护优先于清理）；
  - 其余字段原样保留。
- 迁移后触发 `SONGS_UPDATED_EVENT`，UI 自动刷新。

## 兼容与回滚

- 迁移有 localStorage 打标，重复启动不会二次执行；但**清理不可逆**（无备份），回滚代码只能恢复"不再清理"，已清数据无法找回——风险已在 PRD D5 由用户接受。
- 原生层零改动，Android 侧无需重新发版验证扫描行为。

## 测试策略

- 单测更新：`tests/unit/` 中涉及 `shouldPersistOnlineLyrics`、`shouldApplyStoredLyricsOverRuntime`、prefetch、writeback 来源标记的用例。
- 新增单测：迁移清洗规则（含 manual 保护跳过）、`scrape` 标记的 sanitize 往返。
- 手工验收：AC1/AC2 断网播放、AC5 刮削写回两分支。
