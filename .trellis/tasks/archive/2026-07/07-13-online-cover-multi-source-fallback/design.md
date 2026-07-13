# 设计：封面回退增加咪咕 (mg)

## 边界

| 层 | 改动 |
|----|------|
| `providers/mg.ts` | 新建：搜索 → 封面 URL |
| `types.ts` | `OnlineCoverSource` 增加 `'mg'` |
| `match.ts` | defaultProviders 追加 `mgCoverProvider` |
| 单测 + spec | 顺序与回退 |
| 不改 | controller 触发、cacheRemoteCover、歌词 |

## mg 方案

优先使用 **较简的咪咕移动搜索**（无需 jadeite 签名）：

```
GET https://m.music.migu.cn/migu/remoting/scr_search_tag
  ?rows=10&type=2&keyword={encodeURIComponent(term)}&pgc=1
Headers:
  User-Agent: 桌面/移动常见 UA
  Referer: https://m.music.migu.cn/
```

响应 JSON 中 `musics[]` 项常见字段：

- `songName` / `singerName` / `albumName`
- **`cover`**：封面 URL（直接可用）

实现要点：

1. `term = title + artist + album`（同 itunes/kw）
2. 取列表首条或简单标题相关项的 `cover`
3. 规范化：http → https（若适用）；必须 `^https?:`
4. 空列表 / 无 cover / 非 JSON → null
5. **独立实现**，不粘贴 music-tag-web（GPL）源码；接口形态属公开 Web 搜索常见路径

any-listen 的 mg 签名搜索（jadeite）本轮 **不采用**（复杂度高）。

## 编排

```ts
const defaultProviders = [itunesCoverProvider, kwCoverProvider, mgCoverProvider]
// 既有 for 循环：成功 URL 即 return；catch 下一源
```

## 风险

| 风险 | 缓解 |
|------|------|
| m.migu 接口变更/风控 | catch → null；有 iTunes/kw 兜底 |
| Referer 缺失失败 | 带 Referer + UA |
| 误匹配 | 仅补缺；可选简单 normalize 标题相关（与 itunes 类似可做轻量） |

## 回滚

- 从 defaultProviders 去掉 mg；删除 `providers/mg.ts`
