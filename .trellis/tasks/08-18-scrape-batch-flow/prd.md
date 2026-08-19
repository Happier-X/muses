# child3：批量刮削流程页——匹配 → 差异预览 → 确认 → 写回

## Goal

刮削中心（`/scrape`）：从待刮削队列批量发起云端匹配，展示「当前 vs 候选」差异预览，用户逐行/整批勾选后确认写回（曲库 + 音频文件），失败行可识别、可重试、可回滚。

## Background / 依赖

- Parent: `08-18-library-tag-governance`（design.md §4 写回、§6 风险）。
- 依赖 child1 队列 API + 来源字段（写回后标记来源）；依赖 child4 置信度函数（高/低置信默认勾选策略）。
- 复用：`searchEditCloudMeta`（editMeta 层三路候选）、`writeLocalAudioMetadata`/`writeWebDavAudioMetadata`（本地/WebDAV 写回桥，失败返回 ok:false+code）、`cacheRemoteCover`、WebDAV 密码（sources storage）。

## Requirements

- R3-1 页面三态：队列 → 匹配中（进度）→ 差异预览；队列空时引导入队。
- R3-2 匹配编排：队列 songId resolve 到最新 SongItem，逐曲三路并行匹配（文本/封面/歌词），**不写库**。
- R3-3 差异预览：每行展示当前值 vs 候选（封面图、歌词格式）；置信度徽标（高/低）；高置信默认勾选、低置信默认不勾，点行开候选列表手动选。
- R3-4 写回编排：勾选行批量执行——先写文件（本地并行/WebDAV 串行），再写库（来源 embedded/cloud 按文件结果）；写前缓存旧值到回滚 journal。
- R3-5 结果态：成功 / 文件失败(已入库) / 失败；失败行可重试（仅重跑失败行）。
- R3-6 撤销：一次性「撤销本次刮削」恢复曲库旧值（写库回滚）；UI 明示文件已写不可撤。
- R3-7 与在线补缺联动：刮削写回后作废旧在线补缺 token（防并发覆盖）；在线补缺写库字段带 cloud 来源（来源模型来自 child1）。

## Acceptance Criteria

- [ ] 队列歌曲可一键发起批量匹配，进度展示，单曲失败不影响其他曲。
- [ ] 差异预览逐行正确展示当前/候选；高置信默认勾选、低置信默认不勾。
- [ ] 确认后：曲库值更新 + 本地/WebDAV 文件写入（真机验证）；成功行来源 embedded、文件失败行来源 cloud。
- [ ] 失败行状态可识别、可重试；重试仅重跑失败行。
- [ ] 撤销恢复曲库存量旧值；提示文案明示文件级不可逆。
- [ ] 写回/回滚单测 ≥ 8 例；vue-tsc build 通过；既有回归不破坏。

## Out of Scope

- 入队入口（child2）、评分/置信度算法细节（child4 提供接口）、新元数据源平台。