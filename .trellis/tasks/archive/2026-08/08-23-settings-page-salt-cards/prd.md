# PRD：设置页列表改为椒盐卡片样式

## 目标

把设置页两组列表改成椒盐音乐（Salt Player）同款卡片观感：灰色页面底上的白色圆角卡片组。

## 现状与方案（代码勘察已定位根因）

- `src/views/SettingsPage.vue` 当前用 `<m-list inset>`——`inset` 只提供左右安全区边距 + `--m-radius-card` 圆角 + overflow hidden，**无背景色**，列表直接透出页面灰底
- `MList` 已内置 `strong` 修饰符（`src/components/ui/MList.vue:59-61`）：`--m-surface-1` 卡片底（Salt subBackground 同源 token：浅 #f9f9f9 / 深 #262626）+ 行间分隔线
- 改法：两处 `<m-list inset>` → `<m-list inset strong>`

## 需求

1. 「关于」「音频」两组列表均改为卡片样式（inset + strong）
2. 上一任务加的图标容器、分组标题层次保持不变
3. 明暗主题下卡片底自动跟随 token（无需额外深色覆盖）

## 验收标准

1. 两组列表呈现 `--m-surface-1` 卡片底 + 圆角，行间有分隔线（椒盐观感）
2. 明暗主题均正常（真机或 CDP 验证）
3. 功能回归：检查更新可点、音量均衡开关可用
4. lint / test:unit / build 全过（禁止管道吞退出码）

## 范围外

- 不改 MList 组件本身
- 不动其他页面

## 约束

- 不改功能逻辑；遵循 Salt token 体系
