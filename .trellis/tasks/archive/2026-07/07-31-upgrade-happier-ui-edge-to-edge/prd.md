# 升级 happier-ui 到 0.0.7 并完成 edge-to-edge

## Goal

将 happier-ui 从 0.0.6 升级到 0.0.7，使组件库正式版的 Capacitor 8 `--safe-area-inset-*` 回退（issue #12）生效，移除宿主临时 CSS 覆盖，完成 Android edge-to-edge 效果，并验证 0.0.7 中重构组件（HBottomSheet/HDialog/HPopup、HSelect、HButton）与 Muses 现有使用兼容。

## Background

### 已确认事实

- Muses 当前 `happier-ui` 锁定 `0.0.6`（精确版本，无 `^`）。
- 0.0.7 已发布，包含：
  - `c468411` fix(nav-bar,tab-bar)：`.h-nav-bar--safe-area` / `.h-tab-bar--safe-area` 改为 `var(--safe-area-inset-top/bottom, env(…, 0px))` 三级回退，**closes #12**。
  - `cda7abc` HPopup 通用浮层 + HBottomSheet/HDialog 薄包装重构 + useScrollLock。
  - `77f5e50` HSelect 升级为 HeroUI Web 风格自定义 popover 面板。
  - `04055aa` popup fullscreen + swipe-to-close。
  - `8d5f679` HTooltip、`d26eeea` HScrollbar 新组件（Muses 未用，不影响）。
  - `01b1503` HButton 补齐 PC hover 交互态。
- Muses 实际使用：HBottomSheet、HDialog、HButton、HCard、HCell、HCellGroup、HCheckbox、HEmpty、HFloatingBubble、HIcon、HImage、HInput、HNavBar、HPagination、HProgress、HRange、HSelect、HSidebar、HSwitch、HTabBar、HTable、HTag、HTextarea、HToast。
- 宿主 `src/theme/tailwind.css:62` 有 `.h-nav-bar--safe-area` 临时覆盖（上轮 workaround），0.0.7 正式支持后应移除。
- `capacitor.config.ts` 已显式 `SystemBars: { insetsHandling: 'css' }`，Android 资产已同步。

### 用户价值

组件库正式能力接管安全区适配，宿主不再持有临时 CSS 覆盖；Android 15/16 Edge-to-Edge 下 Navbar/TabBar 内容避让正确。

## Requirements

1. **R1 升级依赖**：`package.json` 与 `package-lock.json` 中 happier-ui 更新为 `0.0.7`（保持精确版本锁定）。
2. **R2 移除宿主临时覆盖**：删除 `src/theme/tailwind.css` 中 `.h-nav-bar--safe-area` 的临时 workaround，由组件库正式实现接管。
3. **R3 edge-to-edge 效果**：Android 真机/模拟器验证 Navbar 背景铺到状态栏后、内容避让安全区、图标可读；TabBar 底部安全区正常。
4. **R4 重构组件兼容**：HBottomSheet / HDialog / HPopup 重构、HSelect 升级、HButton hover 态变化后，Muses 现有调用（props/slots/events）全部正常，无控制台报错、无样式回归。
5. **R5 构建与类型**：`npm run lint`、`npm run build`（含 vue-tsc）通过。
6. **R6 不引入 file: 链接**：保持 npm 精确版本，不使用 `file:../happier-ui` 或本地 alias。

## Out of Scope

- 升级到 0.0.7 之后的其他未发布版本。
- 迁移 Muses 到 HPopup 新能力（fullscreen/swipe-to-close 等）——除非修复兼容问题所需。
- 修改 happier-ui 源码或发版。
- iOS 原生工程专项改造。

## Acceptance Criteria

- [ ] **AC1** `package.json`/lock 中 happier-ui = `0.0.7`，无 `file:` 链接。
- [ ] **AC2** `src/theme/tailwind.css` 不再含 `.h-nav-bar--safe-area` 覆盖（组件库正式接管）。
- [ ] **AC3** `npm run lint`、`npm run build` 通过；`npx cap sync android` 后 Android 资产含 `SystemBars.insetsHandling: "css"`。
- [ ] **AC4** 打开歌曲/设置/歌单等页（有 HBottomSheet/HDialog 交互的页面）正常：底部面板、对话框打开/关闭无报错、样式正确。
- [ ] **AC5** HSelect（若 Muses 有用到下拉的场景）交互与样式正常。
- [ ] **AC6** Android edge-to-edge 下 Navbar 内容避让正确（依赖真机验证，若无法真机则列出待验项）。
- [ ] **AC7** 0.0.7 修复未引入其他视觉回归（对比 0.0.6 的按钮/卡片/表单等）。

## Key Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| 升级方式 | 精确版本 0.0.7 | 项目既定策略（component-guidelines），避免意外 minor 漂移 |
| 临时覆盖 | 移除 | 库已正式支持，宿主覆盖属于 workaround |
| 重构组件 | 先兼容验证，不主动迁移新能力 | 最小改动，控制风险 |
| 真机验证 | 尽力执行 | edge-to-edge 视觉需真机/模拟器确认 |

## Risks / Deferred Items

- HBottomSheet/HDialog 重构可能改变内部 DOM/样式，需逐页检查浮层（QueuePage 无 sheet，Songs/Playlists 有 sheet/dialog）。
- HSelect 升级为自定义 popover，行为与原生 select 不同，需检查 Muses 是否有 select 使用。
- 若真机不可用，edge-to-edge 视觉验证降级为「构建 + 配置 + 代码审查」清单，标注待真机确认。
