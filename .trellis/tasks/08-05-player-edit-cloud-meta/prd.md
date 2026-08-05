# 编辑歌曲信息接入云端元信息

## Goal

在沉浸页「编辑歌曲信息」中，用户可**主动**从已有多平台云端拉取 **title / artist / album / 封面 / 歌词**；展示各维度**最优 + 可换其它候选**；经 **分字段勾选** 应用到表单后仍可手改；保存仍走现有写库 + 尽力写文件（含 `userEditedFields`）。

## Background

- 编辑已存在：手改六字段 + `saveCurrentSongUserEdit`（D4 库优先、文件尽力）。
- 云端已存在但仅服务播放**静默补空**：`matchOnlineTextMeta` / `matchOnlineCoverRemote` / `matchOnlineLyrics`（多为 first-hit，文本还受 `needsOnlineTextMeta` 限制）。
- 本需求是**编辑态主动搜 + 多候选 + 用户确认字段**，与静默补空产品路径分离。

## Decisions locked

| # | 决策 | 选择 |
|---|------|------|
| D1 | 交互形态 | **C 混合**：默认最优预览；可展开其它候选切换预览 |
| D2 | 拉取范围 | **C** 文本 + 封面 + 歌词（RG 不拉） |
| D3 | 触发 | **A 仅手动**「从云端获取」；打开编辑不自动搜 |
| D4 | 写入表单 | **D 分字段勾选**：不自动覆盖表单；勾选 title/artist/album/cover/lyrics 后「应用到表单」 |
| D5 | 候选深度 | **A 全维多候选**：文本 / 封面 / 歌词均可换候选 |
| D6 | 勾选默认 | **有云端值的字段默认全勾**（未单独确认，产品默认） |

### 用户主路径

1. 打开「编辑歌曲信息」→ 表单为当前曲库/播放态（与现网一致）。
2. 点 **从云端获取** → loading（不阻塞播放）。
3. 结果区展示各维度**当前选中候选**（初始=各维度最优）+ 来源/格式摘要；可展开列表换候选（只改预览，不改表单）。
4. 勾选要写入的字段（默认：该维有值则勾选）→ **应用到表单**。
5. 表单可继续手改（含封面本地选图、RG）→ **保存** → `saveCurrentSongUserEdit`。
6. 关 sheet / 切歌：丢弃在途请求与未应用的云端预览状态。

## Requirements

1. **入口**：仅沉浸页已有编辑 sheet 内增加云端区；不新增列表/MiniPlayer 编辑入口。
2. **强制搜索**：编辑拉取**忽略**「仅补空 / not-needed / 已有 userEditedFields 跳过请求」；以**当前表单**（或打开时 seed）的 title/artist/album 为查询词。
3. **多候选**：
   - 文本：多源可收集多条 `TextMetaHit`（去重后排序），默认最优。
   - 封面：多源可收集多个 http(s) URL，默认第一条可用；**应用时**再 `cacheRemoteCover` → 安全 `file://` 进表单（禁止 data/http 入库）。
   - 歌词：多源可收集多条（含 format/source/可选 translation）；列表展示来源+格式+短预览，不整页倾倒全文；应用写入 lyrics 文本 + 对应 format（及 translation 若产品在表单侧支持——当前编辑表单仅 LRC 大文本，**应用时以主词 text 写入，format 写入 patch；translation 若现表单无独立字段则写入 lyricsTranslation 仅当保存路径已支持，否则 MVP 将 translation 附注或合并策略写在 design**）。
4. **分字段应用**：仅勾选字段写入 `editForm` / 封面 ref；未勾选不动。
5. **保存**：不改 D4 保存契约；只有相对 baseline dirty 的字段进 `userEditedFields`。
6. **反馈**：全失败 Toast/文案；部分成功分维状态（如文本 ok、歌词 miss）；网络错误可重试（再点获取）。
7. **并发**：token/世代号；关 sheet 或 `currentSong.id` 变化作废结果。
8. **播放路径不变**：现有 `matchOnline*` 静默补空行为保持；新 API 可并列（如 `search*ForEdit`）或带 `mode: 'edit' | 'playback'`，禁止破坏补空语义。
9. **无障碍**：获取/应用/候选按钮有明确 `aria-label`；loading 时禁用重复提交。

## Out of Scope

- 新平台、登录、付费 API
- iOS 写标签
- 打开编辑自动搜
- MiniPlayer / SongsPage 更多菜单编辑
- 改播放静默补空的「仅补空」策略
- 云端拉 ReplayGain
- 未经勾选确认直接改表单

## Acceptance Criteria

- [ ] AC1：编辑 sheet 有「从云端获取」；打开编辑**不会**自动发起云端请求
- [ ] AC2：获取后展示文本/封面/歌词预览（有结果时）；可切换该维其它候选且**不**改表单
- [ ] AC3：分字段勾选 +「应用到表单」只更新勾选字段；未勾选保持原表单值
- [ ] AC4：默认勾选「当前预览有值」的字段；无结果维不勾或禁用
- [ ] AC5：应用封面后表单预览为安全 URI 路径（经 cache）；保存仍禁止 data/http 入库
- [ ] AC6：应用歌词后表单歌词区更新；保存可 dirty 进库并标 `lyrics` 保护
- [ ] AC7：全 miss / 网络失败有明确反馈；部分维成功可应用成功维
- [ ] AC8：获取中可取消（关 sheet）且结果不回写；切歌不串曲
- [ ] AC9：保存仍 D4：库成功 + 文件尽力 + Toast 区分；dirty-only
- [ ] AC10：播放中静默在线补缺行为回归不破坏
- [ ] AC11：`npm run lint` + `npm run build` 通过
- [ ] AC12：spec（features-player / component-guidelines）记录编辑云端路径与禁止项

## Task structure

体量适合 **父任务 + 子任务**：

| 子任务 | 可独立验收 |
|--------|------------|
| 云端强制搜 + 多候选编排 API | 无 UI 可测的 search API / 纯函数 |
| 编辑 sheet 云端 UI | 按钮、预览、勾选、应用、接保存 |

父任务持有本 PRD 与集成 AC；实现以子任务为主，父任务做集成验收。

## Notes

- 复杂；需 parent `design.md` + 各 child `implement` 或 parent 总 `implement.md` 映射子任务顺序。
- journal-2 ≈ 1993/2000，收尾可能需 journal-3。
