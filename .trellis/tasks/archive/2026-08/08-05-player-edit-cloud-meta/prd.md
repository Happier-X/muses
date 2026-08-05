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
3. **多候选**：文本 / 封面 / 歌词均可换候选；封面应用时 `cacheRemoteCover`；歌词应用主词 + format。
4. **分字段应用**：仅勾选字段写入表单。
5. **保存**：不改 D4；dirty-only → `userEditedFields`。
6. **反馈 / 并发 / 无障碍**：分维状态、Abort、aria-label。
7. **播放路径不变**。

## Out of Scope

- 新平台、登录、付费 API、iOS 写标签
- 打开编辑自动搜、MiniPlayer/列表编辑入口
- 改播放静默补空策略、云端拉 RG、未确认直接改表单

## Acceptance Criteria

- [x] AC1：编辑 sheet 有「从云端获取」；打开编辑**不会**自动发起云端请求
- [x] AC2：获取后展示文本/封面/歌词预览（有结果时）；可切换该维其它候选且**不**改表单
- [x] AC3：分字段勾选 +「应用到表单」只更新勾选字段；未勾选保持原表单值
- [x] AC4：默认勾选「当前预览有值」的字段；无结果维不勾或禁用
- [x] AC5：应用封面后表单预览为安全 URI 路径（经 cache）；保存仍禁止 data/http 入库
- [x] AC6：应用歌词后表单歌词区更新；保存可 dirty 进库并标 `lyrics` 保护
- [x] AC7：全 miss / 网络失败有明确反馈；部分维成功可应用成功维
- [x] AC8：获取中可取消（关 sheet）且结果不回写；切歌不串曲
- [x] AC9：保存仍 D4：库成功 + 文件尽力 + Toast 区分；dirty-only
- [x] AC10：播放中静默在线补缺行为回归不破坏（match* 无 diff）
- [x] AC11：`npm run lint` + `npm run build` 通过
- [x] AC12：spec（features-player）记录编辑云端路径与禁止项

## Task structure

| 子任务 | 状态 |
|--------|------|
| `08-05-edit-cloud-meta-api` | 已归档 |
| `08-05-edit-cloud-meta-ui` | 已归档 |

## Notes

- 实现见 `src/features/editMeta` + `PlayerPage.vue` 云端区块。
