# 设计：将 navbar 替换为 happier-ui

## 架构与边界

本次改造只替换各页面/overlay/modal 的**顶部导航栏结构**，不触碰正文列表、内容容器、业务逻辑和路由定义。所有 navbar 统一由 `happier-ui` 的 `HNavBar` 渲染。

`HNavBar` 是普通 `<header>` 元素，结构为 `left / title / right` 三栏 grid，提供：

- props：`title`、`showBack`、`backAriaLabel`、`fixed`（默认 `true`）、`safeArea`（默认 `true`）
- slots：`left`、`title`、`right`
- events：`handleLeftClick`、`handleRightClick`（点击左/右栏容器触发）

关键约束：`fixed=true` 时 `position: fixed`，会脱离文档流覆盖内容；`safeArea=true` 时加 `env(safe-area-inset-top)` 顶部内边距。

## 布局策略

### 页面级（ion-page 内）

沿用 Ionic 页面壳，把 `HNavBar` 作为 `ion-page` 的直接子级放在 `ion-content` **之前**，用 `:fixed="false"`：

```vue
<ion-page>
  <h-nav-bar title="…">…</h-nav-bar>
  <ion-content :fullscreen="false">…</ion-content>
</ion-page>
```

- `ion-page` 是 flex column；非 fixed 的 `HNavBar` 占据顶部自然高度，`ion-content` 填充剩余空间并独立滚动，正文不会被遮挡，无需额外 padding 偏移。
- `safeArea` 保持默认 `true`，替代原 `ion-header` 的状态栏避让。
- `ion-content` 从 `:fullscreen="true"` 改为 `false`（不再需要 header 半透明叠加与折叠联动）。
- 放弃 `collapse="condense"` 折叠大标题层（决策 A）。

### QueuePage overlay（非 ion-page）

`.queue-overlay` 是 `position: fixed` 的 flex column 容器，`HNavBar` 用 `:fixed="false"` 作为第一个 flex 项，`ion-content` 用 `flex: 1` 填充。左栏放返回按钮，右栏在 `queueState.hasItems` 时放清空按钮。

### modal 头部（ion-modal 内）

modal 内部有自己的定位上下文且无系统状态栏避让需求，`HNavBar` 用 `:fixed="false"` 且 `:safe-area="false"`，标题走 `title` prop 或 `title` slot，右栏 `right` slot 放"关闭"文字按钮（保留 `HButton`/`ion-button` 现有样式与 disabled 逻辑）。

## 各处映射

| 位置 | 标题 | 左栏 | 右栏 | 备注 |
|------|------|------|------|------|
| `MPage.vue` | `#title` slot | `#start` slot（有则渲染） | `#end` slot（有则渲染） | 移除 `condensedTitle` 相关逻辑与折叠层 |
| SongsPage | 歌曲 | — | 搜索按钮 | navbar 下方保留 shuffle 固定条 |
| PlaylistsPage | 歌单 | — | 新建 | |
| PlaylistDetailPage | `playlist?.name ?? '歌单'` | 返回（`showBack`） | 播放全部 | 返回逻辑见下 |
| QueuePage | 播放队列 | 返回 | 清空（条件） | overlay |
| SettingsPage | 设置 | — | — | |
| SourcesPage | 音源 | — | 添加 | |
| SourcesPage modal ×4 | 编辑音源/扫描设置/扫描进度/添加 WebDAV | — | 关闭（文字，含 disabled） | `:fixed="false" :safe-area="false"` |

### 返回按钮处理

原 `PlaylistDetailPage` 用 `ion-back-button default-href="/tabs/playlists"`。改为 `HNavBar` 的 `showBack` + `@handleLeftClick`，用 `useIonRouter()`：能返回则 `ionRouter.back()`，否则 `ionRouter.navigate('/tabs/playlists', 'back', 'pop')`，保持原 default-href 语义与转场方向。

QueuePage 返回继续调用 `closeQueueOverlay()`（不变），改挂到 `@handleLeftClick` 或 `left` slot 内按钮。

### SongsPage shuffle 固定条

原为 `ion-header` 内第二个 `ion-toolbar.shuffle-toolbar`。改为 `HNavBar` 与 `ion-content` 之间的独立固定块（如 `<div class="shuffle-bar">`），与 navbar 一起保持固定、不随列表滚动。功能与点击语义（`clearQueue → enqueueSongs → toggleShuffle → selectSongAtIndex(0) → playSong`）完全不变。`scroll-margin-top` 依据新的"navbar + shuffle 条"实测高度调整（navbar ~56 + shuffle ~48 ≈ 104，保留缓冲，替代原依赖双 ion-toolbar 的 120px）。

## 全局样式与测试

- 删除 `src/theme/variables.css` 中仅服务 Ionic navbar 的规则：`ion-header ion-toolbar > ion-title:not([size="large"])` 绝对居中、`ion-header ion-toolbar > ion-buttons` 层级、`ion-header` 阴影相关（若 navbar 全部脱离 ion-header，可一并收敛；保留仍被其它 ion-header 使用的通用修正）。`HNavBar` 自带居中样式，无需全局补丁。
- 重写 `tests/unit/navbar-title.spec.ts`：从断言 `ion-title` markup 改为断言各页面/modal 使用 `HNavBar`（`<h-nav-bar` 或组件标签）承载对应标题，且不再出现 navbar 用途的 `ion-header/ion-toolbar/ion-title`。

## 兼容与回归

- Web / Android / iOS 安全区：页面级 `safeArea=true` 覆盖状态栏；modal 内关闭。
- 宽屏平板：`.tablet-content-limit` / `.list-grid` 限位逻辑不变，仅 navbar 结构变化。
- 不改动 `happier-ui` 包、路由表、播放器与扫描逻辑。

## 回滚

改动集中在各 `.vue` 模板/脚本/样式、`variables.css`、测试文件，无数据与配置迁移。`git revert` 或还原相关文件即可回滚，无副作用。
