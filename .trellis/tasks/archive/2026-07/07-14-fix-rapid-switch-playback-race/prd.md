# 修复快速切歌状态竞态

## Goal

修复 Issue #28 / #29：快速切歌时状态异常、进度停、显示暂停但音频仍在播。

## Root Cause

1. `playSong` 在 `await AudioPlayerNative.play()` 完成后无条件写 `status = 'playing'`；被新切歌 supersede 的旧 `play` 仍可能成功返回并把 UI 设错。
2. 切歌过程中 native unload 可能上报 `paused`/`stopped`；若 `currentSongId` 缺失，`isCurrentNativeState` 会放行（`!currentSongId` 为 true）。
3. 旧请求（歌词/封面）已有 token；播放会话本身缺 generation。

## Requirements

1. `playSong` 使用 generation：仅当前代写 playing/error 与后续 scan/prefetch。
2. 收紧 native 状态应用：有 `state.currentSong` 时必须 songId 匹配；loading 期间忽略无关 paused/stopped。
3. 单测：快速连切 A→B，最终 status/song 为 B 且 playing。

## Task Type

Lightweight
