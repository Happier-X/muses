# PRD：修复目录浏览器行内按钮挤压行内容导致错位

## 背景

用户反馈：WebDAV 目录浏览时页面错位。CDP 实测定位根因：

- `MButton` 默认样式 `width: 100%`（`src/components/ui/MButton.vue`）。
- `WebDavDirectoryBrowser` 目录行结构为「行按钮(`__row-btn`, flex:1) + 动作 m-button(选择/进入)」，m-button 以 100% 宽度参与 flex 分配占约 376px，行按钮被挤成宽度 0，目录名/路径文字竖排堆叠，列表整体错位。
- 该问题自 SourcesPage 内嵌浏览器时期即存在（3903fb9 引入），抽取组件时带入新页面。

## 需求

1. 目录行动作按钮不再撑满整行：在 `WebDavDirectoryBrowser.vue` 行样式中用 `:deep(.m-button)` 约束 `width: auto; flex: 0 0 auto`，优先级需压过 MButton 默认值。
2. 行按钮恢复 `flex: 1` 正常占位，目录名与路径单行省略展示。
3. single / multiple 两种模式均生效；组件外其他使用 MButton 的场景不受影响。

## 验收标准

1. MuMu 实测：编辑页连接浏览后，目录行布局正常（名称+路径横排、动作按钮靠右、行高正常）。
2. 新增模式浏览同样正常。
3. lint / vue-tsc / 单测通过。
