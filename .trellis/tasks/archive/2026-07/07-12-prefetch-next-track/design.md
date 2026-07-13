# 设计：预取下一首 WebDAV 歌曲

## 边界

| 层 | 职责 |
|----|------|
| `queue.ts` | 新增 `peekNext()`，不改索引 |
| `controller.ts` | 播放成功后调度预取；队列/模式变化时重调度 |
| `native.ts` | 解析完整缓存；未命中远程直链；触发后台预取 bridge |
| `AudioPlayerPlugin.kt` + `WebDavAudioCache` | 完整缓存查询 / 后台完整下载 / 同 URL 会话复用 |

## 数据流

### 预取

```
playSong(current) 成功 → playing
  → next = peekNext()
  → if next is webdav and next.id !== current.id
       resolve password
       AudioPlayerBridge.prefetchWebDavAudioFile({ url, username, password, songId })
  → 原生：getCachedFile 命中则 no-op；否则 downloadInBackground（完整下载）
```

### 切歌播放

```
playSong(webdav)
  → try getCachedWebDavAudioFile(url)
  → if complete file:
       play file://
       bufferedPosition = full
  → else:
       play remote URL + Basic Auth
       bufferedPosition = null
  → 然后调度 peekNext 预取
```

## 合约

### queue

```ts
export const peekNext = (): SongItem | null
```

语义与 `advanceToNext` 相同，但不写 `currentIndex` / 不 refresh 持久化副作用。

### native bridge（建议）

```ts
prefetchWebDavAudioFile?(options: {
  url: string
  username: string
  password: string
  songId: string
}): Promise<{ cached: boolean; started: boolean }>

getCachedWebDavAudioFile?(options: {
  url: string
}): Promise<{ uri: string | null }>
```

- `cached=true`：已有完整缓存，无需下载
- `started=true`：已启动后台完整下载
- `getCached...` 仅返回完整文件 URI；partial 返回 null

### 并发策略

- 旧预取不取消；新下一首可并行启动。
- 原生 `WebDavAudioCache` 对同 URL 复用下载会话/已有缓存，避免重复写。
- 前端可用 `activePrefetchSongId` 仅作诊断/去重提示，不能作为正确性唯一依据。

## 安全

- 密码只在 controller 解析后传入 bridge，不进入 reactive state。
- 不 log password / Authorization header。
- 缓存文件在 app cache 目录，受现有 trim 策略约束。

## 与 #14 的兼容

- 播放路径仍禁止 progressive partial。
- 仅当 `getCachedFile` 返回完整文件时才本地播放。
- 未完成下载永远远程直链。

## 风险

| 风险 | 缓解 |
|------|------|
| 并行下载抢带宽 | 仅下一首；完整缓存命中后 no-op |
| 队列快速变化浪费流量 | 产品接受旧下载继续；同 URL 复用 |
| 把 partial 当完整 | getCachedFile 只认完整目标文件，不认 .partial/.tmp |
| 单曲循环自预取 | peekNext 后若 next.id === current.id 跳过 |

## 回滚

- 去掉 controller 预取调用与 native 缓存优先分支，即可回到纯远程直链。
