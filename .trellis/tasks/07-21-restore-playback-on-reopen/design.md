# 设计：#49 重启后恢复上次播放信息

## 产品决策

- **恢复为暂停**（方案 A），不自动出声。
- `stopPlayback` 清空当前播放记忆；队列列表仍保留。

## 持久化

扩展或新增 localStorage 键（建议 `muses:playback-session`）：

```ts
{
  currentSongId: string
  position: number
  // 可选：updatedAt
}
```

`currentIndex`：可由 `currentSongId` 在 active order 中解析；切歌时写入 songId 更稳。

队列仍用现有 `muses:queue`。

## 写入时机

- 切歌成功、seek 成功、pause：立即写
- playing 中 position：节流（如 2–5s）或与 #47 轮询同频节流写
- stop：清除 session

## 恢复（`initializePlayer`）

1. 读 session + 队列
2. song 仍在曲库且在队列中 → `setCurrentIndex` / 等价 API，填充 `playerState` 展示（paused, position clamp）
3. **不**调用 native play；用户点播放再 `playSong` 或「从 position 起播」路径
4. 缺失 song：清 session 或尝试跳过

## 起播衔接

恢复后点播放：应对当前曲 `play` 并 `seek` 到保存 position（或 play 支持 startPosition）。需注意与现有 `playSong` 从 0 起播的差异。

## 排序

在 #47 后实现，避免错误进度写入。
