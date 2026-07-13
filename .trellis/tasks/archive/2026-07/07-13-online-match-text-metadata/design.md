# 设计：在线文本元信息（artist / album）

## 边界

| 层 | 改动 |
|----|------|
| `src/features/metadata/` | 新建：查询、多源编排、负缓存 |
| `controller.ts` | 播放后异步 `matchOnlineTextMeta`；token；upsert + sync |
| 单测 + spec | 仅补缺、触发、顺序 |
| 不改 | 封面模块对外 API 契约、歌词 amll、title 写回 |

## 数据流

```
playSong
  → (可选) scanSongMetadata
  → 若 artist/album 仍有空
  → matchOnlineTextMeta(query)  // 异步
  → 命中 partial { artist?, album? }
  → 与 latest 合并（仅空字段）
  → upsertSong
  → token 校验 → syncDisplayStateFromSong + 媒体会话文本
```

## 模块草图

```
src/features/metadata/
  types.ts       OnlineTextQuery / MatchResult / Source
  match.ts       编排 + 负缓存
  providers/     可复用 cover 的 http，但返回文本字段
  index.ts
```

Provider 契约（示意）：

```ts
type TextMetaHit = {
  title?: string
  artist?: string
  album?: string
  source: 'kw' | 'tx' | 'wy' | 'kg' | 'mg'
}

type TextMetaProvider = {
  id: TextMetaHit['source']
  search(query: OnlineTextQuery): Promise<TextMetaHit | null>
}
```

- 查询关键词：`title + artist + album`（空段省略）；至少有 title
- 命中条件：返回的 hit 中，**当前仍缺的字段至少有一个非空**
- 打分：标题相关优先（与 cover 类似），避免乱匹配

### 与 cover provider 关系

- **不**强制调用 `matchOnlineCoverRemote`
- 可复制/抽取各平台「搜索解析」逻辑到 metadata providers（允许一定重复，优先交付）
- 后续可再抽 `features/online-search` 共享层（本任务不做大重构）

## 源顺序

`kw → tx → wy → kg → mg`（串行；单源异常 catch 下一源）

## 写回合并

```ts
const next = {
  ...latest,
  artist: latest.artist?.trim() ? latest.artist : (hit.artist?.trim() || latest.artist),
  album: latest.album?.trim() ? latest.album : (hit.album?.trim() || latest.album),
}
// 若 next 相对 latest 无字段变化 → 不 upsert
```

## 触发

与封面并列：

- `scanSongMetadata` 完成或跳过扫描后
- `needsTextMeta(song) = !song.artist?.trim() || !song.album?.trim()`
- `onlineTextToken` 与 `onlineCoverToken` 分开

## 风险

| 风险 | 缓解 |
|------|------|
| 误匹配写库 | 仅补缺；标题打分；负缓存 |
| 与封面重复请求 | 可接受；失败互不影响 |
| 文件名 title 导致搜偏 | 不改 title；搜索仍用现有 title |

## 回滚

- 去掉 controller 调用；删除 `features/metadata`
