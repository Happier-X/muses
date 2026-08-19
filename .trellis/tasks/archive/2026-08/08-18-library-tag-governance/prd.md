# 曲库 tag 治理（parent）：来源可视化 + 刮削模块 + 匹配质量升级

## Goal

用户反馈 tag 获取体验乱：内置 tag 与云端补缺混拼、来源不可见、歌词匹配质量差。用户期望：在主应用内新增一个**主动刮削模块**（类比 MusicBrainz Picard / beets / MusicTag），针对用户音乐库的音频文件批量抓取云端元数据、预览差异、确认后写回，实现曲库优化闭环——字段来源可视化、内置 tag 权威、匹配质量升级、批量治理。

## Background（代码已确认事实）

tag 数据流共 4 条链路，SongItem 里的字段是它们**合并**的结果：

1. **扫描入库**（`src/features/library/scanner.ts` → `readLocalAudioTags` / `readWebDavAudioTags` / `native.ts readMetadata`）：从音频文件读**内置** tag（title/artist/album/duration/lyrics/cover/RG）。
2. **播放懒扫描**（`src/features/player/controller.ts` `scanSongMetadata`）：当 `tagsScanned !== true || metadataVersion !== 3 || (!lyrics && !coverUri)` 时，播放中再读一次内置 tag。
3. **在线补缺**（controller + `prefetchMetadata.ts`，三路并行）：
   - 文本：artist/album **仅补空**，弱 title（=文件名）可替换；源链 kw→tx→wy→kg→mg
   - 封面：**仅补缺**；源链 iTunes→kw→tx→wy→kg→mg，下载到 cache/covers
   - 歌词：格式/质量**严格更优**才覆盖（`lyricsFormatRank`：ttml/yrc/qrc=2，lrc=1）；源链 amll TTML → 平台五源 → LRCLIB
   - 写库前经 `userEditedFields` 保护（`storage.ts applyTagsRespectingUserEdits`）
4. **手动编辑**（PlayerPage 编辑 sheet → `saveCurrentSongUserEdit` → `updateSongUserEdit`）：写曲库 + 尝试写回音频文件（本地 `writeLocalAudioMetadata` / WebDAV `writeWebDavAudioMetadata`），字段进入 `userEditedFields` 保护集。写回桥已支持 title/artist/album/lyrics/cover/RG，失败返回 `ok:false + code/message`。

**问题根源**：
- SongItem 无来源字段——只有歌词带 `lyricsSource`（embedded/sidecar/online），title/artist/album/封面完全不可见来源 → 用户感知「乱」。
- 在线补缺**自动写库**、无确认闭环；补错的内容会永久留在曲库存量里（即使不被保护字段，下次扫描 `?? previous` 也不回退）。
- 歌词采纳门槛低：`score.ts` `MIN_ACCEPT_SCORE = 60`（歌名「包含级」即采纳），歌手权重仅 25、无歌手信息不罚分 → 同名歌/翻唱/Live-录音室多版本误配常见。
- 无批量治理入口：优化曲库只能一首首打开编辑。

## Requirements

- R1 字段来源追踪与可视化：持久化每个字段（title/artist/album/cover）的来源（embedded/cloud/manual），UI 可识别；歌词沿用现有 `lyricsSource`。
- R2 权威合并策略：**文件内置 tag 为权威**。云端仅确定性补缺，仅用户显式选择才覆盖内置有值字段；内置空→云端补并标注来源；内置值可疑（乱码/文件名占位）→列候选，确认后覆盖。
- R3 匹配质量升级：歌词/文本/封面匹配更可靠——歌手/时长/专辑约束参与判定，提高采纳门槛，低置信进候选而不是自动写库。
- R4 播放页标记入列：播放页一键把当前曲目推入「待刮削」队列，不打断听歌。
- R5 入队入口（三层）：①播放页标记；②歌曲列表/详情/多选加入；③主动筛选可疑歌曲批量加入（标题=文件名占位/缺歌手/缺专辑/缺封面/缺歌词/来源可疑）。
- R6 批量刮削流程：选择范围（队列/筛选结果）→ 批量云端匹配 → 候选与差异预览 → 确认 → 写回。
- R7 写回语义：确认后**曲库 + 音频文件都写**（本地/WebDAV 均有桥）；写文件失败不影响播放、明确提示；写前缓存旧值支持回滚。

## Acceptance Criteria（parent 级，跨 child 集成验收）

- [ ] AC1 数据模型：新增来源字段与待刮削队列存储；旧数据无来源标记默认视为内置，升级不丢数据（存量兼容）。
- [ ] AC2 三个入队入口全部可用：播放页标记、列表/多选加入、筛选批量加入；重复入队幂等；队列可查看/移除/清空。
- [ ] AC3 刮削流程闭环：对队列歌曲批量匹配 → 差异预览（当前 vs 候选）→ 逐行/整批勾选 → 确认。
- [ ] AC4 写回：确认后曲库更新 + 文件写入（本地/WebDAV）；失败行状态可识别、可重试；写前旧值可回滚。
- [ ] AC5 匹配质量：歌词采用需要歌手/时长/专辑约束；低置信匹配进入候选不自动写库；提高采纳门槛。
- [ ] AC6 来源可视化：编辑页/详情页可见字段来源标识；在线补缺写入的新数据带来源标记。
- [ ] AC7 回归：扫描/懒扫/在线补缺/手动编辑既有行为不破坏；vue-tsc + vite build + vitest 通过；原生单元测试通过。
- [ ] AC8 真机验证：本地写回 + WebDAV 写回各验证一次成功与失败路径。

## Out of Scope

- 桌面端/网页版 tag 编辑器（本任务只在 muses 主应用内实现）
- 新增云端元数据源平台（复用现有 kw/tx/wy/kg/mg/itunes/LRCLIB/amll）
- 声纹识别（acoustid 类）匹配——仅用文本/时长匹配
- 备份到云端/外部存储的管理界面（回滚仅内存/本地 journal）

## Notes

- 复杂任务：parent + 4 children，各 child 见 `task-subtree` 链接。
- 需求决策已收敛：Q1 内置权威 / Q2 主动刮削模块 / Q3 三层入口 / Q4 曲库+文件都写 / Q5 存量默认内置（全部 2026-08-18 用户确认或代码依据）。
- 风险：R7 写回文件不可逆性问题通过「写前缓存旧值 + 回滚 journal」缓解，仍提示用户确认；WebDAV 写回依赖网络，失败行 label 处理不重扫。