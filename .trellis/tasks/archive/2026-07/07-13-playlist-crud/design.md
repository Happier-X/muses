# 歌单 CRUD 技术设计

## 边界

| 层 | 职责 |
|----|------|
| `src/features/playlist/*` | 类型、localStorage、CRUD API、事件 |
| `PlaylistsPage.vue` | 列表 + 新建/重命名/删除 UI |
| `PlaylistDetailPage.vue` | 详情 + 移曲 + 播放全部 |
| `SongsPage.vue` | 更多 → ActionSheet → 加队列/加歌单 |
| `router` | `/tabs/playlists/:id` 详情 |

不改 capgo 插件；不改原生层。

## 数据契约

```ts
export type Playlist = {
  id: string
  name: string
  songIds: string[]
  createdAt: string // ISO
  updatedAt: string
}
```

- 存储键：`muses:playlists`
- 事件：`muses:playlists-updated`（与 `muses:songs-updated` 同模式）
- `songIds` 仅存曲库 `SongItem.id`；展示时 `loadSongs()` 映射，缺失则跳过
- 同一 playlist 内 `songIds` **唯一**（append 时去重）

## API（建议）

```ts
loadPlaylists(): Playlist[]
savePlaylists(list: Playlist[]): void // 内部用
createPlaylist(name: string): Playlist | null // 空名 null
renamePlaylist(id: string, name: string): boolean
deletePlaylist(id: string): boolean
addSongToPlaylist(playlistId: string, songId: string): boolean // 已存在 true 且不改序
removeSongFromPlaylist(playlistId: string, songId: string): boolean
getPlaylist(id: string): Playlist | undefined
resolvePlaylistSongs(playlist: Playlist, songs: SongItem[]): SongItem[]
```

id：`pl-` + 时间戳/uuid 片段，与现有 song id 风格一致即可。

## UI 流

### 列表

- 空态：引导新建
- 行点击 → `router.push(/tabs/playlists/:id)`
- FAB 或 toolbar「新建」→ Alert 输入名
- 行更多或长按/按钮：重命名 / 删除（删除 `Alert` 确认）

### 详情

- toolbar 返回 + 标题 = 歌单名
- 「播放全部」按钮（有效曲 > 0）
- 列表点曲 → `playSong`（可选：先把歌单入队再定位；**MVP**：点单曲仅播该曲；「播放全部」才替换队列——与歌曲页点行一致）
- 移曲：行尾移除或滑动删除

### 歌曲更多

- ActionSheet：
  1. 添加到队列
  2. 加入歌单…
  3. 取消
- 「加入歌单…」→ 第二层 ActionSheet/Alert：各 playlist 名 + 「新建歌单」

## 播放全部

对齐 `SongsPage.onShuffleAll` 的非随机版：

```ts
clearQueue()
enqueueSongs(resolvedSongs)
void playSong(resolvedSongs[0])
```

## 兼容与风险

- 旧装机无该 key → 空数组
- 损坏 JSON → 空数组 + 不抛
- 大量歌单/曲：localStorage 足够 MVP；不做分页
- PlayerPage 保活与歌单无关

## 测试

- `tests/unit/playlist.spec.ts`：load/save、create 空名、add 去重、remove、delete
- 可选：SongsPage ActionSheet 浅测（若现有 mount 成本高可只测 feature）
