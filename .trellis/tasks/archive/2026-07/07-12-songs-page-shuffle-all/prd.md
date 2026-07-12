# 歌曲页随机播放全部按钮

## Goal

在歌曲页顶部左侧增加随机播放按钮，点击后随机播放全部歌曲。

## Issue

- GitHub #6：`歌曲页面顶部左侧加一个随机播放的按钮，在点击的时候，随机播放全部歌曲`

## Confirmed Facts

- `SongsPage.vue` 顶部已有 `<ion-toolbar>` / `<ion-title>`；右侧已有歌曲数量与排序按钮。
- 播放队列：`clearQueue()`、`enqueueSongs(songs)`、`toggleShuffle()`、`selectSongAtIndex(index)`、`playSong(song)` 均已导出。
- `shuffleEnabled` / `repeatMode` 状态可读。
- 现有列表点击播放：`playSong`，不自动 enqueue 全部。
- 随机播放语义：清空队列 → 装入全部 → 开启 shuffle → 从乱序首曲播放。

## Requirements

1. **R1** 歌曲页顶部左侧（title 左侧或 toolbar 内合适位置）放一个随机播放按钮，图标用 `shuffle`。
2. **R2** 点击后：`clearQueue` → `enqueueSongs(allSongs)` → 开启 shuffle → 选取乱序第一首 `playSong`。
3. **R3** 必须提供 `aria-label`（如「随机播放全部」）。
4. **R4** 无歌曲时按钮禁用或不响应。
5. **R5** 不破坏现有顶部右侧功能（排序、数量显示）。
6. **R6** 补充单测；lint/type-check 通过。
7. **R7** 同步 component-guidelines（如有 SongsPage 约定）。

## Acceptance Criteria

- [ ] AC1：顶部左侧有 shuffle 图标按钮，带 aria-label。
- [ ] AC2：点击后随机播放全部（队列含全部、shuffle 开启、从乱序首曲起播）。
- [ ] AC3：无歌曲时点击无副作用。
- [ ] AC4：相关单测通过。

## Technical Notes

实现建议：

```ts
const onShuffleAll = () => {
  if (songs.value.length === 0) return
  clearQueue()
  enqueueSongs(songs.value)
  if (!shuffleEnabled.value) toggleShuffle()
  const first = selectSongAtIndex(0)
  if (first) void playSong(first)
}
```

注意 `toggleShuffle` 会生成 `shuffleOrder`；`selectSongAtIndex(0)` 取乱序首曲。

## Task Type

Lightweight
