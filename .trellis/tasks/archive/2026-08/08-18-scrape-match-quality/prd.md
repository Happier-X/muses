# child4：匹配质量升级——歌手/时长/专辑约束 + 采纳门槛

## Goal

修复歌词/文本匹配质量差的问题：提高采纳门槛，引入歌手/时长/专辑约束，低置信匹配进候选而不是自动写库。产出被 child3 消费的置信度判定。

## Background / 依赖

- Parent: `08-18-library-tag-governance`（design.md §5）。
- 现状事实：
  - `lyrics/score.ts`：`MIN_ACCEPT_SCORE = TITLE_CONTAINS = 60`——歌名包含即采纳；歌手权重仅 25 且**无歌手信息不罚分**；无时长/专辑参与。
  - `metadata/util.ts needsOnlineTextMeta`：仅判定「需不需要补」，无质量门槛。
  - `player/types.ts shouldPersistOnlineLyrics`：仅按歌词格式 rank 严格更优才覆盖（ttml/yrc/qrc=2 > lrc=1），不校验歌手/时长。
- 本 child 独立于 UI；child3 消费其置信度函数。

## Requirements

- R4-1 `lyrics/score.ts`：门槛重构——title exact(100) 且 artist 命中(≥25) 为高置信；title contains(60) 必须 artist 命中才可采纳；有 duration 时偏差 ≤ 5s 才加分/强制约束；无 artist 信息不得采纳 contains 级。
- R4-2 `metadata/util.ts`：`needsOnlineTextMeta` 增加「云端来源字段需时长/歌手齐备才补」约束；输出置信度分级（高/低）供候选呈现。
- R4-3 `player/types.ts shouldPersistOnlineLyrics`：仅高置信（exact + artist + 时长符合）可覆盖现有 lrc；ttml/yrc/qrc 质量升级保留但同样加 artist/duration 校验。
- R4-4 候选呈现接口：低置信/多候选返回 `items[]`（现有 `EditDimResult` 结构语义）供 child3 刮削页候选选择；播放时自动补缺路径仅保留高置信自动写。

## Acceptance Criteria

- [ ] score 单测更新 + 新增用例：exact 无 artist 可采纳 / contains 有 artist 采纳 / contains 无 artist 拒绝 / duration 偏差 >5s 拒绝 / 同名歌不同歌手拒绝。
- [ ] `shouldPersistOnlineLyrics` 行为按新规则收紧（回归现有歌词质量升级场景不误伤）。
- [ ] 置信度函数输出高/低两级，child3 可消费。
- [ ] 专项回归：播放时在线歌词自动写库仍触发高置信场景；低置信不写库。
- [ ] vue-tsc build 通过；既有 19 例 vitest 不回归。

## Out of Scope

- 刮削页 UI（child3）、数据来源模型（child1）、声纹识别。