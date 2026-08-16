# PRD：侧边栏改为灰色卡片样式

## 背景

用户第二轮反馈（08-16-sidebar-salt-polish 归档后）：
1. 不需要顶上的三个图标按钮（header 区删除）
2. 菜单图标不需要蓝色（激活态去蓝）
3. 侧边栏改为「卡片」，背景灰色（对齐椒盐抽屉的悬浮圆角卡片观感）
4. 明确说明：**不改交互**（推屏式保留），只改侧边栏视觉

## 椒盐卡片实测规格（MuMu 12.2.0 像素）

- 抽屉为悬浮圆角卡片：左缘离屏 48px（18dp）空隙、右缘与主内容 12px 空隙、四边 1px `#e9e9e9` 描边、顶部大圆角（y96→119 渐窄 ≈ 24dp 圆角）
- 卡片背景 `#f9f9f9`（= --m-surface-1）
- 主内容不移动（椒盐为覆盖式；Muses 保持推屏，视觉上卡片做在 50vw 槽位内）

## 需求

仅 `src/views/TabsPage.vue`：

1. **删除 header 区**（`.tabs-layout__panel-header` 及三个按钮：✕/主题/⚙️；drawer 与平板 aside 都删）
2. **图标去蓝**：`.tabs-layout__nav-icon` 恒定 `--m-text-2`（灰色），删除 `--active` 蓝色覆盖；激活态仅保留文字加粗
3. **抽屉卡片化（移动端 drawer）**：
   - drawer 槽位改透明（去 surface-1 全铺、去 border-right）→ 卡片由 `.tabs-layout__panel` 呈现
   - panel：`margin: 0 12px`（左 18px 空隙 + 右 12px 空隙对齐椒盐实测；顶部从安全区下直接起、底部 12px+safe 保留）、`background: var(--m-surface-1)`（灰色）、`border-radius: 24px`、`border: 1px solid var(--m-hairline)`（1px 描边）、轻阴影 `0 8px 24px rgba(0,0,0,0.08)`（深色自动弱化）
4. **平板 aside 保持现状**（常驻侧栏，不做悬浮卡片；用户反馈针对移动端抽屉观感）
5. 深色模式：卡片背景自动 `#262626`、描边/阴影正常

## 验收

- [ ] MuMu 真机：抽屉打开为灰色圆角卡片（描边 + 空隙 + 阴影），无顶部按钮
- [ ] 菜单图标恒灰色（激活不蓝），激活态仅文字加粗
- [ ] 推屏交互/手势/Escape/焦点/inert 无回归
- [ ] 深色模式正常
- [ ] 平板 aside 无回归
- [ ] lint / type / build 通过

## 回滚

单文件样式改动，git revert 即可。