# Implement — 媒体通知延迟与退出后丢失

## 前置

- 已确认 D1/D2/D3
- 规范：`.trellis/spec/frontend/features-player.md`
- 不修改 `node_modules/@capgo/*`

## 执行清单

### 1. 退出界面改为最小化

- [x] `src/App.vue`：Tab 层返回键 `App.exitApp()` → `App.minimizeApp()`
- [x] 保持 overlay（player/queue）优先关闭逻辑不变
- [x] 失败时（非 Android）静默兜底，避免抛未实现错误打断 UI

### 2. MediaSession 状态映射

- [x] `src/features/player/mediaSession.ts`：`updateMediaSessionPlayback`
  - `playing` → `playing`
  - `paused` → `paused`
  - `loading` + 有活跃曲目语境 → 保持 `playing`（或调用方传入 active）
  - 其余 → `none`
- [x] 确认 `clearMediaSession` / `stopPlayback` 路径仍会清到 `none` 并移除通知

### 3. Metadata 文字先上、封面后补

- [x] `updateMediaSessionMetadata` 先无 artwork 推送 title/artist/album
- [x] 异步 `prepareArtworkDataUrl` 成功后二次 `setMetadata`
- [x] 用递增 token / song 标识丢弃过期封面结果，避免快切串封面

### 4. 控制器联动核对（尽量少改）

- [x] 核对 `controller.ts` 的 `syncMediaSessionSong` / `syncMediaSessionState` 是否因 2/3 已足够
- [x] 仅在必要时：`playSong` loading 阶段显式推一次 playback state（避免依赖遗漏）
  - 结论：`syncMediaSessionSong` 在 loading 时已调用 `updateMediaSessionPlayback`，无需再改 controller

### 5. 规范回写（实现完成后 / finish 前）

- [x] 更新 `.trellis/spec/frontend/features-player.md`：
  - 返回键最小化语义
  - loading 不得映射 none
  - metadata 两段式更新

## 验证

### 命令

```bash
npm run lint
npm run build
cd android && ./gradlew :app:compileDebugKotlin
```

（有条件时再 `assembleDebug` 装真机）

### 手测清单

1. 本地曲：点播 → 通知尽快出现（可无封面）→ 封面稍后补上
2. 切歌：通知不消失重开；标题先变
3. 播放中按返回 → 回桌面 → 通知仍在 → 可暂停/继续/上下曲
4. 通知 stop 或 App 内停止 → 通知消失
5. WebDAV 曲重复 1–4
6. 确认只有一条媒体通知

## 风险点 / 回滚点

| 点 | 回滚 |
|----|------|
| `App.vue` minimize | 改回 `exitApp` |
| mediaSession 映射/封面 | 恢复单次 setMetadata + 原 none 映射 |

## Start 门槛

- [x] `prd.md` 收敛
- [x] `design.md`
- [x] `implement.md`
- [ ] 用户审阅通过后 `task.py start`
