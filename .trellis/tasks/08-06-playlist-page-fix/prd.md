# 修复歌单页面问题

## Goal

修复歌单页下方意外出现「确定删除该歌单」提示的问题。

## Background（已确认事实，代码证据）

### 根因（已实证，非猜测）

**PlaylistsPage.vue 的组件导入缺少 `HBottomSheet`、`HDialog`、`HInput`**：

```ts
// 当前（错误）：只导入了用到的部分组件
import { HButton, HEmpty, HIcon, HNavBar, MCover } from '@/components/ui'
```

模板中使用了 `<h-bottom-sheet>`、`<h-dialog>`、`<h-input>`，但项目**没有全局注册**（main.ts 只 `use(router)`），这些标签未被解析为 Vue 组件，被 Vue 当作**原生自定义元素**渲染：

- `<h-dialog modelvalue="false" title="删除歌单">…子内容…</h-dialog>` 变成普通 HTML 元素
- **子内容无条件渲染在文档流中**（v-model 变成普通属性 `modelvalue`，不控制显示）
- 没有 `.h-popup` 弹层结构、没有 teleport 到 body、没有样式

### 现象与根因的对应关系

| 用户现象 | 根因解释 |
|---|---|
| 「一进入歌单页就有」 | 组件挂载即渲染，无需点击 |
| 「没有点击」 | v-model 不生效，内容无条件显示 |
| 「在列表下面」 | 渲染在 `.m-content` 文档流中（模板位置：列表之后） |
| 「没有完整的对话框」 | 没有 `.h-popup` 弹窗结构，只有裸文本+按钮 |
| 「确定删除该歌单」 | deleteMessage 的 fallback 文案（`activePlaylistId=null` 时显示「该歌单」） |

### 实证过程

1. 用户确认：提示在「列表下面」、没有完整对话框、没点击就出现
2. happy-dom 测试：h-dialog 组件本身行为正常（v-model=false 不渲染、打开渲染到 body、关闭移除）
3. **Android 模拟器 + WebView CDP 远程调试复现**：
   - 歌单页 body 文本包含「确定删除「该歌单」？此操作不可撤销。」，但**不在任何 `.h-popup` 内**
   - DOM 链：`P → H-DIALOG → DIV.m-content → DIV.m-page`（teleport 未生效）
   - `<h-dialog modelvalue="false" …>` 是原生自定义元素（非 Vue 组件）
4. 对比正常页面：SongsPage 完整导入了 `HBottomSheet, HButton, HDialog, HEmpty, HFloatingBubble, HIcon, HInput, HNavBar, MCover`
5. 全页面扫描：仅 PlaylistsPage 缺失（PlayerPage 为多行导入，完整；其余页面均完整）
6. 回归来源：c7dc92b（脱离 Ionic 迁移）引入 h-dialog/h-bottom-sheet 时即未补导入，且当时无全局注册——**该 bug 从迁移起一直存在**

### 为什么 lint/build 未拦截

- vue-tsc 对包含连字符的未知标签默认视为自定义元素（Custom Element），不报错
- ESLint `flat/essential` 配置未启用 `vue/no-undef-components` 规则

## Requirements

- R1（核心修复）：PlaylistsPage 补全缺失组件导入（`HBottomSheet`、`HDialog`、`HInput`），恢复弹层组件正常渲染（teleport + v-model 控制）。
- R2（防回归）：启用 `vue/no-undef-components` 规则，防止模板使用未导入组件再次发生。
- R3（顺带）：PlaylistsPage 列表底部补 padding（防 MiniPlayer 遮挡），与 SongsPage 一致。

## Acceptance Criteria

- [ ] AC1：歌单页不再出现「确定删除该歌单」裸文本（v-model=false 时 dialog 内容不渲染）。
- [ ] AC2：歌单操作 sheet、新建/重命名 dialog、删除确认 dialog 恢复为正常弹层（teleport 到 body、居中显示、有遮罩）。
- [ ] AC3（若 R2 纳入）：`npm run lint` 能检出「模板使用未导入组件」类错误（验证规则有效且不误报）。
- [ ] AC4（若 R3 纳入）：歌单列表滚动到底最后一行不被 MiniPlayer 遮挡。
- [ ] AC5：`npm run lint` 与 `npm run build` 通过。

## Out of Scope

- 其他页面（已扫描确认无同类问题）。
- 组件库本身的行为（已验证正常）。

## Open Questions

- 无（根因已实证）。
