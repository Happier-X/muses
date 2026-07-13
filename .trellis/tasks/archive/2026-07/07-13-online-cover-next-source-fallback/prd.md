# 在线封面下一平台回退

## Goal

在已有 **iTunes → kw → mg** 链路上继续增加下一平台回退源，进一步提升缺封面命中率；保持仅补缺、安全写回、失败静默。

## Background

### 当前默认链

`src/features/cover/match.ts`：`itunes` → `kw` → `mg`

### 候选源粗评（本轮选型）

| 源 | 搜索 | 取图 | 备注 |
|----|------|------|------|
| **kg 酷狗**（倾向推荐） | `songsearch.kugou.com/song_search_v2` 普通 GET | 搜索结果常带 `Image`（`{size}` 可替换），或再走 privilege | 实测有结果；实现成本中等 |
| **tx QQ** | 旧 `client_search_cp` 可能空结果；扩展用桌面 `signRequest` | 可用 albumMid 拼 `y.gtimg.cn` 封面 | 签名复杂；旧接口不稳定 |
| **wy 网易云** | eapi/weapi | 详情 picUrl | 加密重，本轮不优先 |

### 继承约束

- 仅封面、仅补缺、安全 `file://` 写回
- 全链串行，单源失败跳过
- 不引入扩展宿主 / GPL 拷贝

## Decisions

| 决策 | 结论 |
|------|------|
| 下一平台 + 顺序位置 | **A：+kg 酷狗**，挂链尾 → iTunes → kw → mg → kg |
| 失败策略 | 继承：全链串行；单源失败/无 URL 即跳下一源；无全局超时 |
| 取图方式 | 直接用搜索结果 `Image`（`{size}` → 480），不再二次拉详情 |
| http→https | 升 https（与 kw/mg 一致） |
| 误匹配 | 轻量打分（标题/歌手/专辑），参考 mg 实现 |

## Requirements

1. **R1** 增加 `kg` CoverProvider，`searchCoverUrl(query)` 返回可用 HTTP(S) 封面 URL 或 null。
2. **R2** 默认链为 iTunes → kw → mg → **kg**；任一源成功即停止。
3. **R3** 单源网络/解析异常不得中断整链；全失败行为与现负缓存一致。
4. **R4** `OnlineCoverSource` 扩展含 `'kg'`；controller/落盘/触发不变。
5. **R5** 单测：kg 单独命中；前三源 miss 后 kg 命中；四源全 miss；顺序（前源成功不调后源）。
6. **R6** spec 同步：features-player / state-management 源顺序含 kg。
7. **R7** 独立实现（参考公开搜索接口形态），不拷贝 GPL 项目源码。

## Out of Scope

- 用户可选源 UI
- 一次加满 tx+wy+kg
- 覆盖已有封面

## Task Type

Complex
