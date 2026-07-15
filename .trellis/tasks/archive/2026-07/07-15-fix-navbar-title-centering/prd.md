# 修复 navbar 标题未居中

## 目标

修复 GitHub Issue [#30 navbar的标题没有居中](https://github.com/Happier-X/muses/issues/30)：统一确保所有页面顶部 navbar 的普通小标题相对整个 toolbar 水平居中，不受左右操作按钮占位影响。

## 背景

- Issue #30 没有正文、截图或评论，需求以标题和用户补充的“所有页面”为准。
- Ionic 8 的 iOS 模式默认将普通 `ion-title` 绝对定位并居中；Material Design 模式下普通 `ion-title` 位于 toolbar 的 flex 内容区，会被 `slot="start"` / `slot="end"` 按钮挤压，单侧按钮或两侧宽度不同时产生视觉偏移。
- 项目 Android 场景使用 Material Design 模式，因此仅设置 `text-align: center` 不能保证相对整个 toolbar 居中。
- 受影响范围包括主页面、歌单详情、播放队列及 Modal 中所有 `ion-header` 普通标题。
- `size="large"` 是 `ion-content` 中的折叠大标题，应保持 Ionic 默认左对齐和折叠动画，不属于本次居中范围。

## 需求

- 统一居中所有 `ion-header` → `ion-toolbar` 内未设置 `size="large"` 的 `ion-title`，覆盖主页面、详情页、播放队列与 Modal。
- 标题必须相对完整 toolbar 水平中心定位，不因只有单侧按钮或左右按钮宽度不同而偏移。
- 为标题两侧保留对称安全空间；动态长标题应单行省略，不得覆盖返回、搜索、新建、添加、播放、清空或关闭按钮。
- 左右按钮的位置、层级、点击热区、可访问性和业务行为保持不变。
- `size="large"` 折叠标题保持现有左对齐、尺寸与动画行为。
- 移动端、宽屏端、Material Design 与 iOS 模式保持兼容。
- 采用统一全局样式规则，移除或避免继续依赖页面级 `text-align: center` 作为居中机制。

## 验收标准

- [ ] 歌曲、专辑、艺术家、歌单、音源、设置和歌单详情的顶部小标题相对 toolbar 视觉中心居中。
- [ ] 播放队列及扫描设置、扫描进度、添加 WebDAV 等 Modal 的顶部小标题相对 toolbar 视觉中心居中。
- [ ] 仅有右侧按钮时标题不再向左偏移；左右按钮宽度不同或一侧按钮条件隐藏时标题仍居中。
- [ ] 动态长歌单标题单行省略且不遮挡左右操作按钮。
- [ ] 所有 `size="large"` 折叠标题保持左对齐，不受全局居中规则影响。
- [ ] navbar 操作按钮仍可正常点击，图标、文案与行为不变。
- [ ] 相关单元测试、lint、类型检查与生产构建通过。

## 范围外

- 调整底部 TabBar 导航项布局。
- 改变 navbar 文案、按钮图标或点击行为。
- 修改折叠大标题的对齐方式或动画。
- 重构 Ionic 页面容器或路由结构。
