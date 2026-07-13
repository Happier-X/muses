# 在线封面多平台回退

## Goal

在现有「iTunes → 酷我 (kw)」在线封面匹配链路上增加 **咪咕 (mg)** 回退，提高缺封面歌曲（尤其华语）命中率；保持仅补缺、安全本地缓存写回、失败静默。

## Background

### 已实现（#18）

- `src/features/cover/`：`matchOnlineCoverRemote` 多源编排 + 负缓存
- Provider：`itunes`、`kw`
- 触发 / 落盘 / 安全写回规则已稳定

### 本轮范围

- 仅新增 **mg** 一源；顺序 **iTunes → kw → mg**
- 失败策略与 #18 一致：全链串行，单源失败 try/catch 后跳过

## Decisions

| 决策 | 结论 |
|------|------|
| 新增平台 | **咪咕 mg** |
| 默认顺序 | **iTunes → kw → mg** |
| iTunes 为首 | **是** |
| 失败策略 | **A：全链串行**；单源失败/无 URL 即跳下一源；无全局超时预算 |

## Requirements

1. **R1** 增加 `mg` CoverProvider，实现 `searchCoverUrl(query)`，返回可用 HTTP(S) 封面 URL 或 null。
2. **R2** 默认 provider 顺序为 iTunes → kw → mg；任一源成功即停止。
3. **R3** 单源网络/解析异常不得中断整条链；全部失败行为与现负缓存一致。
4. **R4** `OnlineCoverSource` 扩展为含 `'mg'`；controller/落盘/触发逻辑不变。
5. **R5** 单测：mg 单独命中；iTunes+kw miss 后 mg 命中；三源全 miss；顺序（前源成功不调后源）。
6. **R6** 同步 features-player / state-management 中「iTunes → kw」表述为含 mg。
7. **R7** 不拷贝 GPL 项目源码；咪咕实现可参考公开移动端搜索接口形态，自研适配（与 kw 移植 any-listen Apache 代码区分清楚）。

## Acceptance Criteria

- [ ] AC1：默认链为 iTunes → kw → mg。
- [ ] AC2：前两源 miss/失败时 mg 成功可返回封面 URL 并走既有下载写回。
- [ ] AC3：mg 失败不影响前源已成功路径；三源皆失败静默 + 负缓存。
- [ ] AC4：触发/仅补缺/安全 URI 写回无回归。
- [ ] AC5：测试 / lint / type-check 通过；spec 已更新。

## Out of Scope

- tx / wy / kg
- 用户可选源设置
- 全局超时预算
- 覆盖已有封面、文本元数据、歌词

## Task Type

Complex
