# PRD：调整设置页面样式

## 目标

提升设置页的视觉丰富度：列表项加图标、强化分组层次，解决当前页面过于单调的问题。不改任何功能逻辑。

## 现状事实（代码勘察）

- 页面结构（`src/views/SettingsPage.vue`）：`m-navbar` + `m-content` 内两组内容——「关于」（Muses 版本、检查更新）与「音频」（音量均衡开关），均用 `m-block-title` + `m-list inset` + `m-list-item`
- `MListItem` 已有 `media` 左侧缩略图插槽（`src/components/ui/MListItem.vue:10`），当前设置页未使用
- `@lucide/vue@^1.31.0` 已是项目依赖（PlayerPage/MiniPlayer 在用）；业务图标约定为 lucide 组件直渲染
- 功能逻辑：检查更新（GitHub Releases API）、音量均衡开关——本次不动

## 需求

1. 三个列表项左侧加图标容器（`media` 插槽）：统一尺寸的圆角容器 + 主色调浅底 + lucide 线条图标，明暗主题均适配
   - 「Muses 版本」→ 信息类图标（如 `Info`）
   - 「检查更新」→ 刷新类图标（如 `RefreshCw`）
   - 「音量均衡」→ 音量类图标（如 `Volume2`）
2. 分组标题（`m-block-title`）观感微调：字号/字重/颜色层次更清晰，不脱离现有设计 token
3. 明暗主题下图标容器对比度都成立；平板宽度（≥768px）布局不错位

## 验收标准

1. 设置页三个列表项均显示左侧图标，样式统一（同尺寸/圆角/底色规则）
2. 明暗两种主题下图标与容器可读性正常（真机或浏览器双主题验证）
3. 功能回归：检查更新点击可用、音量均衡开关可用
4. `npm run lint`、`npm run test:unit -- --run`、`npm run build` 全部通过（禁止管道吞退出码）

## 范围外

- 不新增设置分组/条目
- 不改其他页面样式
- 不动 SourcesPage/SongsPage 等页面的既有列表样式

## 约束

- 不改功能逻辑（检查更新、音量均衡）
- 遵循现有 UI 体系：happier-ui + 自建 M* 组件 + lucide 图标直渲染，禁止引入 Ionic
