# 弱 title 在线补全

## Goal

在已有 artist/album 在线补缺上，当 `title` 为 **弱标签**（等于去扩展名文件名）时，允许用在线命中的 title 覆盖；强 title 禁止覆盖。

## Background

- 无内嵌标题时：`title = getTitleFromPath(path)`
- 当前 metadata 永不改 title
- 本任务扩展：弱 title 可写；artist/album 规则不变

## Decisions

| 决策 | 结论 |
|------|------|
| 弱 title 判定 | **A**：`normalizeText(title) === normalizeText(getTitleFromPath(path))` |
| 写 title 门槛 | **B**：hit.title 非空，且与当前 title **相关**（normalize 后相等或互相包含） |
| 强 title | **禁止**覆盖 |
| 继承 | 仅补缺 artist/album；源 kw→tx→wy→kg→mg；播放自动；写回曲库；与封面并行 |

## Requirements

1. **R1** 提供 `isWeakTitle(title, path)`：与 `getTitleFromPath(path)` normalize 后相等即为弱。
2. **R2** `needsOnlineTextMeta`：在 artist/album 为空之外，**弱 title 也需要匹配**。
3. **R3** `hitFillsMissing`：弱 title 且 hit 含相关 title 时视为可填。
4. **R4** `mergeTextMetaFillEmpty`：弱 title 且 hit.title 相关时写 title；强 title 不变；artist/album 仍仅补空。
5. **R5** controller 查询与 upsert 传入 `path`；写回顶层 `title` + 必要时 tags。
6. **R6** 单测 + spec：弱可改、强不改、仅相关 title 才写、artist/album 回归。

## Acceptance Criteria

- [ ] AC1：title 等于文件名基名时可被相关在线 title 替换。
- [ ] AC2：title 与文件名不同（强）时不被覆盖。
- [ ] AC3：hit.title 与当前弱 title 无关时不写 title（仍可补 artist/album）。
- [ ] AC4：artist/album 仅补缺行为无回归。
- [ ] AC5：测试 / lint / tsc / spec 通过。

## Out of Scope

- 占位词表、音轨号启发式（非 A 判定）
- 手选候选、批量刮削、歌词

## Task Type

Complex
