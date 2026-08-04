# 设计 — 沉浸页更多菜单与编辑歌曲信息

## 1. 边界

| 层 | 职责 |
|----|------|
| `PlayerPage.vue` | mode-bar 更多键、操作 sheet、打开编辑 UI；不直接散落原生调用 |
| 编辑 UI | 表单展示/校验/提交；可与 Player 同文件起步，字段多则抽 `SongEditSheet.vue`（app 内组件，非 happier-ui） |
| `features/library` | `SongItem.userEditedFields`、sanitize、upsert 合并保护字段、`updateSongUserMetadata`（或等价）写库 API |
| `features/player` | 保存后 `syncDisplayStateFromSong`、媒体会话、RG `setVolume`；扫描/在线/预取路径尊重保护 |
| Android 插件 | 新增写元数据：local SAF 写回、webdav 缓存写 tag + 前端/插件 PUT |

## 2. 数据契约

### 2.1 `userEditedFields`

```ts
type UserEditedField = 'title' | 'artist' | 'album' | 'cover' | 'lyrics' | 'replayGain'

// SongItem 新增可选：
userEditedFields?: UserEditedField[]
```

- 保存时：对用户本次提交且意图保留的字段 **union** 进数组（清空歌词/RG 若仍算手改，也标记保护，避免扫描写回旧值）。
- `hasSongChanged` / `isSongItem` / `sanitizeSongForStorage` 必须识别该字段。
- **合并规则**：任何自动 `upsertSong`（扫描 tags、在线补缺）在写入前：若 `field ∈ userEditedFields`，则 **不得**用自动 tags 覆盖该字段（保留库内用户值）。

### 2.2 写库 API（建议）

```ts
// 伪契约
updateSongUserEdit(songId, {
  title, artist?, album?,
  coverUri?, // 已是安全 file:// 或清除
  lyrics?, lyricsFormat?, // 用户粘贴默认 lrc；空=清歌词
  replayGainTrackDb?: number | null, // null=清除标签语义
}): SongItem
```

- 内部：`loadSongs` → 按 id 找 → 写字段 + 更新 `userEditedFields` + `updatedAt` → `saveSongs` → 返回 song。
- 也可扩展 `upsertSong` 增加 `preserveUserEdited: true` 与显式 patch；优先**专用函数**避免扫描路径误用。

### 2.3 保护点清单（必须全部挂钩）

1. `controller.scanSongMetadata` → upsert 前 strip 受保护字段的 tags  
2. 在线文本 `matchOnlineTextMetaForSong`  
3. 在线封面 `matchOnlineCoverForSong`（`cover` 受保护则整段跳过）  
4. 在线歌词质量写回（`lyrics` 受保护则不 `upsert` 歌词）  
5. `scanner` / `reconcile` 批量 upsert  
6. `prefetchNextMetadata` 写库  

实现可用共享 helper：

```ts
applyTagsRespectingUserEdits(song: SongItem, tags: AudioTags): AudioTags
```

## 3. UI / 交互

### 3.1 mode-bar

```
[repeat] [shuffle] [queue] [more]
```

- 更多：`ellipsisVertical`，`aria-label="更多"`  
- 点击 → `isPlayerActionsOpen = true`  
- 样式：沿用沉浸 ghost；`mode-bar` `max-w` 可提到 ~320px，矮屏断点同步

### 3.2 操作 sheet（D2）

- `HBottomSheet` title「歌曲操作」  
- 一项：`编辑歌曲信息` → 关操作 sheet，开编辑 sheet  
- 一项：取消  

### 3.3 编辑 sheet

- 第二层 `HBottomSheet`（或更高 `detent`/全高），title「编辑歌曲信息」  
- 字段：
  - title `HInput` 必填  
  - artist / album `HInput` 可选  
  - 封面：当前预览 +「选择图片」+ 可选「清除封面」；选图经 Capacitor 相册/文件（若无现成插件，用 `input[type=file] accept=image/*` + 原生拷到 covers 缓存 API，design 实现时选成本最低路径）  
  - 歌词 `HTextarea` 大文本；提示「LRC 文本」；清空=无词  
  - ReplayGain `HInput` type 文本/数字，单位 dB；占位「如 -6.5」；空=清除 RG  
