# 展示入库音乐数据

## 目标

把已经扫描入库的歌曲数据展示到“歌曲”“专辑”“艺术家”三个标签页，让用户能看到本地歌曲库中的歌曲明细，并按专辑、艺术家进行基础聚合浏览。

## 背景与已确认事实

- 当前路由已有 `/tabs/songs`、`/tabs/albums`、`/tabs/artists`，原先分别指向 starter 命名的 `src/views/Tab1Page.vue`、`src/views/Tab2Page.vue`、`src/views/Tab3Page.vue`。本任务将它们重命名为业务语义更清晰的 `SongsPage.vue`、`AlbumsPage.vue`、`ArtistsPage.vue`。
- 当前三个页面仍是 Ionic starter 占位内容，仅显示 `Tab 1/2/3` 和 `ExploreContainer`。
- 底部标签栏 `src/views/TabsPage.vue` 已显示“歌曲”“专辑”“艺术家”。
- 入库歌曲存储在 `localStorage` key `muses:songs`，通过 `src/features/library/storage.ts` 的 `loadSongs()` 读取。
- 歌曲模型 `SongItem` 已包含 `title`、`artist?`、`album?`、`duration?`、`sourceId`、`sourceType`、`path`、`uri` 等字段。
- 当前项目没有全局状态库；规范要求使用最小状态机制，页面可直接通过 feature-local helper 读取数据。

## 需求

- R1：歌曲页展示所有已入库歌曲，至少显示歌曲标题、艺术家、专辑、时长或来源信息中的可用字段。
- R2：专辑页按歌曲 `album` 聚合展示专辑列表，至少显示专辑名、歌曲数、艺术家摘要；缺失专辑时归为“未知专辑”。
- R3：艺术家页按歌曲 `artist` 聚合展示艺术家列表，至少显示艺术家名、歌曲数、专辑数；缺失艺术家时归为“未知艺术家”。
- R4：三个页面在没有入库歌曲时显示空状态，引导用户去“音源”页添加/扫描音源。
- R5：页面进入或重新激活时能读取最新 `muses:songs` 数据；用户扫描完成后返回这些页面应能看到新数据。
- R6：展示逻辑不得暴露 WebDAV 密码、Authorization Header 或 SecureStorage 值。
- R7：保留 Ionic 页面结构和 tab 路由，不引入全局状态库。

## 验收标准

- [ ] `/tabs/songs` 显示入库歌曲列表，不再是 starter 占位页。
- [ ] `/tabs/albums` 显示按专辑聚合的列表，缺失专辑归入“未知专辑”。
- [ ] `/tabs/artists` 显示按艺术家聚合的列表，缺失艺术家归入“未知艺术家”。
- [ ] 当 `muses:songs` 为空或无有效歌曲时，三个页面都有清晰空状态。
- [ ] 单元测试覆盖歌曲列表展示、专辑聚合、艺术家聚合和空状态。
- [ ] 页面只读取歌曲库 metadata，不读取或展示任何凭据。

## 技术边界

- 本任务只做列表/聚合展示，不实现播放、搜索、排序配置、详情页、封面图、专辑/艺术家二级页。
- 专辑页和艺术家页本轮不做点击展开或进入歌曲列表，只展示聚合列表。
- 可新增轻量的 `src/features/library/views.ts` 或类似 helper 来封装专辑/艺术家聚合，避免把聚合逻辑散落在页面里。
- 将 `Tab1Page.vue`、`Tab2Page.vue`、`Tab3Page.vue` 重命名并改造为 `SongsPage.vue`、`AlbumsPage.vue`、`ArtistsPage.vue`，同步更新路由和测试引用。

## 已决策问题

- Q1：本轮只做专辑/艺术家聚合列表展示，不做点击展开或详情页。

## 待决问题

- 无。
