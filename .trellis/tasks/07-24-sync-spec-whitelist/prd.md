# 同步 spec 白名单：反映 0.0.2 已填补的能力

父任务：`07-24-replace-ionic-with-happier-ui`
前置：应在 `07-24-replace-ionic-low-risk` 与 `07-24-replace-player-range` 结论确定后进行，以便白名单与实际替换/保留结果一致。

## Goal

更新 `.trellis/spec/frontend/component-guidelines.md` 的"何时直连 `ion-*`（白名单）"表与相关约定，使其反映 happier-ui 0.0.2 已填补的能力，避免 spec 与代码脱节。

## Background

component-guidelines.md 白名单目前仍把以下项标为"库缺口保留"，但 0.0.2 已提供对应组件：

- `ion-progress-bar` → `HProgress`（已可替换）
- `ion-card*` → `HCard`（已可替换）
- icon-only `ion-button` → `HIconButton`（列表/导航栏场景可替换）
- `ion-range` → `HRange`（结论以 `07-24-replace-player-range` 为准）

同时 `HCell`/`HCellGroup` 是 0.0.2 新增、可能用于设置行/列表行的组件，需要在白名单/缺口说明里给出立场（本轮是否采用）。

## Requirements

- 依据两个替换子任务的实际结果更新白名单表：已替换项从"缺口保留"改为"优先 happier-ui"。
- 若 PlayerPage `ion-range` 保留，则在白名单/缺口说明中写明 `HRange` 的具体能力差距与保留原因。
- 明确 `HCell`/`HCellGroup` 的立场（采用 / 暂不采用并说明）。
- 保持 spec 内其他版本号、约定与实际代码一致；不制造与代码脱节的新表述。

## Acceptance Criteria

- [ ] 白名单表中 `ion-progress-bar`、`ion-card*`、icon-only `ion-button` 的表述与实际替换结果一致。
- [ ] `ion-range` 的表述与 `07-24-replace-player-range` 结论一致（替换或带差距说明的保留）。
- [ ] `HCell`/`HCellGroup` 立场明确。
- [ ] spec 内无自相矛盾或与代码脱节的白名单条目。

## Notes

- 纯文档任务，轻量级 PRD-only。
- 依赖前两个子任务结论，最后执行。
