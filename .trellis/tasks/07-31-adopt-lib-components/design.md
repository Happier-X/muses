# Design：用组件库 0.0.7 组件替换宿主自建实现

## 目标架构

```text
迁移前（宿主自建）
  App.vue: <Transition><QueuePage v-if="queueOverlayVisible"/></Transition>   ← 手写 Transition + v-if
  QueuePage.vue: <div class="fixed inset-0 z-[1200]">…</div>                 ← 手写全屏容器
  overlay.ts: queueOverlayVisible ref + open/close                          ← 状态保留

迁移后（HPopup fullscreen）
  QueuePage.vue: <h-popup v-model="queueOverlayVisible" position="fullscreen">…</h-popup>
  App.vue: <QueuePage v-if="queueOverlayVisible"/>（保留 v-if，由 HPopup 管理转场与锁）
  overlay.ts: queueOverlayVisible ref + open/close（不变）
```

## 变更文件

| 文件 | 变更 | 风险 |
|------|------|------|
| `src/views/QueuePage.vue` | 外层 `<div class="fixed inset-0 z-[1200]">` → `<h-popup v-model="queueOverlayVisible" position="fullscreen" lock-scroll :close-on-overlay="false">`；内容移入默认 slot | 中：结构重构 + 滑动关闭行为变化 |
| `src/App.vue` | Transition 包裹 QueuePage 移除（HPopup 自带转场）；`syncBodyOverlayLock` 逻辑调整（仅 PlayerPage 驱动） | 中：滚动锁语义变化 |
| `src/theme/tailwind.css` | `html/body.muses-overlay-open` 规则保留（PlayerPage 用）；`.m-content` 锁保留 | 低 |
| `src/features/player/overlay.ts` | 不变 | — |
| `src/components/ExploreContainer.vue` | 删除文件 | 低（无引用） |
| `src/components/ui/index.ts` | 确认无需改动（HPopup 需要时导入） | 低 |

## HPopup 集成细节

### QueuePage → HPopup fullscreen

```vue
<h-popup
  v-model="queueOverlayVisible"
  position="fullscreen"
  :lock-scroll="true"
  :close-on-overlay="false"
  :close-on-esc="false"
>
  <!-- 原 QueuePage 内容：HNavBar + 虚拟列表 -->
</h-popup>
```

- `queueOverlayVisible` 来自 `overlay.ts`（`ref`），直接 v-model 双向绑定（HPopup emits `update:modelValue`）。
- `:close-on-overlay="false"`：fullscreen 无 overlay 点击需求（overlay 是全屏背景，点击不应关闭——原 QueuePage 无此行为）。
- `:close-on-esc="false"`：桌面 ESC 不应直接关队列？——需确认。原实现无 ESC 处理；保留 false 更贴近原行为，back 按钮/系统返回键关闭。
- `lock-scroll` 默认 true：HPopup 内部 useScrollLock 锁 documentElement。**与宿主 `muses-overlay-open` 锁并存**——QueuePage 打开时宿主锁由 `hasGlobalOverlay`（含 queueOverlayVisible）驱动，两个锁同时生效（inline overflow + class !important，均幂等）。关闭时各自解锁，互不干扰。
- 下滑关闭：HPopup fullscreen 在 panel scrollTop=0 时下滑触发 `requestClose()` → 更新 `queueOverlayVisible=false`。原 QueuePage 无下滑关闭（仅返回按钮），迁移后为**增强行为**（更符合移动端习惯）。

### App.vue 调整

- 移除 `<Transition>` 包裹（HPopup 自带 `h-popup-fullscreen-in/out` 转场）。
- `hasGlobalOverlay` 仍 = `playerOverlayVisible || queueOverlayVisible`，`syncBodyOverlayLock` 保留（PlayerPage 需要 class 锁；QueuePage 关闭时 hasGlobalOverlay 可能仍为 true——PlayerPage 打开状态，锁不释放，正确）。
- `keepPlayerPageMounted` 不变（PlayerPage 保活不动）。
- backButton 监听不变（queue → player → minimize 顺序）。

### 滚动锁并存验证

- 场景 1：仅 Queue 开 → hasGlobalOverlay=true → class 锁生效；HPopup lockScroll 锁 documentElement。关 Queue：hasGlobalOverlay=false → class 解锁；HPopup unlock。**一致**。
- 场景 2：Player 开（class 锁）→ Queue 开（HPopup 锁叠加）。关 Queue：HPopup 解锁，class 锁仍生效（Player 仍开）。关 Player：class 解锁。**正确**。
- 场景 3：Queue 开 → Player 开（Queue 在 Player 之上？实际 App.vue 中 QueuePage 在 PlayerPage 之后渲染，z 更高）。关 Player：HPopup 锁（Queue）仍在。**正确**。

### 潜在问题

- HPopup fullscreen 的 panel `overflow: auto` 默认；QueuePage 虚拟列表需要精确滚动容器。将原列表容器（`listParentRef`）作为 slot 内容即可，HPopup panel 自身可能也滚动——需设置 slot 内容高度 `h-full`，让 panel 不产生额外滚动。面板 `inset:0` + 内容 `h-full overflow-hidden` 可保证。
- HPopup panel 有 `touch-action: pan-y`：QueuePage 虚拟列表是原生滚动，兼容。
- Teleport 默认 `body`：QueuePage 内容挂到 body，`role="dialog"` + `aria-modal` 语义由 HPopup 提供；原 QueuePage 无 aria-modal。可接受（增强无障碍）。

## 删除 ExploreContainer

- `src/components/ExploreContainer.vue` 无任何 import 引用（grep 确认），直接删除。
- 确认 `src/components/ui/index.ts` 不导出它（当前只导出 M 系列 + H 系列）。

## 回滚

- `git checkout src/views/QueuePage.vue src/App.vue src/theme/tailwind.css` 恢复；重新 `git add src/components/ExploreContainer.vue` 找回死代码文件。

## 验证命令

```bash
npm run lint
npm run build
rg -n "ExploreContainer" src   # 期望无输出
rg -n "muses-overlay-open" src/theme/tailwind.css  # 保留（PlayerPage 用）
```
