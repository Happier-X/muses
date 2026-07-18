# 执行计划

1. 在 `SourcesPage.vue` 的虚拟卡片上接入 TanStack Virtual 的实际元素测量，并补充必要的索引属性。
2. 检查卡片 CSS 的定位、盒模型和窄屏内容溢出，避免测量结果与实际占位不一致。
3. 运行 `npm run lint`、`npm run build`、`npm run test:unit`。
4. 检查 Git diff，确认只涉及 #31 的布局修复。
