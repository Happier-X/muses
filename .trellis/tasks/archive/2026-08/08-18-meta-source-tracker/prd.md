# child1：元数据来源追踪 + 待刮削队列存储（数据模型层）

## Goal

为曲库治理提供数据地基：SongItem 可识别每个字段（title/artist/album/cover）的来源（embedded/cloud/manual），提供待刮削队列持久化。后续 child2/3 全部消费此层。

## Background / 依赖

- Parent: `08-18-library-tag-governance`（design.md §2 数据模型）。
- 现有：`storage.ts`（upsertSong/updateSongUserEdit/userEditedFields）、`metadataVersion=3`、`SONGS_UPDATED_EVENT` 广播模式。
- 歌词来源已存在 `lyricsSource`，本任务不重复建模。

## Requirements

- R1-1 `MetaFieldKey`/`FieldSource` 类型 + `SongItem.metaSources?`。
- R1-2 `upsertSong` 自动路径来源写入：扫描/懒扫写的字段标 `embedded`；在线补缺（prefetch/controller match*）写的字段标 `cloud`；不写空来源、不覆盖旧来源。
- R1-3 `updateSongUserEdit`：手改字段不单独存 manual（与 `userEditedFields` 派生一致），从 `metaSources` 移除该 key。
- R1-4 `getFieldSource(song, key)` 读取辅助：manual 由 `userEditedFields` 派生；缺省视为 `embedded`（存量兼容）。
- R1-5 `CURRENT_METADATA_VERSION` 3→4；`isSongItem` 扩展校验（新字段缺省合法，旧数据不丢）。
- R1-6 待刮削队列：localStorage `muses:scrape-queue`（`{version, items:[{songId, addedAt}]}`），入队幂等、读取时懒清理已删歌曲、事件广播；API load/enqueue/remove/clear。

## Acceptance Criteria

- [ ] SongItem 增字段后，旧 localStorage 数据加载不丢字段、不抛错。
- [ ] 扫描写入字段 `getFieldSource` = embedded；在线补缺 = cloud；手改 = manual（派生）。
- [ ] metadataVersion 4：懒扫触发 `shouldRefreshMetadata`（回归确认 controller 逻辑因 version 变化重扫）。
- [ ] 队列：幂等入队、去重、移除/清空、删除歌曲后懒清理；事件广播生效。
- [ ] 新增单测：storage 来源规则 ≥ 5 例、queue ≥ 6 例；vue-tsc build 通过；既有 19 例 vitest 不回归。

## Out of Scope

- 入队 UI（child2）、刮削页（child3）、评分门槛（child4）。