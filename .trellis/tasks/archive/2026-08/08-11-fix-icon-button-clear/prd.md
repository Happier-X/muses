# PRD：图标按钮规范（k-button + clear）补漏

## 背景

用户指出："图标按钮应该是 k-button 配合 clear 才行"。经查证 Konsta 5.3.0 源码确认规范，并全项目扫描排查漏用 clear 的纯图标按钮（修复已完成于 commit 2013a30，本任务为留档补记）。

## 规范（写入认知）

k-button 有 5 种 style，**纯图标按钮必须 `clear`**（否则默认 `fill` 渲染为实心主题色蓝底）：

| style | 效果 | 适用 |
|---|---|---|
| fill（默认） | 实心主题色（蓝底白字） | 主操作按钮 |
| outline | 描边 | 次级操作 |
| **clear** | 透明底 + 文字色 | **图标按钮** |
| tonal | 浅色填充 | 弱化操作 |

## 排查结果（全项目 k-button × 图标）

**漏 clear（已修，commit 2013a30）**：
- `PlaylistDetailPage.vue:61`「从歌单移除」（removeCircleOutline 图标）→ 补 `clear`
- `PlaylistsPage.vue:31`「更多歌单操作」（ellipsisVertical 图标）→ 补 `clear`

**已正确**：PlayerPage 播放控制键（上一曲/播放/下一曲/循环/更多等）均已带 clear；带文字的 k-button（"应用到表单"、"提交"、编辑表单按钮等）保持 fill 属正确主操作样式。

## 验收（全部通过）

- [x] 两处纯图标按钮补 `clear`，与项目其余图标按钮一致（透明底）
- [x] `npm run build` 通过
- [x] 全项目扫描确认无其他遗漏（L444/L671 为带文字主操作按钮，fill 合理）

## 备注

- 本任务为补记留档：修复与验证在任务创建前已完成（fix commit 2013a30）