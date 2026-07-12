# 歌词点击跳转对应行

## Goal

在沉浸式歌词页点击某一行歌词，跳转到该行对应播放进度。

## Issue

- GitHub #2：`增加歌词点击跳转到对应行的功能`

## Confirmed Facts

- 使用 `@applemusic-like-lyrics/vue` 的 `LyricPlayer`
- 组件 emits：`lineClick`（内部监听 core 的 `line-click`）
- 事件类型 `LyricLineMouseEvent`：含 `lineIndex`、`line`（`LyricLineBase`，有 `startTime`，单位毫秒）
- 当前 `PlayerPage` 未绑定 `@line-click` / `@lineClick`
- 进度同步：`current-time="playerState.position * 1000"`（毫秒）
- seek API：`seekPlayback(seconds)`

## Requirements

1. **R1** 点击有时间戳的歌词行，调用 `seekPlayback(line.startTime / 1000)`
2. **R2** 无效行（无 startTime / 负数）不 seek
3. **R3** 不改动无歌词空状态
4. **R4** 点击 seek 不触发关闭 overlay 或误切换面板（必要时 stop 手势）
5. **R5** 单测覆盖点击行触发 seek

## Acceptance Criteria

- [ ] AC1：绑定 AMLL `lineClick`，seek 到行起始秒
- [ ] AC2：有歌词时点击可跳转；无歌词页无此行为
- [ ] AC3：相关单测通过

## Technical Notes

```vue
<LyricPlayer @line-click="onLyricLineClick" ... />
```

```ts
const onLyricLineClick = async (event: LyricLineMouseEvent) => {
  const startMs = event.line?.startTime
  if (typeof startMs !== 'number' || !Number.isFinite(startMs) || startMs < 0) return
  await seekPlayback(startMs / 1000)
}
```

## Task Type

Lightweight
