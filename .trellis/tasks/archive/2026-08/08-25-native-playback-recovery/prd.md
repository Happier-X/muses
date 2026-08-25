# 播放失败自动恢复与安全错误文案（父任务子任务）

> 背景：Web `controller.ts` 有完整的播放失败恢复链（损坏队列不卡死、安全错误文案、恢复进度保护），原生侧 ExoPlayer 出错直接停住且无错误状态暴露。全部落在 core/media，与 UI 会话零冲突。

## Goal

- 播放失败自动恢复：onPlayerError 时沿当前生效顺序向后查找未尝试候选，最多回绕一次，跳过 attempted 集合；无可播候选才停止并暴露错误
- 安全错误文案：ExoPlayer 错误码 → 白名单人话文案；未知错误统一「播放失败，请稍后重试。」不泄露内部信息
- `PlayerConnection.playbackError: StateFlow<String?>` 暴露（P4 播放页一行接线消费）

## 方法论

1. **Web=规格书**：controller.ts 的恢复链语义（attempted 去重、回绕一次、safe 文案白名单 8 条）逐条对齐
2. **平台差异显式记录**：Web 的 #53 resume-seek-guard 是 WebView 轮询时代的 UI 闪烁防护——native 恢复用 `setMediaItems(items, index, positionMs)` 原子定位，天然无此问题，不移植（决策 D2）
3. **隔离**：只动 `core/media`；`feature/player` 的 ViewModel 接线留给 P4

## 批次

| 批次 | 内容 |
|---|---|
| R0 | `PlaybackRecoveryController`：attempted 管理 + 候选选择 + 错误码→文案映射 |
| R1 | PlaybackService/PlayerConnection 接线：onPlayerError → 恢复链；`playbackError` StateFlow |

## Acceptance Criteria

- [ ] 候选选择：沿 active order 向后回绕一次、跳过 attempted、无候选返回 null
- [ ] 文案映射：IO_FILE_NOT_FOUND→「音频文件不存在或已失效…」、AUTHENTICATION_FAILED→WebDAV 认证失败文案、网络类→检查网络文案、未知→兜底
- [ ] 用户主动切歌/队列变更时清空 attempted 与 error 状态
- [ ] 单测：候选选择（含回绕/全跳过）、文案映射各分支
- [ ] 全量门禁绿；不修改 feature/* 与 nativem1

## 关键决策

- **D1 恢复逻辑落服务侧**：PlaybackService 持有 player，MediaController 侧只读状态
- **D2 不移植 #53 resume-seek-guard**（2026-08-25）：native `setMediaItems(items,index,pos)` 原子定位无中间态
