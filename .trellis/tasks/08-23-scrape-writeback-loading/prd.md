# PRD：写回过程按钮加载态反馈

## 目标

WebDAV 写回耗时长（下载整个音频+写标签+PUT，可达数分钟），点击「确认写回」后界面无任何反馈，用户以为没反应还会重复点。给写回全过程加可见的加载态。

## 现状（代码勘察）

- `ScrapePage.vue` `onWriteback`（:636）：直接 await `applyScrapeChanges`，**无 busy 标记、按钮不禁用、无任何过程提示**
- 「重试失败项」`onRetryFailed`（:661）、「确认撤销」`onConfirmRevert` 同样裸 await
- 重复点击风险：长耗时期间再点会再次触发 applyScrapeChanges

## 需求

1. **R1 写回按钮加载态**：新增 `isWritebackBusy` ref；onWriteback 开始置 true、finally 复位。busy 时：
   - 按钮禁用 + 文案变「写回中…」（可加转圈图标，样式与页面现有 pill 一致）
   - 「返回队列」等结果区其他操作按钮同时禁用（防中途跳转）
2. **R2 重试/撤销同款处理**：`isRetryBusy` / `isRevertBusy` 分别覆盖 onRetryFailed 与 onConfirmRevert
3. **R3 预期管理 toast**：onWriteback 启动时 showToast('开始写回，云端歌曲可能需要较长时间…', duration 较长)；完成/失败由现有结果页展示

## 验收标准

1. MuMu/CDP 实测：点击确认写回后按钮立即变禁用+「写回中…」，期间重复点击无效；完成后恢复并进入结果页
2. 重试与撤销路径同样有禁用反馈
3. lint / test:unit / build 全过（禁止管道吞退出码）

## 范围外

- 不做逐曲进度上报（需原生进度回调，另立任务）
- 不改 writeback.ts 编排逻辑

## 约束

- 仅改 ScrapePage.vue；遵循现有 Salt token 样式体系
