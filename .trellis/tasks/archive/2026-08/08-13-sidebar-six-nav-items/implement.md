# 实施计划：侧边栏六项独立导航

## 实施步骤

1. 阅读前端规范与当前 `TabsPage.vue`、`router/index.ts`、三个列表页、两个详情页、`App.vue`、`categoriesSegment.ts`，确认引用关系。
2. 更新 `src/router/index.ts`：`/tabs/albums|artists|playlists` 改为直接渲染各自页面；`/tabs/categories` 改为兼容重定向到 `/tabs/albums`。
3. 更新 `src/views/TabsPage.vue` 的 `navItems` 为 6 项（含 childPrefixes），图标复用 `@/icons` 现有导出。
4. 给 `AlbumsPage.vue`、`ArtistsPage.vue` 补 `MNavbar`（标题 + 汉堡自动注入），保持网格内容不变。
5. 给 `PlaylistsPage.vue` 补 `MNavbar`（标题「歌单」+ 右上角「新建歌单」按钮，页内直接调用 `openCreateAlert`），保持列表内容不变。
6. 更新详情页返回：`PlaylistDetailPage.goBack` 兜底改 `/tabs/playlists`；`LibraryDetailPage.goBack` 兜底按 kind 改 `/tabs/albums` 或 `/tabs/artists`；移除 `setCategoriesSegment` 依赖。
7. 删除 `CategoriesPage.vue` 与 `categoriesSegment.ts`（确认无其他引用），清理相关 import。
8. 更新 `src/App.vue` `topLevelPaths` 为六个一级路径。
9. 更新 `.trellis/spec/frontend/component-guidelines.md`：侧栏菜单从 4 项改为 6 项独立页面契约，移除「音乐库聚合页」描述。
10. 运行 `npm run build`、`npm run lint`、`git diff --check`、`npx cap sync android` 与 Android Debug 构建。
11. MuMu WebView 110 运行态验证：6 项菜单、三个新页面导航栏与汉堡、歌单新建、详情返回与高亮、Android 返回行为、无 JS/Vue/崩溃。
12. 全量复检、提交代码与规范、归档任务。

## 风险与检查点

- CategoriesPage 删除后，检查所有 `import CategoriesPage` / `from './CategoriesPage.vue'` 残留。
- `categoriesSegment.ts` 删除前确认 `getCategoriesSegment/setCategoriesSegment` 无其他调用方（仅 CategoriesPage 与两个详情页）。
- PlaylistsPage 的「新建歌单」逻辑原来通过 CategoriesPage 的 `playlistsRef.openCreateAlert()` 触发，独立后需在 PlaylistsPage 内直接绑定，确保 `openCreateAlert` 仍可访问。
- 三个页面加 navbar 后保持 `m-page` flex 列布局，`m-content` 仍为唯一滚动区，避免出现双滚动。
- 平板侧栏和移动端抽屉共用 navItems，改一处即可；`isNavActive` 的 childPrefixes 需覆盖专辑/艺术家/歌单详情路径。

## 验证命令

```bash
npm run build
npm run lint
git diff --check
npx cap sync android
cd android && ./gradlew assembleDebug
```
