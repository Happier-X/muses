# 展示入库音乐数据设计

## 架构边界

本任务只消费已有歌曲库 metadata，不触碰扫描、播放、WebDAV 凭据或原生插件。

拟新增/调整：

- `src/features/library/views.ts`：封装展示层聚合逻辑，例如歌曲排序、格式化时长、专辑聚合、艺术家聚合。
- `src/views/SongsPage.vue`：歌曲列表页，由原 `Tab1Page.vue` 重命名。
- `src/views/AlbumsPage.vue`：专辑聚合页，由原 `Tab2Page.vue` 重命名。
- `src/views/ArtistsPage.vue`：艺术家聚合页，由原 `Tab3Page.vue` 重命名。
- `src/views/PlaylistsPage.vue`、`src/views/SettingsPage.vue`：语义化占位页，替代原先复用 `Tab1Page.vue` / `Tab3Page.vue` 的路由占位，避免歌单/设置误显示歌曲或艺术家内容。
- `tests/unit/library-views.spec.ts`：覆盖聚合 helper。
- `tests/unit/example.spec.ts` 或新增页面测试：更新 starter 页面测试，覆盖新页面可见内容。

## 数据流

1. 页面挂载时调用 `loadSongs()` 读取 `muses:songs`。
2. 页面使用本地 `ref<SongItem[]>` 保存当前列表。
3. Ionic 生命周期或 Vue 生命周期触发时刷新数据，保证用户从音源页扫描后切回能看到最新数据。
4. 歌曲页直接展示歌曲列表。
5. 专辑页调用 `groupSongsByAlbum(songs)` 生成聚合项。
6. 艺术家页调用 `groupSongsByArtist(songs)` 生成聚合项。

## 聚合规则

- 歌曲排序：默认按标题中文友好排序；标题相同时按路径排序。
- 专辑 key：`song.album?.trim()`，空值统一为“未知专辑”。
- 艺术家 key：`song.artist?.trim()`，空值统一为“未知艺术家”。
- 专辑艺术家摘要：收集专辑内非空艺术家，去重后用 `、` 连接；没有则显示“未知艺术家”。
- 艺术家专辑数：按非空专辑去重；无专辑歌曲归入“未知专辑”并计入专辑数。
- 时长格式化：支持 `m:ss` 或 `h:mm:ss`；缺失时不展示或显示“未知时长”。

## UI 设计

- 三个页面使用标准 `ion-page`、`ion-header`、`ion-toolbar`、`ion-title`、`ion-content`。
- 有数据时使用 `ion-list` / `ion-item` 展示主要信息。
- 空状态使用居中布局，提示“还没有歌曲/专辑/艺术家”，并说明“请先到音源页扫描入库”。
- 本任务不新增跨页导航；如用户后续需要专辑详情/艺术家详情，另开任务。

## 兼容性与安全

- 只读取 `muses:songs` 中的 song metadata。
- 不调用 `getWebDavPassword`、SecureStorage、WebDAV 原生插件或任何凭据读取 API。
- 无效歌曲记录由 `loadSongs()` 过滤，页面不额外信任 raw localStorage。

## 回滚形态

- 页面改造集中在 `SongsPage.vue`、`AlbumsPage.vue`、`ArtistsPage.vue`。
- 聚合 helper 独立在 `src/features/library/views.ts`，可单独回滚。
- 不改变歌曲库存储结构。
