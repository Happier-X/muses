# 在线封面增加 QQ 与网易云回退

## Goal

在已有 **iTunes → kw → mg → kg** 链路上增加 **tx（QQ 音乐）** 与 **wy（网易云）** 回退，进一步提升缺封面命中率；保持仅补缺、安全写回、失败静默。

## Background

### 当前默认链

`src/features/cover/match.ts`：`itunes` → `kw` → `mg` → `kg`

### 接口实测（2026-07-13）

| 源 | 结论 | 方案 |
|----|------|------|
| **tx** | 旧 `client_search_cp` / 无签名桌面搜索常空 | 移动端 `search_for_qq_cp` 有 `albummid` → 拼 `y.gtimg.cn` 封面 |
| **wy** | 搜索常无 `picUrl` | `api/search/get/web` 取 id → `api/song/detail` 取 `album.picUrl` |

### 继承约束

- 仅封面、仅补缺、安全 `file://` 写回
- 全链串行，单源失败跳过
- 不引入扩展宿主 / 不拷贝 GPL

## Decisions

| 决策 | 结论 |
|------|------|
| 本轮新增 | **tx + wy** |
| 默认顺序 | **iTunes → kw → mg → kg → tx → wy** |
| 失败策略 | 继承全链串行；无全局超时 |
| tx 搜索 | 移动 `search_for_qq_cp`（不用签名桌面接口） |
| wy 取图 | 搜索 + 详情两步；无加密 weapi |

## Requirements

1. **R1** 增加 `tx` CoverProvider：搜索 → albummid → 封面 URL 或 null。
2. **R2** 增加 `wy` CoverProvider：搜索 → song detail → picUrl 或 null。
3. **R3** 默认链为 iTunes → kw → mg → kg → **tx → wy**；任一成功即停。
4. **R4** 单源异常不中断整链；全失败负缓存行为不变。
5. **R5** `OnlineCoverSource` 含 `'tx' | 'wy'`；controller/落盘/触发不变。
6. **R6** 单测：tx/wy 单独命中；前源 miss 后命中；六源全 miss；顺序。
7. **R7** spec 同步源顺序。
8. **R8** 独立实现；不拷 GPL；不引入 any-listen 签名宿主。

## Acceptance Criteria

- [ ] AC1：默认链含 tx、wy 且位于 kg 之后。
- [ ] AC2：前源 miss 时 tx 或 wy 可返回可用 HTTP(S) URL。
- [ ] AC3：前源成功不调后源；全失败静默 + 负缓存。
- [ ] AC4：仅补缺 / 安全 URI 写回无回归。
- [ ] AC5：测试 / lint / type-check / spec 通过。

## Out of Scope

- 用户可选源 UI
- any-listen 桌面签名搜索（tx）
- 网易云 weapi/eapi 加密
- 覆盖已有封面、文本元数据

## Task Type

Complex
