# Design：升级 happier-ui 0.0.7 完成 edge-to-edge

## 目标架构

```text
升级前（0.0.6 + 宿主 workaround）
  .h-nav-bar--safe-area { padding-top: env(...) }   ← 库：只读 env，Android WebView<140 不可靠
  src/theme/tailwind.css:62  覆盖为 var(--safe-area-inset-top, env(...))  ← 宿主 workaround

升级后（0.0.7 正式支持）
  .h-nav-bar--safe-area { padding-top: var(--safe-area-inset-top, env(..., 0px)) }  ← 库 c468411
  .h-tab-bar--safe-area  { padding-bottom: var(--safe-area-inset-bottom, env(..., 0px)) }
  src/theme/tailwind.css  移除 .h-nav-bar--safe-area 覆盖  ← 宿主 workaround 删除
```

## 变更文件

| 文件 | 变更 | 风险 |
|------|------|------|
| `package.json` | happier-ui `0.0.6` → `0.0.7` | 低 |
| `package-lock.json` | npm install 自动更新 | 低 |
| `src/theme/tailwind.css` | 删除 `.h-nav-bar--safe-area` 覆盖块（62-69 行附近） | 中：确认无其他依赖该选择器 |
| `node_modules` | 重新安装 | 低 |
| `android/` | `npx cap sync` 更新资产 | 低 |

## 兼容性分析（0.0.6 → 0.0.7 重构组件）

### HBottomSheet（SourcesPage 4 处）→ HPopup 薄包装

- 0.0.7 的 `HBottomSheet.vue` 是 `HPopup(position="bottom")` 薄包装，**props/emits 保持兼容**：`modelValue` / `closeOnOverlay` / `showHandle` / `title` / `ariaLabel` / `teleport`；emits `update:modelValue` / `close`。
- Muses 用法：`v-model`、`title`、`show-handle`、`@close` —— 全部在兼容范围内。
- 新增默认 `lockScroll: true`：Muses 已有 body 滚动锁（`muses-overlay-open`），需验证两者不冲突（叠加锁是幂等的，均为 `overflow:hidden`）。
- BEM 类从 `.h-bottom-sheet__*` 变为 `.h-popup__*`：**Muses 未依赖任何浮层内部类**（grep 确认），安全。
- 样式变量：`.h-popup` 复用 `--h-bottom-sheet-*` token 兜底（见 popup.css），视觉不变。

### HDialog（Songs/Playlists 3 处）→ HPopup 薄包装

- 0.0.7 `HDialog.vue` 同样兼容 `modelValue` / `closeOnOverlay` / `closeOnEsc` / `title` / `description` / `ariaLabel` / `teleport`。
- `#actions` slot → `#footer`：Muses 用的是 `#actions`？需核实。若 Muses 用默认 slot 而非 actions slot，则无需改动。
- Muses 用法：`v-model`、`title`、正文默认 slot —— 兼容。

### HSelect

- Muses 未使用 HSelect（grep 0 处），升级不影响。跳过验证。

### HButton

- 仅新增 PC hover 态（`:hover` 背景），视觉略有变化但属增强；Muses 大量使用 HButton，需构建后抽查颜色。

### 其他

- HScrollbar/HTooltip 为新组件，Muses 未用。
- tokens.css 变化 58 行：需确认 `--muses-*` 别名仍映射（组件库声明不破坏）。

## edge-to-edge 验证点

| 项目 | 期望 |
|------|------|
| Navbar 顶部 | 背景铺到状态栏后，标题/按钮避让（`--safe-area-inset-top` 生效） |
| TabBar 底部 | 导航条背景铺到屏幕底，图标避让（`--safe-area-inset-bottom`） |
| 沉浸播放器 | 全屏铺满，状态栏图标样式切换仍正常 |
| Queue overlay | 不受影响 |
| 桌面浏览器 | inset 0，无变化 |

## 回滚

- `git checkout package.json package-lock.json src/theme/tailwind.css` 恢复 0.0.6 + workaround。
- 无需数据迁移。

## 验证命令

```bash
npm run lint
npm run build
npx cap sync android
rg -n "SystemBars|insetsHandling" android/app/src/main/assets/capacitor.config.json
rg -n "h-nav-bar--safe-area" src/theme/tailwind.css   # 期望无输出（workaround 已删）
```
