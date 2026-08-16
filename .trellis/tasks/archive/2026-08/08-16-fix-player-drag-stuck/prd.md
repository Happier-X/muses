# PRD：修复沉浸式播放页下滑收起回弹卡住

## 背景

真机（Android WebView / 移动端浏览器）上，沉浸式播放页（PlayerPage）下滑收起时：

- 下滑距离**小于收起阈值**（当前 `max(96, min(160, innerHeight*0.18))`）时，预期松手后页面平滑回弹回全屏顶部；
- 实际表现为：只下滑一点距离后松手，页面**卡在半屏不回弹**，无法归位。

## 问题描述（用户原话）

> 在真机上，沉浸式播放页面，在下滑收起的时候，如果只下滑一点距离，会导致页面卡住，不回弹到整个页面上

## 根因分析（调研结论）

回弹完全依赖 `watch(dragOffsetY)` 的**一次性机会**驱动，存在多条会被静默吞掉的路径，且无任何兜底：

1. **watch 早退竞态**（`PlayerPage.vue` 1746 行附近）：

   ```ts
   watch(dragOffsetY, (next, prev) => {
     if (reboundControls || !dragLayerRef.value) return   // ← 回弹动画进行中时吞掉新回弹
     const from = prev ?? 0
     if (from <= 0 || next !== 0) return
     ...
   })
   ```

   - 回弹动画 `duration: 0.22s` 未结束时再次松手 → 新回弹请求被吞；
   - 若此刻旧动画又被 `stopRebound()` 中途 kill：motion `stop()` = `commitStyles()`（把**当前中间值**写进 `el.style.transform`）+ `cancel()`（**不触发** `onComplete`）→ `reboundControls` 虽被置 null，但中间值 transform 已残留，之后没有任何代码再纠正它 → **视觉卡在半屏**。

2. **触摸序列被打断**：真机上下拉通知栏、多指触控、系统手势、低端机事件丢失时，`touchend / touchcancel` 可能不来或错乱 → `dragOffsetY` 残留在下滑中间值，无超时/兜底拉回 0。

3. **动画元素与绑定元素分离**：拖拽位移绑定在**内层** drag-layer（`:style`），回弹动画却作用在**外层** `dragLayerRef`。松手瞬间 Vue 先把内层置 0、watch 再把外层锁回 `from` 再动画，产生一帧视觉抖动，且链路隐晦。

## 需求

1. 松手后页面**必须**回到全屏顶部（`translateY(0)`），无论下滑距离、操作速度、连点乱序、触摸序列是否被系统打断。
2. 下滑超过阈值时仍正常收起（行为不变）。
3. 拖拽跟手行为不变（无过渡、跟手），松开后回弹顺滑（≈220ms easeOut）。
4. 快速连续下滑/中途回拉/多指/系统打断等异常序列下，不允许再次出现残留半屏。

## 验收标准

- [ ] 真机上多次“只下滑一点距离松手”→ 页面每次都平滑回弹到全屏，无一残留半屏
- [ ] 回弹动画进行中再次下滑/松手 → 页面仍正确回弹到全屏
- [ ] 快速连续下滑、中途回拉、横竖手势穿插 → 不回弹到半屏
- [ ] 下滑超过阈值 → 正常收起关闭播放页（原行为不变）
- [ ] 拖拽跟手无过渡；回弹动效 ≈220ms easeOut，无松手瞬间抖动
- [ ] 重新打开播放页 → 页面处于全屏顶部（`dragOffsetY === 0`），无上次残留位移

## 约束

- 动画继续用 `motion-v` 命令式 `animate()`（spec：全量 Motion 动画契约），不用 CSS transition 做回弹。
- 不开新依赖。
- 改动范围仅限 `src/views/PlayerPage.vue`（+ 需要时同步 spec 文档）。
- 遵循 `.trellis/spec/frontend/component-guidelines.md` 中 PlayerPage 拖拽/回弹相关既有契约（歌词区手势隔离、露底透明、保活重置等不得破坏）。