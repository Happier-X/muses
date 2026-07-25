# PRD: 音源页 ActionSheet 替换为 HBottomSheet

## 问题

SourcesPage 的「添加音源」菜单使用了 `ion-action-sheet`，而 Ionic 原生组件应逐步替换为 happier-ui 组件。目前底部弹出式菜单应改用 `HBottomSheet`。

## 目标

将 `ion-action-sheet` 替换为 `HBottomSheet`，保持功能不变：
- 添加本地文件夹
- 添加 WebDAV 文件夹
- 取消

## 验收标准

1. 点击右上角添加图标按钮，底部弹出 HBottomSheet，带有手柄和标题
2. 选项列表包含「添加本地文件夹」「添加 WebDAV 文件夹」「取消」
3. 点击各选项触发对应功能
4. 点击遮罩层可关闭
5. 不再引用 `ion-action-sheet` 及相关 Ionic 导入