- 底部：取消 / 保存（保存中 disabled）  
- `@tanstack/vue-form`，`validators.onSubmit` only  

### 3.4 保存反馈

| 结果 | 提示 |
|------|------|
| 库成功 + 文件成功 | 已保存 |
| 库成功 + 文件失败/不支持 | 已更新曲库，写入音频文件失败（可附短因） |
| 库失败 | 保存失败，不写文件 |

## 4. 原生写标签

### 4.1 API 草图

```ts
writeAudioMetadata(options: {
  sourceType: 'local' | 'webdav'
  uri: string
  // local: content uri; webdav: 远程 url 或本地缓存 path + 认证由插件/前端约定
  title?: string
  artist?: string
  album?: string
  // cover: 本地文件 path/uri 或跳过
  // lyrics: 字符串
  // replayGainTrackDb?: number | null
}): Promise<{ ok: boolean; code?: string; message?: string }>
```

### 4.2 Local（SAF）

1. `content://` → 复制到 cache 临时文件（与 read 类似）  
2. jaudiotagger `AudioFileIO.read` → set FieldKey TITLE/ARTIST/ALBUM；歌词 LYRICS；封面 artwork；RG 写 TXXX `REPLAYGAIN_TRACK_GAIN`（`xx.xx dB`）  
3. `AudioFileIO.write`  
4. 将临时文件字节写回 Document：`contentResolver.openOutputStream(uri, "wt")` 或 DocumentFile；失败返回 `not_writable`  
5. **播放中**：若写失败，前端已按 D4 只提示  

### 4.3 WebDAV

1. 确保有完整本地缓存文件（可复用 getCached / 再下载）  
2. 同 jaudiotagger 写临时/缓存文件  
3. HTTP PUT 回远程路径（Basic Auth）；失败 `put_failed`  
4. 密码不进日志 / reactive state  

### 4.4 格式

- 优先保证 **mp3 / flac / m4a** 常见容器；其余尽力，失败归 D4  
- 不支持容器：返回 `unsupported_format`，库仍已保存  

## 5. 封面 / 歌词 / RG 细则

| 字段 | 库 | 文件尽力 | 保护 key |
|------|-----|----------|----------|
| 文本三元组 | 必写 | FieldKey | title/artist/album |
| 封面 | 安全 `file://` covers 缓存；禁 data/远程入库存 | 嵌入 artwork | cover |
| 歌词 | 正文 + `lyricsSource` 建议 `sidecar` 或新语义 `user`（若类型扩展成本高，用 embedded + 保护字段即可，**质量序不得覆盖**） | 内嵌 LYRICS 或同目录 `.lrc` 写（sidecar 写 SAF 更复杂，MVP **优先内嵌**，失败仅库） | lyrics |
| RG | `replayGainTrackDb` number \| undefined | TXXX | replayGain |

- 歌词 format：用户粘贴默认 `lrc`；若可检测 ttml/yrc 再设（MVP 可固定 lrc）  
- 在线歌词：`lyrics ∈ userEditedFields` → 跳过质量写回  
- 封面：`cover ∈ userEditedFields` → 跳过在线封面  

## 6. 与播放展示同步

保存返回 `SongItem` 后：

1. 若 `currentSong.id` 匹配：`syncDisplayStateFromSong`  
2. 刷新 `state.lyrics` / format / translation 策略：用户歌词替换运行时词；AMLL 依赖现有 key 重建  
3. `setVolume(resolvePlaybackVolume(song))` 若 playing/paused  
4. 媒体会话 metadata 再 sync  

## 7. 兼容与迁移

- 旧 `SongItem` 无 `userEditedFields` → 视作 `[]`，行为与今相同  
- 不改 song id / path / uri  

## 8. 回滚

- 功能开关无需；回滚提交即可  
- 原生写失败不影响库（D4），数据层可逆性依赖用户再次编辑  

## 9. 测试要点（手工 / 后续）

- 更多 → 编辑 → 改 title → 保存 → 沉浸标题与列表（重进）一致  
- 改后切歌再切回，懒扫不恢复旧 title  
- 无写权限文件：库变、Toast 失败  
- RG 修改后播放音量变化（均衡开）  
- 保护歌词后在线匹配不覆盖  
