# 技术设计

## 方案

调整 `src/views/PlayerPage.vue` 中 `LyricPlayer` 的 `align-position`，将当前行从现有的约 0.38 可视位置调整至 0.5，使活跃行位于歌词播放器可视区域中心。保留现有真实容器尺寸、翻译副行样式和安全区布局。

## 兼容性

不修改 AMLL 解析、歌词行合并、翻译显隐、点击 seek 或 overlay 手势；仅改变定位参数。

## 验证

运行 lint、build、unit test，并检查模板中仍保留 `align-anchor="center"`、翻译属性及歌词行点击事件。
