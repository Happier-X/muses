# 设计：编辑歌曲信息接入云端元信息

## 边界

| 层 | 职责 |
|----|------|
| `features/metadata|cover|lyrics` | 编辑用 search API：强制搜、收集多候选；播放 match* 不动语义 |
| `PlayerPage` 编辑 sheet | 获取按钮、预览/候选、勾选、应用表单 |
| `controller.saveCurrentSongUserEdit` | 不变；应用后的 dirty 照旧保存 |
| 原生 `cacheRemoteCover` | 仅应用封面时调用 |

## 数据流

```
[从表单读 query] → searchEditCloudMeta(query)
    → { textCandidates, coverCandidates, lyricsCandidates, statuses }
→ UI 预览选中 index + 勾选 set
→ 用户「应用到表单」
    → setFieldValue / editCoverUri(+cacheRemoteCover) / lyrics
→ 用户保存 → saveCurrentSongUserEdit(dirty patch)
```

## API 草图（api 子任务落地）

```ts
type EditCloudMetaQuery = {
  songId: string
  title: string
  artist?: string
  album?: string
  durationSec?: number
}

type EditCloudMetaResult = {
  text: { status: EditDimStatus; items: TextMetaHit[]; defaultIndex: number }
  cover: { status: EditDimStatus; items: { remoteUrl: string; source: string }[]; defaultIndex: number }
  lyrics: {
    status: EditDimStatus
    items: { text: string; format: SongLyricsFormat | string; source: string; translationText?: string }[]
    defaultIndex: number
  }
}
```

- 实现可选：`searchEditCloudMeta` 并行三路，或分 `searchEditTextCandidates` 等。
- 文本：遍历 providers，每源取 top-K（改 pickBest 为返回列表），合并去重（normalize title+artist+album）。
- 封面：不 first-stop；每源最多 1～N URL，合并去重 URL。
- 歌词：不 first-stop；每源命中 push；注意正文体积，UI 只存引用+预览切片。
- 取消：`AbortSignal` 优先；无则调用方 token。

## UI 草图（ui 子任务）

- 编辑 sheet 顶部或表单上：`从云端获取` + 状态文案。
- 获取成功后区块「云端结果」：
  - 三维：当前候选摘要（封面缩略图、文本三行、歌词来源+前 N 字）
  - 「更换」展开列表
  - `HCheckbox`（或等价）× 五字段 + `应用到表单`
- 应用中封面下载失败：该字段应用失败提示，其它勾选仍可应用。
- 样式：沿用 BottomSheet + happier-ui；沉浸 ink 色注意可读（sheet 在 surface 上，用默认 ink，勿强制 immersive 白字）。

## 歌词 translation

当前编辑表单仅单块 LRC 文本。MVP：

- 应用主词 `text` + 保存时 `lyricsFormat`。
- 若 hit 含 `translationText`：能合并进可解析结构则 design 实现时选「追加 tlyric 约定」或「暂不应用译词」——**推荐 MVP：主词 only，译词留候选详情只读**，避免污染单文本框；二期再做双框。

## 兼容与回归

- 播放 `matchOnline*` / prefetch / userEdited 门闸：不调用编辑 search，或编辑 search 独立导出。
- 负缓存：编辑强制搜**不要**误伤播放负缓存键；可用独立 cache key 前缀 `edit:` 或编辑路径跳过读播放负缓存。

## 风险

| 风险 | 缓解 |
|------|------|
| 多源全拉超时 | 每源超时、总超时、并行维、UI 可关 sheet 取消 |
| 歌词多全文内存 | 候选上限（如每维 ≤8）、列表虚化预览 |
| Sheet 过长 | 云端区可折叠；候选用二次 sheet/列表 |
| 误 start 父任务 | 父任务只集成；实现 start 子任务 |

## 回滚

- 特性集中在新 API + PlayerPage 云端区块；回退提交即可；无迁移。
