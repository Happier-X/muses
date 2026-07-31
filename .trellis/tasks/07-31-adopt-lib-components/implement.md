# Implement：用组件库 0.0.7 组件替换宿主自建实现

## 执行清单

1. **加载规范与调研**
   - 阅读 `.trellis/spec/frontend/component-guidelines.md`（happier-ui 接入契约、浮层组件使用约定）。
   - 阅读 `design.md` 的 HPopup 集成细节。

2. **删除死代码 ExploreContainer**
   ```bash
   git rm src/components/ExploreContainer.vue
   ```
   确认 `src/components/ui/index.ts` 无导出、src 无引用。

3. **QueuePage 迁移 HPopup fullscreen**
   - 编辑 `src/views/QueuePage.vue`：
     - 外层 `<div class="fixed inset-0 z-[1200] …">` → `<h-popup v-model="queueOverlayVisible" position="fullscreen" :close-on-overlay="false" :close-on-esc="false">`
     - 内容移入默认 slot，列表容器保持 `h-full overflow-auto`（HPopup panel overflow:auto 需内容约束）
     - import 增加 `HPopup`
   - `queueOverlayVisible` 直接作 v-model。

4. **App.vue 调整**
   - 移除 `<Transition>` 包裹 QueuePage（HPopup 自带转场）。
   - 保留 `hasGlobalOverlay` / `syncBodyOverlayLock`（PlayerPage 用）、`keepPlayerPageMounted`、backButton 逻辑。

5. **构建验证**
   ```bash
   npm run lint
   npm run build
   ```

6. **回归抽查**
   - QueuePage 打开/关闭/清空/选歌/删除/返回。
   - 下滑关闭手势可用。
   - PlayerPage → QueuePage → 返回 PlayerPage 路径的滚动锁无残留。
   - PlayerPage 保活（#22）与手势行为不回归。

7. **更新 spec**
   - component-guidelines.md 记录：QueuePage 已迁移 HPopup fullscreen；PlayerPage 因保活/手势保留宿主实现，迁移需组件库 keepAlive + 手势开关。

## 验证命令

```bash
npm run lint
npm run build
rg -n "ExploreContainer" src                       # 期望无输出
rg -n "muses-overlay-open" src/theme/tailwind.css  # 保留（PlayerPage 用）
```

## 风险文件

| 文件 | 风险 |
|------|------|
| `src/views/QueuePage.vue` | HPopup 结构重构；虚拟列表滚动容器约束 |
| `src/App.vue` | Transition 移除；滚动锁语义变化 |
| `src/theme/tailwind.css` | `.m-content` 锁保留正确性 |

## 回滚点

- `git checkout src/views/QueuePage.vue src/App.vue`；`git restore --staged src/components/ExploreContainer.vue && git checkout -- src/components/ExploreContainer.vue`。

## Review Gates

- lint/build 全绿。
- 滚动锁三种场景（仅 Queue / Player+Queue / Queue+Player）无残留。
- PlayerPage 不回归（AC7）。
