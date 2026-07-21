# 统一同语义图标（队列 / 歌单等）

## Goal

修复 GitHub **#44**：同一语义在不同入口使用同一 Lucide 适配导出，避免用户感知「同功能不同图标」。

## Background

业务图标已统一经 `src/icons/ion-lucide.ts`，但导出名历史混用 ionicons 语义，调用点未严格对齐：

| 语义 | 现状不一致 | 期望 |
|------|------------|------|
| 打开播放队列 | MiniPlayer：`list`（ListMusic）；PlayerPage 队列键：`listOutline`（List） | 统一 **`list`（ListMusic）** |
| 歌单 Tab / 歌单列表占位 | Tabs「歌单」：`list`（ListMusic）；PlaylistsPage 行图标：`listOutline`（List） | 统一 **`list`（ListMusic）** |
| 顺序播放（非随机） | PlayerPage：`listOutline`（List） | **保持 `listOutline`**（与队列/歌单不同语义） |
| 列表循环 / 单曲循环 | `repeatOutline` / `repeat` | 保持可区分（不在本任务改形态） |
| 播放主控 fill | 已在 #45 处理 | 不回退 |

`musicalNotes` 与 `musicalNotesOutline`、`add` 与 `addOutline` 在适配层已是同一 Lucide 几何的别名，视觉已一致；本任务不强制改名，仅在规范中写明别名关系。

## Related Issues

- https://github.com/Happier-X/muses/issues/44

## Requirements

### R1. 语义 → 图标映射（MVP）

在代码与 `component-guidelines` 中固定：

| 语义 | 导出符号 | Lucide |
|------|----------|--------|
| 打开队列 | `list` | ListMusic |
| 歌单导航 / 歌单占位 | `list` | ListMusic |
| 顺序播放模式（shuffle off） | `listOutline` | List |
| 随机播放模式 | `shuffle` | Shuffle |
| 列表循环 / 单曲循环 | `repeatOutline` / `repeat` | Repeat / Repeat1 |
| 播放主控 | `play` / `pause` / `playSkip*`（fill） | 既有 |
| 列表次级播放 | `playOutline` | 既有 outline |

### R2. 调用点修正

- `MiniPlayer` 队列按钮：保持 `list`（已正确）。
- `PlayerPage` 打开队列：`listIcon` 从 `listOutline` 改为 **`list`**。
- `PlaylistsPage` 歌单行图标：从 `listOutline` 改为 **`list`**。
- `TabsPage` 歌单 Tab：保持 `list`。
- 顺序/随机模式图标逻辑不变（仍 `shuffle` vs `listOutline`）。

### R3. 规范

- 更新 `component-guidelines`：补充「同语义同图标」表；明确 `list` vs `listOutline` 不得混用（队列/歌单 vs 顺序播放）。
- `ion-lucide.ts` 文件头对照表如与上表冲突则同步。

### Out of Scope

- **#46** 响度/音量归一
- 全库审美重绘、换全新图标集
- 原生通知栏图标
- 发版

## Acceptance Criteria

- [x] MiniPlayer 与 PlayerPage「打开队列」使用同一导出 `list`
- [x] Tabs 歌单与 PlaylistsPage 行占位使用同一导出 `list`
- [x] 顺序播放仍用 `listOutline`，与队列图标可区分
- [x] 无业务侧 `ionicons/icons` 回退
- [x] component-guidelines（及必要时 ion-lucide 注释）已同步
- [x] lint / 相关 unit 通过
- [x] 关闭或评论 #44

## Notes

- 轻量偏中：改动点少但需写清语义表，避免回归。
- 建议 `design.md` + 短 `implement.md` 后 start。
