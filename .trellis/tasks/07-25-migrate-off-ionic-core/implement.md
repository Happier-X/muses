# 执行计划

## 执行策略

单任务一把梭，顺序执行 18 步，最后统一验证。编辑顺序确保「先迁完所有 `ion-*` 组件 → 再拔根」。

## 步骤清单

### 第 1 步：升级 happier-ui 0.0.3 → 0.0.6 + HIconButton 迁移

**文件**：`package.json` + `src/components/ui/index.ts` + 所有引用 HIconButton 的文件

**操作**：
1. `package.json` 中 `"happier-ui": "0.0.3"` → `"happier-ui": "0.0.6"`
2. `npm install`
3. `src/components/ui/index.ts` 移除 `HIconButton` 导出
4. 全局替换 `HIconButton` → `HButton`，追加 `is-icon-only` + `shape` + `variant="ghost"` 按原语义
5. 确认 `happier-ui` 新版本依赖安装成功

**HIconButton 使用位置**：
| 文件 | 当前用法 | 替换为 |
|------|---------|--------|
| PlaylistDetailPage.vue | `<h-icon-button :icon="playOutline" ariaLabel="播放全部" variant="ghost">` | `<h-button variant="ghost" is-icon-only shape="square" aria-label="播放全部">` |
| PlaylistDetailPage.vue | `<h-icon-button :icon="removeCircleOutline" variant="ghost" ariaLabel="从歌单移除..."` | 同上 |
| SongsPage.vue | `<h-icon-button :icon="searchOutline" ariaLabel="搜索歌曲" variant="ghost">` | 同上 |
| SongsPage.vue | `<h-icon-button :icon="ellipsisVertical" ariaLabel="更多歌曲操作" variant="ghost">` | 同上 |
| PlaylistsPage.vue | `<h-icon-button :icon="addOutline" ariaLabel="新建歌单" variant="ghost">` | 同上 |
| PlaylistsPage.vue | `<h-icon-button :icon="ellipsisVertical" ariaLabel="更多歌单操作" variant="ghost">` | 同上 |
| SourcesPage.vue | `<h-icon-button :icon="add" ariaLabel="添加音源" variant="ghost">` | 同上 |
| TabsPage 及更多 | （已全部排查，共 8 处 HIconButton） | 同模式 |

---

### 第 2 步：迁移 ion-button / ion-range / ion-fab

**文件**：MiniPlayer.vue、PlayerPage.vue、QueuePage.vue、SongsPage.vue

**MiniPlayer.vue**：
```diff
-import { IonButton } from '@ionic/vue'
+import { HButton } from '@/components/ui' // 或从 happier-ui

-<ion-button fill="clear" aria-label="暂停播放" @click.stop="togglePlayback">
-  <h-icon :icon="isPlaying ? pause : play" variant="fill" />
-</ion-button>
+<h-button variant="ghost" is-icon-only shape="circle" aria-label="暂停播放" @click.stop="togglePlayback">
+  <h-icon :icon="isPlaying ? pause : play" variant="fill" />
+</h-button>
```

**PlayerPage.vue**：8 处 `ion-button` 全按 design §9.4 替换。`ion-range` 按 §9.4 替换。

**QueuePage.vue**：2 处 `ion-button`（清空用 `variant="danger-soft"`、移除用 `variant="danger-soft" is-icon-only`）

---

### 第 3 步：迁移 ion-item / ion-label / ion-list

**文件**：PlaylistDetailPage.vue、PlaylistsPage.vue、QueuePage.vue、SettingsPage.vue、SongsPage.vue、SourcesPage.vue、TabsPage.vue

按 design §9.6 逐一替换：
- 虚拟列表行 → 原生 div（保持现有类名 `.song-item`/`.queue-item`/`.playlist-row`）
- SettingsPage → 原生结构或 HCellGroup（每项包含 H2+P 描述 + HSwitch）
- TabsPage 侧栏 → `<nav>` + `<RouterLink>`
- SourcesPage 扫描进度列表 → 原生 div
- PlaylistsPage 列表 → 原生 div

---

### 第 4 步：迁移 ion-note

**文件**：QueuePage.vue、SourcesPage.vue（×4）

替换为 `<span class="xxx-note">` + `color: var(--h-color-ink-muted)`。见 design §9.7。

---

### 第 5 步：迁移 ion-action-sheet / ion-alert

**文件**：PlaylistsPage.vue、SongsPage.vue、SourcesPage.vue

- PlaylistsPage `ion-action-sheet` → `<h-bottom-sheet>`（歌单操作 3 项）
- PlaylistsPage `ion-alert` ×2 → `<h-dialog>`（新建/重命名 + 删除确认）
- SongsPage `ion-action-sheet` ×2 → `<h-bottom-sheet>`（歌曲操作 + 选歌单）
- SongsPage `ion-alert` → `<h-dialog>`（新建歌单）
- SourcesPage `ion-alert` → `<h-dialog>`（删除确认）

---

### 第 6 步：迁移 ion-modal → HBottomSheet

**文件**：SourcesPage.vue（4 个 modal）

- 编辑音源 → HBottomSheet + 表单（`closeOnOverlay` 在保存中 = `false`）
- 扫描设置 → HBottomSheet + HSwitch + HButton
- 扫描进度 → HBottomSheet + 统计列表（`closeOnOverlay` 在处理中 = `false`）
- WebDAV 目录浏览 → HBottomSheet

---

### 第 7 步：建 MPage / MContent 自建骨架

