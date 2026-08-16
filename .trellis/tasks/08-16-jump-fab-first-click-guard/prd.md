# 歌曲页 FAB 首次点击跳转失效（防漂移 guard 互斥）

## 背景

用户反馈：歌曲页点「跳转到当前播放」FAB，**有时候第一次跳不过去，第二次就好了**。

## 根因（已定位）

`SongsPage.vue` `onMounted` 的 4 秒防漂移 guard（e3ac5df 引入，对抗 WebView 首屏布局未稳时列表被误滚到底）：

```js
const guard = () => {
  if (!cur || interacted || Date.now() - mountAt > 4000) return
  const target = savedSongsScrollTop > 0 ? Math.min(savedSongsScrollTop, max) : 0
  if (Math.abs(cur.scrollTop - target) > 500) {
    cur.scrollTop = target   // 把 scrollTop 强制拉回期望位置
  }
}
```

每 250ms 检查，scrollTop 偏离期望位置 > 500px 就拉回。交互标记 `interacted` 只由**列表容器的** `touchstart`/`wheel`（once）置位——**FAB 在列表容器外（fixed 定位），点 FAB 不触发**。

时序：进入页面（冷启动或 tab 切回重挂载）→ 4 秒内点 FAB → `scrollToIndex` 跳转成功 → guard 250ms 内把 scrollTop 拉回（0 或保存值）→ 用户看到"第一次没跳过去"；第二次点击在 4 秒后（guard 停止）→ 正常。

"有时候"= 仅在挂载后 4 秒内点击时复现；当前曲在列表深处（FAB 可见）时必现。

## 修复

`src/views/SongsPage.vue`：

- 交互标记提升为**组件级变量**：`let mountInteracted = false` + `const stopMountGuard = () => { mountInteracted = true }`（onMounted 闭包变量无法从外部置位）。
- `scrollToCurrentSong` 开头调用 `stopMountGuard()`（用户主动跳转 = 交互，guard 停止拉回）。
- onMounted 的 touchstart/wheel 监听改用 `stopMountGuard`。

PlaylistDetailPage 有同款 guard 但无跳转 FAB，不受影响，未改。

## 验收标准

- [ ] 真机/模拟器：进入歌曲页后**立即**点 FAB，一次跳转成功（修复前 4 秒内必失败）。
- [ ] 冷启动与 tab 切回（重挂载）两种场景均验证。
- [ ] 防漂移原功能不回归：无交互时 4 秒内列表漂移仍会被拉回；手动滚动（touchstart/wheel）仍立即停止 guard。
- [ ] `npm run build` / `npm run lint` 通过。

## 非目标

- 不改 virtualizer 跳转方式（scrollToIndex smooth + rAF 兜底，08-03 已定）。
- 不改 PlaylistDetailPage guard（无 FAB）。
