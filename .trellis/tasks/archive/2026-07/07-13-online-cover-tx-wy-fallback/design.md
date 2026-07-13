# 设计：封面回退 +tx +wy

## 边界

| 层 | 改动 |
|----|------|
| `providers/tx.ts` | 新建 |
| `providers/wy.ts` | 新建 |
| `types.ts` | `OnlineCoverSource` 增加 `'tx' \| 'wy'` |
| `match.ts` | defaultProviders 追加 tx、wy |
| 单测 + spec | 顺序与回退 |
| 不改 | controller 触发、cacheRemoteCover、既有四源逻辑 |

## tx（QQ）

### 搜索

```
GET https://c.y.qq.com/soso/fcgi-bin/search_for_qq_cp
  ?g_tk=5381&uin=0&format=json&inCharset=utf-8&outCharset=utf-8
  &notice=0&platform=h5&needNewCode=1
  &w={keyword}&zhidaqu=1&catZhida=1&t=0&flag=1&ie=utf-8&sem=1&aggr=0
  &perpage=10&n=10&p=1&remoteplace=txt.mqq.all
Headers:
  Referer: https://y.qq.com/
  User-Agent: iPhone/Mobile 常见 UA
```

响应路径：`data.song.list[]`，字段：

- `songname` / `singer[].name` / `albumname` / **`albummid`**

### 封面 URL

```
https://y.gtimg.cn/music/photo_new/T002R500x500M000{albummid}.jpg
```

- 无 `albummid` 或 mid 为空/`空` → 跳过该项
- 可选用歌手 mid 拼 `T001R500x500M000{singermid}.jpg` 作次选（可选，MVP 仅 albummid 即可）

### 打分

同 mg/kg：标题 / 歌手 / 专辑轻量匹配。

## wy（网易云）

### 1. 搜索

```
GET https://music.163.com/api/search/get/web
  ?s={keyword}&type=1&offset=0&total=true&limit=10
Headers:
  Referer: https://music.163.com
  User-Agent: 桌面 UA
```

`result.songs[]`：`id` / `name` / `artists[].name` / `album.name`（常无 picUrl）

### 2. 详情取图

对打分最高项（或前几条中有 id 者）请求：

```
GET https://music.163.com/api/song/detail/?ids=[{id}]
```

取 `songs[0].album.picUrl`；`http` → `https`。

MVP：对排序后的候选 **最多尝试 3 条** 详情，取第一条有效 picUrl，避免 N 次请求爆炸。

## 编排

```ts
const defaultProviders = [
  itunesCoverProvider,
  kwCoverProvider,
  mgCoverProvider,
  kgCoverProvider,
  txCoverProvider,
  wyCoverProvider,
]
```

## 风险

| 风险 | 缓解 |
|------|------|
| QQ 移动搜索变更 | catch → null |
| 网易云搜索噪声大 | 打分 + 最多 3 次详情 |
| 接口风控 | 前源兜底；失败静默 |

## 回滚

- defaultProviders 去掉 tx/wy；删除对应 provider 文件
