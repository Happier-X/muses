# 低风险 Ionic → happier-ui 替换

父任务：`07-24-replace-ionic-with-happier-ui`

## Goal

把明确可无损替换的 Ionic 组件换成 happier-ui，覆盖进度条、卡片、文字按钮、列表/导航栏 icon-only 按钮，视觉与交互零回归。

## Scope（本子任务范围）

### 1. `ion-progress-bar` → `HProgress`
- 位置：`src/views/SourcesPage.vue:210`（扫描进度，`type="indeterminate"`）
- 映射：`<h-progress indeterminate />`（HProgress 有 `indeterminate` prop）

### 2. `ion-card` 家族 → `HCard`
- 位置：`src/views/SourcesPage.vue:31-44`（音源卡片）
- 映射：`<h-card>` + `#header`（title + subtitle 组合）+ default（path + 操作按钮）
- title/subtitle 无对应组件，用普通元素 + token 样式组合放入 `#header`

### 3. 文字 `ion-button` → `HButton`
- 位置：`src/views/SourcesPage.vue` 的 4 处 modal "关闭"按钮（68、184、203、240 行）
- 映射：`<h-button variant="..." @click="...">关闭</h-button>`；禁用态用 `:disabled`

### 4. 列表/导航栏 icon-only `ion-button` → `HIconButton`
- HIconButton：`icon`（Component prop）+ `ariaLabel` + `variant`/`size`/`shape`
- 位置（保留原 `slot="end"` 等定位属性到 HIconButton 根元素）：
  - `MiniPlayer.vue:21`（播放/暂停）、`:29`（队列）
  - `PlaylistDetailPage.vue:11`（播放全部）、`:50`（列表项更多，slot=end）
  - `PlaylistsPage.vue:5`（新建歌单）、`:37`（列表项，slot=end）
  - `QueuePage.vue:11`（清空队列，danger）、`:49`（列表项删除，slot=end）
  - `SongsPage.vue:5`（搜索）、`:56`（列表项更多，slot=end）
  - `SourcesPage.vue:5`（添加音源）
- 图标从 slot 子元素改为 `:icon` prop；`color="danger"` → `variant="danger"`（或 danger-soft，取视觉一致）

## Out of Scope（明确不动）
- PlayerPage 所有 icon 控件（沉浸深色态 + `variant=fill` 图标 + `is-active`，HIconButton 不覆盖）→ 保留，登记缺口
- PlayerPage `ion-range` → 交子任务 `07-24-replace-player-range`
- `ion-item`/`ion-label`/`ion-note`/`ion-text`/`ion-list`/叠层宿主 → 保留

## Acceptance Criteria

- [x] SourcesPage 进度条→`HProgress`、音源卡片→`HCard`、4 个 modal 关闭按钮→`HButton`、添加音源按钮→`HIconButton` 全部替换。
- [x] PlaylistsPage、PlaylistDetailPage、SongsPage 的列表/导航栏 icon-only 按钮替换为 `HIconButton variant="ghost"`。
- [x] 图标改为 `:icon` prop 传入；`slot="end"` 布局定位属性透传可用（单根组件默认 attr fallthrough，与既有 `m-cover slot="start"` 一致）。
- [x] 危险操作（删除音源用 `danger-soft`）保持 danger 视觉；禁用态（保存中、空列表）保持。
- [x] `npm run build` 通过、无类型错误；337 unit test 全过；lint 干净。
- [x] 无新增硬编码主色/圆角（全部走 HButton/HIconButton variant 与 token）。

## 实际结果偏差（保留 Ionic 的缺口）

按 Notes 规则，以下两处 `HIconButton` 无法零回归对齐，保留 `ion-button` 并登记到父任务 `gaps.md`：

- **MiniPlayer 播放/暂停/队列按钮**：播放/暂停图标用 `variant="fill"`，而 `HIconButton` 内部只透传 `icon`+`size`、不透传图标 variant（图标恒为 stroke），会丢失填充视觉；队列按钮为与相邻按钮保持一致一并保留。
- **QueuePage 清空队列 / 列表项删除按钮**：原 `fill="clear" color="danger"`（透明底 + danger 图标），`HIconButton` 无 "ghost + danger" 组合，`danger`/`danger-soft` 均带底色，无法零回归。

## Notes

- 若某处 `HIconButton` 视觉/定位无法与原 `ion-button` 对齐（如 slot 透传失败），记录到父任务缺口，保留该处 Ionic 实现，不强行替换。
