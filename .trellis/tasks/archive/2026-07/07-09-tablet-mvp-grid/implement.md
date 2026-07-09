# 执行计划：平板 MVP 响应式断点

## 实现顺序

### 步骤 1：全局变量（src/theme/variables.css）
- 在 `:root` 新增 `--muses-breakpoint-tablet: 768px;` 和 `--muses-content-max-width: 720px;`
- 保留文件原有注释
- 验证：`npm run build` 通过

### 步骤 2：TabsPage 宽屏侧栏（src/views/TabsPage.vue）
- 显式 import `IonSplitPane, IonMenu, IonList, IonItem, IonIcon, IonLabel`
- 模板：外层包 `ion-split-pane :when="768" content-id="muses-main"`
- 内层加 `ion-menu side="start" content-id="muses-main"`，里面放 6 个 `ion-item :router-link` 指向 `/tabs/songs`、`/tabs/albums`、`/tabs/artists`、`/tabs/playlists`、`/tabs/sources`、`/tabs/settings`，icon 与 label 与底部 bar 一致
- `<ion-tabs>` 内容包在 `<div id="muses-main">` 中（保持现有路由结构）
- scoped style：宽屏下 `ion-tab-bar { display: none; }`；窄屏 `ion-menu` 由 ion-split-pane 自动隐藏，但仍需确保不出现 hamburger 按钮（不使用 ion-menu-button）
- **风险点验证**：实测宽屏下 `#muses-main` 内 ion-tabs 高度是否撑满。若高度塌陷，加 `#muses-main { height: 100%; display: flex; }` 兜底
- 验证：`npm run build` 通过；窄屏浏览器 devtools 切到 375px 宽确认底部 bar 仍在；切换到 1024px 宽确认侧栏出现、底部 bar 消失

### 步骤 3：SongsPage 多列 + 限位（src/views/SongsPage.vue）
- `<ion-content>` 内、`<ion-list>` 外包一层 `<div class="list-grid tablet-content-limit">`
- scoped style 加 R2 网格规则（D2），引用全局 `--muses-breakpoint-tablet` / `--muses-content-max-width`
- 空状态 `<div class="empty-state">` 不在 list-grid 内，保持原样
- 验证：`npm run build`；宽屏 Songs 显示 ≥2 列；窄屏单列无变化

### 步骤 4：AlbumsPage 多列 + 限位（src/views/AlbumsPage.vue）
- 同步骤 3 模式包 `.list-grid`
- 验证同上

### 步骤 5：ArtistsPage 多列 + 限位（src/views/ArtistsPage.vue）
- 同步骤 3 模式包 `.list-grid`
- 验证同上

### 步骤 6：其它非列表页内容限位（src/views/SettingsPage.vue、PlaylistsPage.vue）
- 在 `<ion-content>` 内层包 `<div class="tablet-content-limit">`
- SourcesPage 用虚拟列表卡片绝对定位，**不强加网格**，但根容器可加 `.tablet-content-limit`（注意虚拟滚动 transform 依赖父容器定位，仅加 max-width 居中，不动 height/overflow）
- 验证：宽屏 Settings/Sources 内容居中且 ≤720px；Sources 虚拟滚动不破

## 验证命令

```bash
npm run lint       # 零错误
npm run build      # vue-tsc + vite build 通过
npm run test:unit  # 不引入新的失败（player.spec.ts 已有失败忽略）
```

真机/平板视觉验证：
- Capacitor WebView 在 ≥768px 下断点生效（与浏览器 devtools 一致）
- 真机窄屏（手机）无回归

## 回滚点

每步骤后 `npm run build` 通过即视为可回滚单元。整体回滚 = revert 这 6 个文件。

## 完成检查（完成全部步骤后交给 trellis-check）

- [ ] 6 个文件已修改，build/lint/test 全绿
- [ ] 窄屏无回归（bottom tab bar + 单列 list）
- [ ] 宽屏 ≥768px：侧栏 + 多列 + 内容居中 ≤720px
- [ ] SourcesPage 虚拟滚动未破
- [ ] 暗色模式正常