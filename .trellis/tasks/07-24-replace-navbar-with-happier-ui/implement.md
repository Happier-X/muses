# 执行计划：将 navbar 替换为 happier-ui

## 前置

- 已读 `.trellis/spec/frontend/component-guidelines.md`（navbar 居中、shuffle 条、组件边界约束）。
- `HNavBar` 已在 `src/components/ui/index.ts` 转出，直接从 `@/components/ui` 引入。

## 实施顺序

按"底座 → 简单页 → 复杂页 → overlay/modal → 样式/测试"推进，每步局部可验证。

1. **MPage 壳（底座）** `src/components/ui/MPage.vue`
   - 用 `HNavBar` 替换 `ion-header/ion-toolbar/ion-title/ion-buttons`；`#title` 走 title slot，`#start`/`#end` 分别映射到 `left`/`right` slot（有内容才渲染）。
   - 移除 `condensedTitle` prop、折叠大标题层及相关 import。
   - `ion-content` 保留，`:fullscreen` 视需要设为 `false`。
   - 影响 AlbumsPage / ArtistsPage（无需改动其页面代码，验证 `#title` 仍生效）。

2. **SettingsPage** `src/views/SettingsPage.vue`
   - 最简单：`HNavBar title="设置"`，移除折叠层与 Ionic navbar import。

3. **PlaylistsPage** `src/views/PlaylistsPage.vue`
   - `HNavBar title="歌单"`，右栏放"新建歌单"按钮（`right` slot，保留 `aria-label` 与 `openCreateAlert`）。

4. **SourcesPage 主页** `src/views/SourcesPage.vue`
   - `HNavBar title="音源"`，右栏"添加音源"按钮。

5. **SongsPage** `src/views/SongsPage.vue`
   - `HNavBar title="歌曲"`，右栏搜索按钮。
   - shuffle 条改为 navbar 与 `ion-content` 之间的独立固定块，保留 `.shuffle-actions` 布局与点击语义。
   - 重算 `.song-item` 的 `scroll-margin-top`（navbar + shuffle 实测，约 104–112px），验证 FAB 跳转不被顶栏遮挡。

6. **PlaylistDetailPage** `src/views/PlaylistDetailPage.vue`
   - `HNavBar :title="playlist?.name ?? '歌单'"`，`show-back` + `@handleLeftClick` 用 `useIonRouter()` 实现返回（能返回则 `back()`，否则 `navigate('/tabs/playlists','back','pop')`）。
   - 右栏"播放全部"按钮（保留 disabled 与 `onPlayAll`）。

7. **QueuePage overlay** `src/views/QueuePage.vue`
   - `.queue-overlay` 内首个 flex 项换 `HNavBar :fixed="false"`；左栏返回（`goBack`→`closeQueueOverlay`），右栏条件清空按钮（保留 `color="danger"` 观感与 `aria-label`）。

8. **SourcesPage 4 个 modal 头部** `src/views/SourcesPage.vue`
   - 每个 modal 头部换 `HNavBar :fixed="false" :safe-area="false"`，标题走 title，右栏"关闭"文字按钮（保留 disabled 逻辑：编辑用 `isEditSaving`、扫描进度用 stage 判断）。

9. **全局样式收敛** `src/theme/variables.css`
   - 删除仅服务 Ionic navbar 的 `ion-title` 绝对居中、`ion-buttons` 层级等规则；保留仍被复用的通用 chrome 修正。

10. **测试重写** `tests/unit/navbar-title.spec.ts`
    - 改为验证各页面/modal 使用 `HNavBar` 承载标题，且不再用 `ion-header/ion-toolbar/ion-title` 组成 navbar。

11. **spec 同步**（收尾）
    - 更新 `.trellis/spec/frontend/component-guidelines.md` 中 navbar 居中全局规则、shuffle 条"放在第二个 ion-toolbar"等已过时描述，改述为 `HNavBar` 契约。

## 验证命令

```bash
npm run lint
npm run build
npx vitest run tests/unit/navbar-title.spec.ts
npx vitest run
```

## 风险与回滚点

- **SongsPage scroll-margin-top**：navbar/shuffle 高度变化后跳转定位易回归遮挡，需实测调值。
- **PlaylistDetailPage 返回**：`useIonRouter` 转场方向/默认回退需验证（直接进入详情页时也能回到歌单）。
- **QueuePage overlay 层级**：`HNavBar :fixed="false"` 不得破坏 `z-index: 1200` 叠放与 flex 滚动。
- **modal safeArea**：modal 内必须 `:safe-area="false"`，否则顶部多出状态栏空白。
- 各文件改动独立，出问题按文件粒度 `git checkout -- <file>` 回滚。
