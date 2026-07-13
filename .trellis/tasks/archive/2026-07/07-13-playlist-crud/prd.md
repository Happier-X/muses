# 歌单增删改查

## Goal

完成本地歌单的增删改查：列表/详情、新建与重命名、删除、向歌单加减歌曲，并能从歌单播放。

## Confirmed Facts（代码可证）

- `PlaylistsPage.vue` 仅为占位：「歌单功能待开发」。
- 路由已有 `/tabs/playlists`，Tabs 已挂「歌单」；**无**详情子路由。
- 无 `src/features/playlist`；持久化惯例为 `localStorage` + `muses:*` 键（songs/sources/queue）。
- 曲库 `SongItem` / `loadSongs` 已存在；播放：`playSong` / `enqueueSong(s)` / `clearQueue` / `selectSongAtIndex` / `toggleShuffle`。
- 歌曲页「随机全部」模式：`clearQueue` → `enqueueSongs` → 可选 shuffle → `playSong(first)`。
- 歌曲行「更多」当前直接 `enqueueSingleSong`，无 ActionSheet。

## Decisions

| 决策 | 结论 |
|------|------|
| MVP 功能边界 | **A**：完整本地 CRUD + 播放；不做拖拽排序、封面拼图、导入导出 |
| 加歌入口 | **A**：歌曲行「更多」→ ActionSheet：「添加到队列」+「加入歌单…」→ 选已有/新建；详情内可移除 |
| 歌单内排序 | **首版不排序**（保持加入顺序；二期） |
| 删除确认与命名规则 | **A**：删歌单需确认；名称 trim 非空；允许重名；详情移曲不二次确认 |
| 播放全部 | 对齐歌曲页「随机全部」的非 shuffle 路径：清空队列 → 入队有效曲 → 播第一首（详情可再提供「随机播放」若成本低，非必须） |

## Requirements

1. **R1** 本地持久化：`Playlist { id, name, songIds[], createdAt, updatedAt }`，键 `muses:playlists`；变更可派发事件便于多页刷新。
2. **R2** 列表页：展示名称与有效曲目数；新建 / 重命名 / 删除；删歌单确认；名称 trim 非空，允许重名。
3. **R3** 详情页：按 `songIds` 顺序展示仍存在于曲库的歌曲；可移除曲目（不二次确认）；失效 id 跳过展示。
4. **R4** 歌曲页「更多」ActionSheet：添加到队列；加入歌单（列表选择或新建后加入）；同一歌单内重复 id **忽略**（幂等）。
5. **R5** 详情「播放全部」：有效曲目非空时，清空队列并入队后从第一首播放。
6. **R6** 空列表/空详情友好空态；与 MiniPlayer / 沉浸式现有行为不回归。

## Acceptance Criteria

- [ ] 可新建、重命名、删除歌单，重启应用后仍在（localStorage）。
- [ ] 可从歌曲「更多」将曲加入指定/新建歌单；重复加入不产生双份。
- [ ] 详情可移除曲目；库中已删歌曲不出现在详情。
- [ ] 播放全部后队列与当前曲与歌单有效曲一致。
- [ ] 单测覆盖 storage CRUD 与关键边界；现有测试不红。

## Out of Scope

- 云同步 / 账号
- 智能歌单 / 自动规则
- 歌单封面拼图
- 导入导出 M3U
- 拖拽排序 / 上移下移
- 详情内「从曲库勾选添加」（首版靠歌曲页入口）

## Task Type

Complex
