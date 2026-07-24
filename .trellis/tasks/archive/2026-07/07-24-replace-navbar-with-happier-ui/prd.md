# 将 navbar 替换为 happier-ui

## 目标

将项目内用户可见的顶部导航栏统一迁移到 `happier-ui@0.0.1` 的 `HNavBar`，消除页面对 Ionic navbar 组件的直接依赖，并保持现有导航与页面操作可用。

## 已确认事实

- `src/components/ui/index.ts` 已从 `happier-ui` 转出 `HNavBar`，无需新增依赖或本地包装组件。
- `HNavBar` 是普通 `<header>` 实现，支持 `title`、`showBack`、`fixed`、`safeArea` 属性，`left`、`title`、`right` 插槽，以及 `handleLeftClick`、`handleRightClick` 事件。
- 当前 navbar 分散在 `MPage.vue`、歌曲、歌单、歌单详情、播放队列、设置、音源等页面和音源相关 modal 中。
- 专辑页、艺术家页通过 `MPage` 间接使用 Ionic navbar。
- 歌单详情使用 `ion-back-button` 自动返回，歌曲页 navbar 下方还有第二层随机播放 toolbar；QueuePage 是自定义 overlay（非 `ion-page`），其顶栏同样由 `ion-header/ion-toolbar/ion-title` 组成。
- SourcesPage 含 4 个 modal（编辑音源、扫描设置、扫描进度、添加 WebDAV），头部均为 `ion-header/ion-toolbar/ion-title` + 右侧“关闭”文字按钮。
- `tests/unit/navbar-title.spec.ts` 和 `src/theme/variables.css` 当前专门约束 Ionic navbar 的标题居中样式，迁移后需要同步调整。

## 关键决策

- **放弃 Ionic 折叠大标题（`collapse="condense"`）体验**，所有页面统一为 `HNavBar` 单行居中标题（方案 A）。目标是彻底移除页面对 Ionic navbar 的直接依赖，navbar 视觉全局一致；代价是失去 iOS 大标题滚动动效。
- **SourcesPage 的 4 个 modal 头部一并迁移到 `HNavBar`**。目标是全项目 navbar 结构统一，不保留 Ionic navbar 尾巴；代价是 modal 内需要显式处理 `fixed` 和关闭按钮插槽。

## 要求

- 所有用户可见的页面级和 modal 顶部 navbar 使用 `HNavBar`。
- 保留现有标题、左右侧操作按钮、返回/关闭行为、无障碍标签和安全区适配。
- 保留歌曲页 navbar 下方“随机播放全部”操作区的功能和布局层级。
- 内容不得被固定 navbar 遮挡；在 Web、Android 和 iOS 安全区下保持可用。
- 删除仅服务于旧 Ionic navbar 的组件导入、全局样式和测试断言。
- 不改变歌曲列表、扫描流程、播放器、路由和 modal 的业务行为。

## 验收标准

- [x] 页面和 modal 的顶部 navbar 均由 `HNavBar` 渲染，不再使用 `ion-header`、`ion-toolbar`、`ion-title`、`ion-buttons`、`ion-back-button` 组成 navbar。
- [x] 所有标题与现有文案一致，长标题可省略且不会覆盖左右操作区。
- [x] 搜索、添加、编辑、删除、关闭、清空队列、返回等 navbar 操作行为保持不变。
- [x] 固定 navbar 和安全区不会遮挡正文或导致页面内容跳动。
- [x] 歌曲页“随机播放全部”操作区仍正常显示和工作。
- [x] 旧 Ionic navbar 专属全局 CSS 被移除或收敛，navbar 测试改为验证 `HNavBar` 使用契约。
- [x] `npm run lint`、`npm run build`、相关单元测试通过。

## 范围外

- 不修改 `happier-ui` npm 包源码或 API。
- 不重设计 navbar 的颜色、字号、间距和交互规范。
- 不迁移正文中的 Ionic 列表、内容容器、modal、按钮等非 navbar 组件。

## 待决策

- 无。
