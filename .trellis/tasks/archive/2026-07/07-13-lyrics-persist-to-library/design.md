# 设计：在线歌词质量写回

## 模型

```ts
type LyricsSource = 'embedded' | 'sidecar' | 'online'
type SongLyricsFormat = 'lrc' | 'ttml' | 'yrc' | 'qrc'
// SongItem.lyricsFormat?: SongLyricsFormat
```

## 质量

```
rank: ttml|yrc|qrc = 2, lrc = 1, empty = 0
shouldWrite(existing, incoming) => rank(incoming) > rank(existing)
```

无 `lyrics` 或空串 → rank 0。无 `lyricsFormat` 但有 lyrics → 视为 `lrc`（兼容旧数据）。

## 数据流

1. `playSong`：snapshot；`state.lyrics/format` 来自库（format 缺失则默认 lrc）。
2. 始终 `matchOnlineLyricsForSong`。
3. 命中：`playerState` 更新为在线结果（当次展示可升级）。
4. 读库最新 song → `shouldWrite` → 是则 `upsertSong` + 同步 snapshot（token 校验）。

## 兼容

- 旧库仅有 embedded/sidecar LRC：可被更好在线格式覆盖。
- storage `isOptionalLyricsSource` 接受 `online`；`lyricsFormat` 白名单校验。

## Spec 变更

- 删除「禁止写回」；改为质量写回 + 字段约定。
