# PRD：删除音源后清理播放器中的失效歌曲

## 目标

删除音源（或删除歌曲）后，若当前播放/队列中的歌曲属于被删范围，播放器要停止播放并清掉对应展示，而不是继续播着已不存在的歌。

## 根因（代码勘察）

- 删除音源：SourcesPage `executeDeleteSource` → `deleteSource` → `reconcileSourceSongs(id, [])` 从曲库移除歌曲并广播 `muses:songs-updated`
- **播放器 controller 没有订阅该事件**（订阅方只有各页面），运行时 `currentSong`、播放队列仍持有已删歌曲——继续出声、迷你条继续显示

## 需求

在 controller.ts 新增对账逻辑：

1. 订阅 `SONGS_UPDATED_EVENT`（onMounted 级生命周期——controller 是模块级单例，注册一次，用 window 事件即可）
2. 事件到达时对账：
   - `loadSongs()` 构建 songId Set
   - **当前曲失效**（currentSong 存在但不在 Set）→ 调 `stopPlayback()` 完整清理（原生停止/清状态/清媒体会话/#52 守卫语义）
   - **队列清理**：遍历队列 items，songId 不在 Set 的逐个 `removeSongFromQueueInternal` 移除；若移除导致 currentIndex 越界或队列空，由 queue.ts 既有逻辑处理
3. 边界保护：
   - 仅当「歌真的从曲库消失」才触发 stop，不得误伤正常播放
   - 对账过程包 try/catch，失败不影响播放
   - 事件可能由任意写库触发（扫描/编辑等），必须先比对再动作（无失效项则 no-op）

## 验收标准

1. 单测：构造 controller 场景较重，可将对账逻辑抽成纯函数（输入 songs/currentSong/queueItems → 输出应停播与待移除列表）单测覆盖：当前曲失效/仅队列失效/无失效 no-op
2. MuMu 实测：播放 WebDAV 音源歌曲 → 删除该音源 → 播放停止、迷你条消失、队列中该源歌曲被移除
3. 回归：正常扫描/编辑歌曲触发 songs-updated 时播放不受影响
4. lint / test:unit / build 全过（禁止管道吞退出码）

## 范围外

- 不改 deleteSource/reconcileSourceSongs 逻辑
- 不改 MPopup/MiniPlayer UI

## 约束

- 复用现有 stopPlayback 清理链路与 #52 守卫语义
- 密码/敏感信息不入日志
