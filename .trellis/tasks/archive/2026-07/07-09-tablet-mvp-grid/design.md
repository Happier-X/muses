# 技术设计：平板 MVP 响应式断点

## 架构与边界

本任务为纯前端响应式改造，分三层改动：

```
全局变量层 (src/theme/variables.css)
  └─ 定义 --muses-breakpoint-tablet / --muses-content-max-width

布局层 (src/views/TabsPage.vue)
  └─ ion-split-pane + ion-menu（宽屏侧栏）+ 现有 ion-tabs（窄屏底部 bar）

内容层 (各列表页 SongsPage / AlbumsPage / ArtistsPage)
  └─ CSS Grid 多列 + 内容最大宽度限位居中
```

## 数据流

无新增数据流，纯 UI/CSS 改动。所有状态（路由、播放器）不改变。

## 关键技术决策

### D1 分屏导航：ion-tabs 窄屏 + ion-split-pane/ion-menu 宽屏

TabsPage 当前结构：
```html
<ion-page><ion-tabs>
  <ion-router-outlet/>
  <ion-tab-bar slot="bottom">6 个 ion-tab-button</ion-tab-bar>
</ion-tabs></ion-page>
```

宽屏方案（保留 ion-tabs 不重写路由结构）：
```html
<ion-page>
  <ion-split-pane :when="TABLET_BREAKPOINT" content-id="muses-main">
    <ion-menu side="start" content-id="muses-main">
      <ion-content>
        <ion-list>
          6 个 ion-item（routerLink 指向 /tabs/*，与底部 bar 一致）
        </ion-list>
      </ion-content>
    </ion-menu>
    <div id="muses-main"><ion-tabs>...现有...</ion-tabs></div>
  </ion-split-pane>
</ion-page>
```

- 宽屏：`ion-split-pane` 显示左侧 menu（ion-menu 标签竖排），tab-bar 通过 CSS 在宽屏 (`@media (min-width: 768px)`) 隐藏（`display: none`）。
- 窄屏：`ion-split-pane when="768"` 不激活侧栏，回退为只渲染 content 子节点（即 ion-tabs + 底部 bar），与现在一致。
- ion-menu 在宽屏不需 hamburger 切换（始终展开），用 `menu-id` 不绑 ion-menu-button，靠 routerLink 切页。

**取舍**：保留 ion-tabs 而非完全替换为 split-pane 路由，是为了：
1. 窄屏 zero 回归风险（路由结构完全不动）
2. 宽屏导航与窄屏共用 ion-router-outlet，无双路由栈

**风险**：ion-split-pane 在某些 Ionic 版本对 nested ion-tabs 的 layout 计算需 CSS 兜底；implement 阶段需实测宽屏下 ion-content 高度是否正确撑满（可能需给 #muses-main `height:100%` / `display:flex`）。这是已知风险点，写入 implement.md return-check。

### D2 列表多列：外层 CSS Grid 包 ion-list

SongsPage/AlbumsPage/ArtistsPage 改动：在 `<ion-content>` 内部、`<ion-list>` 外层包一个 `.list-grid` div：

```css
@media (min-width: var(--muses-breakpoint-tablet)) {
  .list-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 1px;
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
  /* ion-item 在网格内自适应 */
  .list-grid ion-item { width: 100%; }
}
```

- `minmax(320px, 1fr)`：每列最小 320px，可用宽度足够时自动多列。768px 屏宽下两列正好（侧栏约占后剩余宽度）。
- `gap: 1px`：模拟 ion-list 分隔线视觉，避免完全断开。
- 窄屏不应用此规则，ion-list 保持原单列 stack。

### D3 内容最大宽度限位：作用于各 ion-content 内部

对 SettingsPage/SourcesPage/PlaylistsPage 等非列表页，用通用工具类 `.tablet-content-limit`（或直接在 page scoped style 里用）：
```css
@media (min-width: var(--muses-breakpoint-tablet)) {
  .tablet-content-limit {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
```
列表页的 `.list-grid` 自带 max-width，不再重复加 .tablet-content-limit 到外层（避免双重限位冲突）。

### D4 全局变量集中

`src/theme/variables.css`（当前仅注释）新增：
```css
:root {
  --muses-breakpoint-tablet: 768px;
  --muses-content-max-width: 720px;
}
```
单一来源，所有页面引用，不硬编码 768/720。

## 兼容性

- iOS：ion-split-pane/ion-menu 同样适用（Apple 平板同理），但本任务验收以 Android 平板为主；CSS 通用，无平台分支。
- Capacitor WebView：`@media (min-width)` 在 WebView 中按 CSS 像素响应，等价于浏览器。
- 暗色模式：新增 CSS 不依赖 color，与现有 `prefers-color-scheme: dark` 共存。

## 回滚

全部改动为 CSS + TabsPage 模板 + 列表页外层包 div。回滚 = revert 这几个文件即可，无数据迁移、无插件依赖新增。