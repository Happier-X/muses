# 设计：#48 沉浸式页隐藏信息补充提示

## 方案

删除 `PlayerPage.vue` 中 `metadataStatus === 'scanning'|'failed'` 的两处 `<small>` 展示。

不改 `controller` 元信息扫描状态机；不新增 toast。

## 测试

- 现有 PlayerPage 挂载用例不依赖上述文案即可；若有断言文案则删除/改写。
