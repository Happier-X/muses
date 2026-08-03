# 实现清单 — 跳转当前曲二次点击下移

## 步骤

1. **`scrollToCurrentSong`（`SongsPage.vue`）**
   - 保留 `findIndex` + `rowVirtualizer.scrollToIndex(index, { align: 'start' })`。
   - 删除 `row.scrollIntoView(...)`。
   - `nextTick` / rAF 后只做高亮（`highlightedSongId`）；可选二次 `scrollToIndex` 兜底 measure，仍禁止 `scrollIntoView`。
   - 评估 `behavior: 'smooth'`：若 smooth 与二次 index 冲突则改瞬时或仅一次 smooth。

2. **模板**
   - 去掉虚拟行 `scroll-mt-[108px]`（滚动端口已在 navbar/shuffle 下，108px 为错误补偿）。

3. **Spec**
   - 更新 `component-guidelines.md` FAB 段：虚拟列表以 `scrollToIndex` 为准；禁止双滚 + 错误 scroll-margin；注明 chrome 在滚动容器外。

4. **验证**
   - 远处跳转一次、再连点：位置稳定。
   - 列表底部当前曲：最大 scroll。
   - lint / build。

## 回滚

- 还原 `SongsPage.vue` 与对应 spec 段落。