**文件**：新建/修改 `src/components/ui/MPage.vue`、新建 `src/components/ui/MContent.vue`

**MPage.vue** 替换模板内容（详见 design §2.1）
**MContent.vue** 新建（详见 design §2.2）
**src/components/ui/index.ts** 添加 `MContent` 导出

---

### 第 8 步：替换 App.vue 骨架

**文件**：`src/App.vue`

- `<ion-app>` → `<div class="app-shell">`
- `<ion-router-outlet>` → `<div class="app-router-view"><RouterView /></div>`
- 删除 `IonApp`/`IonRouterOutlet` 导入
- 更新 scoped 样式和全局样式选择器（见 design §3）

---

### 第 9 步：改 router/index.ts 导入

**文件**：`src/router/index.ts`

```diff
-import { createRouter } from '@ionic/vue-router'
+import { createRouter } from 'vue-router'
```

---

### 第 10 步：替换 useIonRouter

**文件**：`src/views/PlaylistDetailPage.vue`

```diff
-import { useIonRouter } from '@ionic/vue'
-const ionRouter = useIonRouter()
+import { useRouter } from 'vue-router'
+const router = useRouter()
```

`goBack` 实现见 design §6。

---

### 第 11 步：替换 onIonViewWillEnter

**文件**：AlbumsPage.vue、ArtistsPage.vue、PlaylistDetailPage.vue、PlaylistsPage.vue、SongsPage.vue

- 5 处删除 `import { onIonViewWillEnter } from '@ionic/vue'`
- 5 处删除 `onIonViewWillEnter(fn)` 调用
- PlaylistDetailPage 添加 `watch(playlistId, refresh)` 处理 param 变化

---

### 第 12 步：safe-area 替换

**文件**：MiniPlayer.vue、PlayerPage.vue、QueuePage.vue、SongsPage.vue、TabsPage.vue（共 16 处）

全部 `var(--ion-safe-area-*)` → `env(safe-area-inset-*)`。不保留 fallback 值 0px（env 本身有 fallback）。

---

### 第 13 步：颜色变量清理

**文件**：`src/theme/variables.css`、SourcesPage.vue、TabsPage.vue

- `variables.css`：删除整个文件（或清空内容，保留下次扩展可能）。确认 `main.ts` 不再 import。
- SourcesPage.vue：4 处 `--ion-color-medium` → `--h-color-ink-muted`
- TabsPage.vue：`--ion-color-primary` → `--h-color-primary`、`--ion-background-color` → `--h-color-surface`

---

### 第 14 步：移 Ionic 插件 + 全局 CSS

**文件**：`src/main.ts`

删除 1 个插件导入 + 12 个 CSS 导入 + 1 个暗色 CSS 导入。已删后补齐必要的 body 字体/颜色（见 design §10.2）。

移除 `app.use(IonicVue)`。

---

### 第 15 步：Overlay 锁滚动选择器适配

**文件**：`src/App.vue`

```diff
-body.muses-overlay-open ion-router-outlet ion-content {
+body.muses-overlay-open .app-router-view {
```

---

### 第 16 步：package.json 依赖清理

**文件**：`package.json`

- 删除 `"@ionic/vue": "^8.8.14"`
- 删除 `"@ionic/vue-router": "^8.8.14"`
- 删除 `"ionicons": "^8.0.13"`
- `"happier-ui": "0.0.6"`（已配，无需改）
- `npm install` 验证

---

### 第 17 步：全局残留检查

```bash
rg "@ionic/" src/          # 预期：零命中
rg "<ion-" src/             # 预期：零命中
rg "from 'ionicons'" src/  # 预期：零命中
rg "IonicVue\|IonApp\|IonRouterOutlet\|IonPage\|IonContent\|IonButton\|IonItem\|IonLabel\|IonList\|IonNote\|IonRange\|IonFab\|IonFabButton\|IonActionSheet\|IonAlert\|IonModal" src/  # 预期：零命中
rg "var\(--ion-" src/      # 预期：零命中
```

---

### 第 18 步：验证

```bash
# 类型检查
npx vue-tsc --noEmit

# 构建
npm run build

# 单元测试
npm run test:unit

# e2e（如配）
npm run test:e2e

# 手动启动验收
npm run dev
```

手动验收 checklist（对应 AC5-AC9）：
- 应用启动无报错、Tab 切换正常
- PlaylistsPage/SongsPage 浮层功能正常
- SourcesPage 4 个 HBottomSheet 正常；编辑保存/扫描进度条件关闭
- SongsPage FAB 跳转当前歌曲
- 深色模式正常、主色正常
- safe-area 裁切测试（DevTools 模拟刘海屏）
- 返回键/状态栏回归验证

---

## 回滚方案

如果某步骤导致编译错误且 5 分钟内无法修复：

```bash
git checkout -- src/      # 恢复所有 src 文件
git checkout -- package.json
```

然后重新从失败步骤开始。18 个步骤中步骤 7–14 有结构性依赖，回滚后必须按顺序重做。

## 已知风险

| 风险点 | 缓解 |
|--------|------|
| Step 5 浮层事件处理逻辑（`@didDismiss` vs `v-model`）需逐文件确认 | 每个文件单独检查后改，不改全局 |
| Step 13 删除 variables.css 后主色异常 | 提前搜索 `var(--ion-color-primary)` 确认已全部替换 |
| Step 14 删除 Ionic 全局 CSS 后 layout 异常（滚动/溢出） | 提前在 tailwind.css 中补齐 body 字体/颜色 |
