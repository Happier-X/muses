# 设计：已缓冲进度条与 seek 限制

## 目标体验

| 场景 | 缓冲条 | 可 seek 范围 | 开播 |
|------|--------|--------------|------|
| 本地就绪文件 | 满 | 全长 | 立即 |
| WebDAV/远程 | 随缓冲增长 | `[0, bufferedPosition]` | 首包可播即播（边播边缓冲） |
| 缓冲未知 | 不画假条 | duration clamp | 现有逻辑 |

## 架构原则

1. **不改** `node_modules/@capgo/*`。
2. 业务层只读 `playerState.bufferedPosition` / `duration` / `position`，通过 `seekPlayback` 统一 clamp。
3. 缓冲数据由 **native 层**产生，经 `AudioPlayerNative` 状态广播进入 controller。
4. 保留 seek 保护窗 + 自然结尾判定（第二道防线）。

## 推荐实现路径（体验最优且可维护）

### 路径判定：优先「边下边播的渐进文件」+ 本地全缓冲

capgo 的 `RemoteAudioAsset` ExoPlayer 实例在插件内部，**项目代码拿不到**其 `bufferedPosition`，除非改插件或反射/替换播放栈（风险高）。

为同时满足：

- 不改 capgo 源码  
- 开播快（非整首下完）  
- 有可靠的「已缓冲到哪」数字  

采用 **WebDAV 渐进下载到缓存文件 + 用文件已写入字节估算缓冲 + 对已写入前缀可 seek**：

```
playSong(webdav)
  → native: 启动渐进下载到 cache 文件（支持 Range 更佳；至少边写边可读）
  → 当文件达到可起播阈值（或 duration 已知且有最小字节）→ 用 file:// 交给 NativeAudio 播放
  → 下载进度线程：bufferedBytes / contentLength → bufferedPosition 秒
  → 前端：缓冲条 + seek clamp
  → 下载完成：bufferedPosition = duration
```

本地源：

```
prepareLocalAudioFile 完成后 → bufferedPosition = duration（或 duration 未知时用 position 不限制并标记 full buffer）
```

### 为何不直接读 ExoPlayer bufferedPosition

- 实例关在 capgo 插件私有字段里。
- 改 `node_modules` 违反项目约束，升级即丢。
- 自建第二套 ExoPlayer 与 capgo 双会话/双通知冲突风险高。

渐进文件方案用**磁盘已写入比例**作为缓冲语义，对「只能拖已缓冲段」足够准确，且可边下边播。

## 数据契约

### PlayerState 扩展

```ts
// controller reactive state
bufferedPosition: number | null  // 秒；null = 未知（不画缓冲条）
// 可选
bufferStatus: 'unknown' | 'partial' | 'full'
```

### AudioPlayerNativeState

已有 `bufferedPosition?: number`。约定：

- 单位：秒  
- 单调不减（同一 asset）  
- stop/切歌后清零或省略  
- 本地 full：`bufferedPosition >= duration`（duration>0）

### 原生上报（建议）

在 `AudioPlayerPlugin`（或独立 `PlaybackBuffer` 插件，优先扩展现有 AudioPlayer）增加：

```ts
// 可选：前端轮询兜底
getBufferState(): Promise<{ songId?: string; bufferedPosition: number; duration?: number; fullyBuffered: boolean }>

// 更好：事件
addListener('bufferProgress', (e: { songId: string; bufferedPosition: number; duration?: number; fullyBuffered: boolean }) => void)
```

下载侧：

- `Content-Length` 已知：`bufferedPosition ≈ (written / contentLength) * duration`
- duration 未知：先只上报 `bufferedRatio`，duration 就绪后换算；或 duration 未知时缓冲条用比例宽度、seek 仍用字节安全区（实现选一种写进 implement）
- 无 Content-Length：退化为「未知缓冲」或「仅 fullyBuffered 二值」（尽量用 HEAD/第一次响应拿 length）

### seekPlayback

```ts
const maxSeekable = state.bufferedPosition != null && state.bufferedPosition >= 0
  ? Math.min(state.duration || Infinity, state.bufferedPosition)
  : state.duration || Infinity
const safe = clamp(position, 0, maxSeekable)
```

歌词点击共用 `seekPlayback`，越界即 clamp 或拒绝（PRD：拒绝不跳转；实现可用 clamp-to-end-of-buffer 并 toast「已跳到已缓冲终点」——**默认：拒绝 + 不 seek**，更符合「只能在那段调」）。

## UI

`PlayerPage` 进度区域：

```
[========播放====|----缓冲----|..........未缓冲..........]
```

实现建议：

- 外层 track 容器 + 绝对定位缓冲层（宽度 `buffered%`）+ 现有 range 的 `--progress`
- range 的 `max` 仍为 duration（视觉全长）；在 `input/change` 时 clamp
- 或 `max = maxSeekable`（拖不到未缓冲，但总时长刻度会变短）——**推荐保留 max=duration + 视觉缓冲层 + 逻辑 clamp**，更像视频站

CSS 变量示例：

- `--progress`：已播放 %
- `--buffered`：已缓冲 %

## 状态机要点

| 事件 | bufferedPosition |
|------|------------------|
| playSong 开始 | null 或 0 |
| 本地 ready | = duration |
| 远程下载进度 | 增长 |
| 下载完成 | = duration |
| stop / 失败 | null |
| 切歌 | 先清再设新曲 |

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 渐进下载未完成时 MediaPlayer/Exo 对增长中的 file 支持差 | 使用 capgo 对 file:// 的路径；必要时下载到临时文件并在「可播阈值」后再 preload；测试 Android 真机 |
| duration 晚到导致缓冲秒数不准 | duration 更新后按比例重算；UI 用 min(buffered, duration) |
| 双下载（元数据缓存 + 播放缓存） | 复用 `WebDavAudioCache` 路径/key，避免同 URL 下两份 |
| 首播仍慢 | 可起播阈值尽量小（如 256KB 或 2s 估算），并行下完剩余 |

## 测试策略

- 单元：controller clamp、歌词拒绝、切歌重置、本地 full buffer  
- 组件：进度条 style 含 `--buffered`  
- 真机：WebDAV 大文件边播边拖；拖未缓冲区不切歌  

## 回滚

关闭缓冲限制 feature flag（若加）或 revert 原生下载路径，回退到仅 duration clamp + finished 防护。
