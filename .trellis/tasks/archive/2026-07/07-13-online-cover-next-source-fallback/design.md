# 设计：封面回退增加酷狗 (kg)

## 边界

| 层 | 改动 |
|----|------|
| `providers/kg.ts` | 新建：搜索 → Image URL |
| `types.ts` | `OnlineCoverSource` 增加 `'kg'` |
| `match.ts` | defaultProviders 追加 `kgCoverProvider` |
| 单测 + spec | 顺序与回退 |
| 不改 | controller 触发、cacheRemoteCover、歌词、mg/kw/itunes |

## kg 方案

公开搜索接口（普通 GET，无签名）：

```
GET https://songsearch.kugou.com/song_search_v2
  ?keyword={encodeURIComponent(term)}
  &page=1&pagesize=10
  &userid=0&clientver=&platform=WebFilter&filter=2
  &iscorrection=1&privilege_filter=0&area_code=1
Headers:
  User-Agent: 桌面常见 UA
  Referer: https://www.kugou.com/
```

实测响应结构（`data.lists[]`）关键字段：

- `SongName` / `OriSongName` / `SingerName` / `AlbumName` / `AlbumID`
- **`Image`**：封面 URL，含 `{size}` 占位，例
  `http://imge.kugou.com/stdmusic/{size}/20230920/xxx.jpg`

实现要点：

1. `term = title + artist + album`（同前源）
2. 取 `data.lists`，过滤出 `Image` 非空项
3. `Image` 的 `{size}` 替换为 `480`；`http://` → `https://`；必须 `^https?://`
4. 轻量打分（标题/歌手/专辑匹配），取最高分项
5. 空列表 / 无 Image / 非 JSON / 无 `data` → null
6. 独立实现，不粘贴 music-tag-web（GPL）源码

不进行二次详情拉取（搜索结果已带封面 URL）。

## 编排

```ts
const defaultProviders = [itunesCoverProvider, kwCoverProvider, mgCoverProvider, kgCoverProvider]
// 既有 for 循环：成功 URL 即 return；catch 下一源
```

## 风险

| 风险 | 缓解 |
|------|------|
| kugou 接口变更/风控 | catch → null；前源兜底 |
| `{size}` 占位格式变 | 仅做字符串 replace；保留原值兜底校验域名 |
| `Image` 为空 | 过滤后无项 → null |

## 回滚

- 从 defaultProviders 去掉 kg；删除 `providers/kg.ts`
