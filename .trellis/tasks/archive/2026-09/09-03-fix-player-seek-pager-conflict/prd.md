# PRD：修复沉浸式播放页进度条拖动与左右滑动冲突

## 背景

沉浸式播放页（`PlayerScreen.kt`）手机布局用 `HorizontalPager` 实现 info/lyric 双面板左右滑动；
进度条（`ProgressSection` 自绘细轨）需要支持点击 seek + 拖动预览 seek。
用户反馈：在进度条上拖动时，页面跟着左右切面板，无法正常拖动 seek。

## 根因

`ProgressSection` 的手势代码存在**死代码 bug**：

```kotlin
.pointerInput(canSeek, max) {
    detectTapGestures { ... }   // 内部 awaitEachGesture 无限循环，永不返回
    detectDragGestures( ... )   // 永远执行不到
}
```

`detectTapGestures` 永不返回，后面的 `detectDragGestures` 从未生效。
后果：进度条上的水平拖动手势无人消费，冒泡给父级 `HorizontalPager`，
被当成切页滑动处理 —— 表现为"拖进度条时页面左右滑"。
（附带：平板底部条的拖动 seek 同样是坏的，只有点击有效。）

## 需求

1. 进度条拖动恢复可用：按下→跟手预览（时间数字跟随）→松手 commit seek。
2. 拖动进度条时页面不得左右切面板（手势冲突消除）。
3. 点击进度条直接 seek 的原有行为保留。
4. 非进度条区域左右滑切面板行为不变；下滑关闭、下滑手势分流等不受影响。
5. 平板 `TabletBottomBar` 的进度条同样恢复拖动可用。

## 方案要点（实现时遵循）

- `ProgressSection` 内改用单一 `awaitEachGesture` 循环同时处理 tap + 水平拖动，
  跨过 touch-slop 即 `consume`，阻止父级抢手势。
- 新增 `onSeekDragActive: (Boolean) -> Unit` 回调（默认空实现）：
  进度条上有按下手势期间通知上层；`PhoneImmersiveLayout` 以此控制
  `HorizontalPager(userScrollEnabled = !seekDragging)`，硬保证不冲突。
- 调用链：`PhoneImmersiveLayout → InfoPanel → ProgressSection`；
  `TabletBottomBar → ProgressSection` 用默认回调（无 pager，无需禁用）。
- 不改变现有 `onSeekStart / onSeekEnd` 语义与调用时机。

## 验收标准

- [ ] 手机沉浸式页：进度条上左右拖动，进度跟手预览、松手 seek 到目标位置，页面不切面板
- [ ] 手机沉浸式页：点击进度条仍可直接 seek
- [ ] 手机沉浸式页：其它区域左右滑仍可切换 info/lyric 面板
- [ ] 平板横屏底部条：拖动/点击 seek 均正常
- [ ] `./gradlew :app:assembleMusesDebug` 编译通过

## 非目标

- 不改动歌词面板、封面、下滑关闭、播放控制等其它手势
- 不引入 Material3 Slider（保持自绘细轨 1:1 复刻）
- 不做 UI 视觉改动
