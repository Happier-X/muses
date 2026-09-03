# 设计：刮削预览可见匹配结果并支持编辑

## 1. 背景与问题拆解

```
ScrapeViewModel.startMatching
  → PreviewCandidate(songTitle, currentArtist, matchedTitle/artist/album, coverUrl, confidence, checked)
ScrapeScreen.PreviewStateContent
  → 行内仅：匹配行（title·artist） + 当前 artist [confidence] + checkbox
  问题：无原值/新值分字段对比，无封面可视，无编辑入口 → 用户无法确认“到底改了什么”，也无法细调
```

## 2. 总体策略

- 扩展 `PreviewCandidate` 增加 `currentTitle/currentAlbum` 与可编辑副本字段 `editTitle/editArtist/editAlbum`（或复用 matched* 作为可编辑源，另存 original* 供对比）
- 预览行改为“原→新”分段对比 + 封面缩略 + 编辑按钮；编辑 via `ModalBottomSheet`（`Salt*` 风格，复用 `EditMeta` 文本框样式）
- 编辑弹层与列表解耦：`ScrapeViewModel.updatePreviewEdit(songId, title, artist, album)` 回填行内，写回时优先 `edit* ?: matched*`

## 3. 模块边界与改动点

```
feature:scrape/ScrapeViewModel.kt
  ├─ PreviewCandidate 扩展：currentTitle/currentAlbum/coverUrl 命中展示；editTitle/editArtist/editAlbum 可空（null=未编辑，视为 matched*）
  └─ 新增：updatePreviewItem(songId, title, artist, album) / clearPreviewEdit(songId)

feature:scrape/ScrapeScreen.kt
  ├─ PreviewStateContent 行布局重构：两段对比 + 封面 + 编辑按钮
  └─ 新增：PreviewEditSheet(songId, initialTitle/Artist/Album, onConfirm/onDismiss) BottomSheet

core:scrape 不改写回流程，仅 Preview 模型扩展（若置 core:model 则为 model 层）
```

## 4. 详细设计

### 4.1 数据模型

```kotlin
data class PreviewCandidate(
  val songId: String,
  val songTitle: String,
  val currentTitle: String,      // 新增：原标题（song.title 快照）
  val currentArtist: String?,
  val currentAlbum: String?,
  val matchedTitle: String?,
  val matchedArtist: String?,
  val matchedAlbum: String?,
  val confidence: String?,
  val coverUrl: String?,
  val checked: Boolean = false,
  // 编辑副本：null 表示未编辑，回退 matched*；写回时 resolved = edit ?: matched
  val editTitle: String? = null,
  val editArtist: String? = null,
  val editAlbum: String? = null,
)

fun PreviewCandidate.resolvedTitle() = editTitle ?: matchedTitle
fun PreviewCandidate.resolvedArtist() = editArtist ?: matchedArtist
fun PreviewCandidate.resolvedAlbum() = editAlbum ?: matchedAlbum
```

`startMatching` / `retrySingle` / `retryThrottled` 构造时填充 `currentTitle/Artist/Album = song.*`。

### 4.2 预览行展示

```
[✓] 原：标题 / 歌手 - 专辑          [封面缩略 40dp]
    新：标题 / 歌手 - 专辑  [HIGH]   [编辑]
```
- 原/新各一行，单行 `ellipsis`，新值为空时显示“—”灰色
- 封面：`AsyncImage(coverUrl)` 40dp 圆角，未命中占位 `MusicNote`
- 编辑按钮：`SaltTextButton("编辑")` 常显，点击打开 BottomSheet

### 4.3 编辑弹层

`PreviewEditSheet`（`ModalBottomSheet` + `SaltNavbar`）：
- 三个 `OutlinedTextField`：标题/歌手/专辑，初始值 = `resolved* ?: current*`
- 底部：取消 / 确认；确认时 `viewModel.updatePreviewItem(songId, title.trimOrNull(), artist.trimOrNull(), album.trimOrNull())`
- `trimOrNull` 后空串视为 `null`（回退语义），与 `EditMetaViewModel` 的 `takeIf { it.isNotBlank() }` 一致

### 4.4 写回组装

`confirmWriteback` 中：

```kotlin
changesMap[song.id] = ScrapeChanges(
  title = item.resolvedTitle(),   // edit ?: matched
  artist = item.resolvedArtist(),
  album = item.resolvedAlbum(),
  coverRemoteUrl = item.coverUrl, // 本任务不编辑封面，保留匹配 URL
)
```

保持 `checked` 守卫不变。

## 5. 数据流与时序

```
匹配完成 → Preview(items: current* + matched*)
用户点编辑 → BottomSheet(initial = resolved ?: current)
     → 确认 → updatePreviewItem → Preview(items copy(edit*=new))
     → 取消 → 丢弃
点写回选中 → 取 resolved* 组装 ScrapeChanges → Writing → Result
```

## 6. 兼容与回滚

- `edit*` 新增可空字段，回滚即忽略（写回回退到 `matched*`）
- 行布局重构不影响 `checked` 与批量/限流逻辑
- 无新增网络与 DB 迁移

## 7. 测试策略

- 预览行：原/新分段可视断言，封面缩略加载占位
- 编辑：updatePreviewItem 后 resolved 正确，取消不改，写回用 resolved
- 手测：刮削命中 → 编辑标题 → 勾选写回 → 库内标题为编辑值；未勾选不写回
