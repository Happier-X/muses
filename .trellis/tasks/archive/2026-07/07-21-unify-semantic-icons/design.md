# 设计：同语义图标统一

## 问题根因

适配层故意用两个 Lucide 图标区分历史 ionicons 名：

- `listOutline` → `List`（通用列表线框）
- `list` → `ListMusic`（音乐列表 / 队列 / 歌单）

业务侧把「打开队列」「歌单占位」部分写成了 `listOutline`，与「顺序播放」共用 List，导致：

1. 队列入口两处图标不一致（MiniPlayer vs PlayerPage）
2. 歌单 Tab 与歌单列表行不一致
3. 「顺序播放」与「打开队列」若都用 List 会更难辨认

## 决策

| 导出 | Lucide | 唯一允许的业务语义 |
|------|--------|-------------------|
| `list` | ListMusic | 打开队列；歌单 Tab；歌单列表占位 |
| `listOutline` | List | **仅**顺序播放模式（shuffle off） |
| `shuffle` | Shuffle | 随机播放模式 |
| `repeat` / `repeatOutline` | Repeat1 / Repeat | 单曲 / 列表循环（状态对） |

不改 Lucide 选型，只改误用调用点 + 规范。

## 代码改动面

1. `src/views/PlayerPage.vue`  
   - import 增加 `list`  
   - `const listIcon = list`（原 `listOutline`）  
   - `shuffleIcon` 仍用 `listOutline` 表示顺序

2. `src/views/PlaylistsPage.vue`  
   - 行图标 `listOutline` → `list`  
   - import 调整

3. `src/icons/ion-lucide.ts`  
   - 注释表写清上述唯一语义（可选小改）

4. `.trellis/spec/frontend/component-guidelines.md`  
   - Icon Conventions 增加同语义映射表  
   - MiniPlayer / PlayerPage 队列图标约定改为 `list`

## 不改

- MiniPlayer / Tabs 已正确的 `list`
- 模式键、主控 fill、playOutline 次级播放
- 导出名重命名（避免大规模 rename；靠注释 + 调用点纪律）

## 测试

- 无强制 UI 截图测试；`npm run lint` + 既有 unit
- 手工：MiniPlayer 队列 vs 沉浸页队列图标一致；歌单 Tab 与列表行一致；顺序/随机切换仍可区分

## 回滚

还原 PlayerPage / PlaylistsPage import 与 `listIcon` 赋值即可。
