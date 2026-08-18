# 修复侧边栏首次打开菜单项黄圈（焦点环）

## Goal

侧边栏抽屉打开时自动 focus 第一个菜单项，Chrome/Android WebView 会给刚获得焦点的链接渲染默认聚焦环（用户感知为黄圈）。修复：侧边栏导航链接（移动端抽屉 + 平板 aside）去掉浏览器默认焦点轮廓，同时保留键盘导航（Tab 键）可感知的焦点样式，不破坏无障碍。

## Requirements

- 触发场景：冷启动/首次打开 app → 打开侧边栏抽屉 → 「歌曲」菜单项周围出现黄圈（焦点环）。
- 根因：`TabsPage.vue` 的 `openDrawer()` / 抽屉手势完成路径会在 nextTick 调用 `drawerLinkRefs.value[0]?.$el?.focus()`（抽屉焦点陷阱），而 `.tabs-layout__nav-link` 未设置 `outline: none`，与项目内其他交互组件（MButton/MFab/MIconButton 等）的惯例不一致。
- 修复范围（保持一致口径）：
  - `.tabs-layout__nav-link` 及 `.tabs-layout__drawer-link`（复用同一选择器）隐藏默认焦点轮廓（outline: none）。
  - 保留 `:focus-visible` 样式：键盘 / 无障碍导航（屏幕阅读器）仍能看到焦点指示。移动端触摸聚焦通过 JS focus() 触发、不算 focus-visible，不受影响。
  - 鼠标/触摸点击选中项时不应出现任何可见焦点环（点击态不变）。
- 不改动抽屉的焦点管理逻辑（focus 陷阱仍有效），只处理视觉。
- 与项目惯例对齐：交互组件统一 `outline: none` + `-webkit-tap-highlight-color: transparent` 的既有模式（详见 src/components/ui 相关组件与 src/theme/index.scss:284）。

## Acceptance Criteria

- [ ] 冷启动后第一次打开侧边栏，第一个菜单项（歌曲）周围不再出现黄圈。
- [ ] 随意切换菜单、关闭再打开抽屉，任何菜单项都不出现焦点环（触摸/点击场景）。
- [ ] 键盘 Tab 导航聚焦菜单项时仍可见焦点指示（:focus-visible 生效）。
- [ ] 平板（≥768px）常驻侧栏链接同样不出现默认焦点环，键盘焦点样式保留。
- [ ] 现有测试/构建不破坏（vue-tsc + vite build 通过）。

## Notes

- 轻量任务，PRD-only，无需 design.md / implement.md。
- 焦点样式选择器归属：`src/views/TabsPage.vue` 的 `<style scoped lang="scss">` 内 `.tabs-layout__nav-link`。