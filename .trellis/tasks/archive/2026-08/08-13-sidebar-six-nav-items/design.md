# 技术设计：侧边栏六项独立导航

## 1. 路由调整（`src/router/index.ts`）

- `/tabs/albums` → `AlbumsPage`（移除重定向）。
- `/tabs/artists` → `ArtistsPage`（移除重定向）。
- `/tabs/playlists` → `PlaylistsPage`（移除重定向）。
- `/tabs/categories` 保留一条兼容重定向到 `/tabs/albums`（避免旧书签/深层链接失效）；不再作为导航入口。若验收要求严格删除，可整条移除。
- 其余路由（songs/sources/settings/playlists/:id/library/:kind/:name）不变。

## 2. 导航菜单（`src/views/TabsPage.vue`）

`navItems` 更新为 6 项（恢复聚合前顺序，图标复用现有导出）：

```ts
const navItems: NavigationItem[] = [
  { to: '/tabs/songs', label: '歌曲', icon: musicalNotes },
  { to: '/tabs/albums', label: '专辑', icon: albums, childPrefixes: ['/tabs/library/album/'] },
  { to: '/tabs/artists', label: '艺术家', icon: person, childPrefixes: ['/tabs/library/artist/'] },
  { to: '/tabs/playlists', label: '歌单', icon: list, childPrefixes: ['/tabs/playlists/'] },
  { to: '/tabs/sources', label: '音源', icon: radio },
  { to: '/tabs/settings', label: '设置', icon: settings },
]
```

- `albums`（Disc3）、`person`（MicVocal）、`list`（ListMusic）、`radio`（Folder）均为 `@/icons` 现有导出，不新增图标。
- 抽屉和平板侧栏共用 `navItems`，无需其他改动。

## 3. 三个页面补独立导航栏

参考聚合前结构与当前 `m-*` 组件用法（如 SongsPage 的 MNavbar + MContent）：

- `AlbumsPage.vue`：模板加 `MNavbar`（标题「专辑」），内容保持 `albums-page__content` 网格；汉堡按钮由 `navigationDrawerKey` 自动注入（无显式 `#left`）。
- `ArtistsPage.vue`：同上，标题「艺术家」。
- `PlaylistsPage.vue`：加 `MNavbar`（标题「歌单」）+ `#right` 的「新建歌单」`MButton`（调用现有 `openCreateAlert` 逻辑，从 CategoriesPage 的 `playlistsRef` 调用改为页内直接调用）；内容保持列表结构。
- 三个页面均保持 `m-page` flex 列布局：navbar 在上、`m-content` 滚动区在下。

## 4. 详情页返回（`PlaylistDetailPage.vue` / `LibraryDetailPage.vue`）

- `PlaylistDetailPage.goBack`：去掉 `setCategoriesSegment('playlists')`，兜底 `router.replace` 目标改为 `/tabs/playlists`。
- `LibraryDetailPage.goBack`：去掉 `setCategoriesSegment(...)`，兜底目标按 `kind` 选择 `/tabs/albums`（album）或 `/tabs/artists`（artist）。
- 主路径仍是 `router.back()`（历史栈返回），兜底仅在无历史时生效。

## 5. 聚合页与分段状态清理

- `src/views/CategoriesPage.vue`：不再被路由使用；删除文件（其 navbar/segmented/新建按钮职责迁移至三个独立页面）。
- `src/features/player/categoriesSegment.ts`：若无其他引用则删除；`getCategoriesSegment/setCategoriesSegment` 的 import 从详情页移除。
- 检查 `src/components/ui` 是否因 CategoriesPage 删除而仍有需要的导出（MSegmented/MSegmentedButton 等保留，组件库不删）。

## 6. App.vue 返回行为

- `topLevelPaths` 更新为：`['/tabs/songs', '/tabs/albums', '/tabs/artists', '/tabs/playlists', '/tabs/sources', '/tabs/settings']`。
- 六个一级页 Android 返回退出应用；详情页返回上一页。逻辑本身不变。

## 7. 兼容与回滚

- 不新增依赖，不改 Capacitor 配置。
- 不触碰推屏轨道、手势、透明关闭区、MiniPlayer 与弹层层级。
- 回滚：恢复 4 项 `navItems` 与 categories 路由重定向、恢复 CategoriesPage 即可。
