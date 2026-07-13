# 设计：弱 title 补全

## 边界

| 改动 | 说明 |
|------|------|
| `metadata/util.ts` | `isWeakTitle` / `titlesRelated`；扩展 needs / hitFills / merge |
| `metadata/types.ts` | `OnlineTextQuery` 增加可选 `path` |
| `metadata/match.ts` | 负缓存 key 含 path/title 弱态（沿用 title+artist+album 即可；path 不进 key 也可，因 title 已是弱值） |
| `controller.ts` | 传 `path`；upsert 用合并后 `title` |
| 单测 + spec | |

## 核心逻辑

```ts
isWeakTitle(title, path) =
  normalizeText(title) === normalizeText(getTitleFromPath(path))
  // path 空或基名为空 → false（不当弱，避免误覆盖）

titlesRelated(a, b) =
  na && nb && (na === nb || na.includes(nb) || nb.includes(na))

needsOnlineTextMeta(q) =
  isBlank(artist) || isBlank(album) || isWeakTitle(title, path)

hitFillsMissing(hit, q) =
  (needArtist && hit.artist) ||
  (needAlbum && hit.album) ||
  (isWeakTitle(...) && hit.title && titlesRelated(hit.title, q.title))

merge(..., hit) =
  title: weak && hit.title related ? hit.title.trim() : latest.title
  artist/album: 仅空则填
```

## 写回

```ts
upsertSong({
  ...,
  title: next.title, // 顶层 title
  tags: { artist, album, title: next.title }, // 与 storage 合并策略对齐
})
```

确认 `storage.upsertSong`：`tags.title?.trim() || input.title` —— 传两者更稳。

## 风险

| 风险 | 缓解 |
|------|------|
| 文件名恰为真实歌名且与在线不同风格 | 弱+相关门槛；相关失败则不改 title |
| path 与 webdav 编码 | getTitleFromPath 已有 |
