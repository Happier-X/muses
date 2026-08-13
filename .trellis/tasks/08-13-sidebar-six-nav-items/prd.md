# 侧边栏拆分为六项独立导航

## 目标

把侧边栏菜单从「歌曲 / 音乐库 / 音源 / 设置」4 项改为「歌曲 / 专辑 / 艺术家 / 歌单 / 音源 / 设置」6 项，恢复聚合前的独立页面形态（用当前 `m-*` 组件实现）。

## 背景与已确认事实

- 当前侧边栏为 4 项；「音乐库」是聚合页 `CategoriesPage`（标题栏 + 专辑/艺术家/歌单 segmented 切换，三个子页面以 v-show 内嵌、无自身导航栏）。
- 路由中 `/tabs/albums`、`/tabs/artists`、`/tabs/playlists` 目前都重定向到 `/tabs/categories`。
- 聚合前的历史形态（`88b6c8d` 之前）：6 项导航（歌曲/专辑/艺术家/歌单/音源/设置），`/tabs/albums|artists|playlists` 直接渲染各自页面，三个页面自带导航栏（专辑/艺术家/歌单标题，歌单页右上角有「新建歌单」按钮）。本次目标是恢复该形态。
- `AlbumsPage`（专辑网格，点击进入 `/tabs/library/album/:name`）、`ArtistsPage`（艺术家网格，点击进入 `/tabs/library/artist/:name`）、`PlaylistsPage`（歌单列表，点击进入 `/tabs/playlists/:id`）三个页面已存在且功能完整，仅缺独立导航栏。
- 详情页返回逻辑目前使用 `setCategoriesSegment` + `router.replace('/tabs/categories')` 兜底。
- `src/App.vue` 的 `topLevelPaths` 目前为 `['/tabs/songs', '/tabs/categories', '/tabs/sources', '/tabs/settings']`，决定 Android 返回键是否退出应用。
- 导航菜单（抽屉 + 平板侧栏共用 `navItems`）、推屏轨道、汉堡入口、路由高亮 `isNavActive`/`childPrefixes` 机制保留不变。

## 需求

- 侧边栏（移动端抽屉 + 平板固定侧栏）菜单改为：歌曲、专辑、艺术家、歌单、音源、设置。
- `/tabs/albums`、`/tabs/artists`、`/tabs/playlists` 直接渲染各自页面，不再重定向到 `/tabs/categories`。
- 三个页面各自补独立导航栏：标题（专辑/艺术家/歌单）+ 汉堡入口自动注入；歌单页保留右上角「新建歌单」按钮。
- 详情页返回目标改为各自父页面：专辑详情 → `/tabs/albums`、艺术家详情 → `/tabs/artists`、歌单详情 → `/tabs/playlists`。
- 移除「音乐库」聚合页作为导航入口。
- 更新 `topLevelPaths` 为六个一级路径，保持 Android 返回行为正确。
- 详情页父入口高亮：专辑 → `/tabs/library/album/`、艺术家 → `/tabs/library/artist/`、歌单 → `/tabs/playlists/`。

## 验收标准

- [ ] 侧边栏显示 6 项：歌曲、专辑、艺术家、歌单、音源、设置；移动端抽屉和平板固定侧栏一致。
- [ ] 点击专辑/艺术家/歌单分别进入 `/tabs/albums`、`/tabs/artists`、`/tabs/playlists` 页面，页面有对应标题导航栏和汉堡入口。
- [ ] 歌单页右上角「新建歌单」按钮可用，行为与原音乐库分段页一致。
- [ ] 专辑/艺术家/歌单详情页返回后回到对应父页面；父入口在详情页保持高亮。
- [ ] 歌曲、音源、设置三个页面不回归；推屏侧栏开关、左滑/右滑、Escape、透明关闭区行为不回归。
- [ ] 删除/停用 CategoriesPage 后无死引用；`/tabs/categories` 不再作为导航入口（可保留兼容重定向）。
- [ ] Android 返回键在六个一级页面上退出应用，在详情页返回上一页。
- [ ] `npm run build`、`npm run lint`、`git diff --check`、Capacitor Android 同步与 Debug 构建通过。
- [ ] MuMu WebView 110 运行态验证：6 项菜单渲染、四入口导航、三个新页面导航栏、详情返回与高亮、无 JS/Vue/崩溃错误。

## 范围外

- 不改变歌曲/音源/设置页面内容与业务逻辑。
- 不改变专辑/艺术家/歌单页的数据加载、卡片/列表结构或点击行为。
- 不引入新的 UI 组件或依赖。
- 不改变推屏侧栏几何、手势或层级契约。
