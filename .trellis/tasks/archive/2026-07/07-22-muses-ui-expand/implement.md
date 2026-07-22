# 执行计划：逐个扩展 Muses UI

## 前置

1. 用户审阅 `prd.md` + `design.md` + `implement.md`
2. `task.py start` 后才写代码
3. 实现前读 `.trellis/spec/frontend`（component / directory）

## 批次顺序（必须按序，完成一批再下一批）

### 批次 1 — MIconButton

- [x] 若需：token 增补 touch-target / icon-size
- [x] 新增 `src/components/ui/MIconButton.vue` + `index.ts` 导出
- [x] 迁移：`MiniPlayer` 播放控件；`SongsPage` / `PlaylistDetail` more；`QueuePage` 移除按钮（至少 2 处）
- [x] lint 相关文件

### 批次 2 — MListRow

- [x] 新增 `MListRow.vue`
- [x] 迁移：`SongsPage` 虚拟行内容；`QueuePage` 队列行；`PlaylistDetailPage` 行（≥2 页）
- [x] 保持 playing / jump-highlight 行为
- [x] 相关单测若有则更新

### 批次 3 — Queue 整页收敛

- [x] empty → MEmptyState；cover 已在 row 内
- [x] 间距 / 背景 token
- [x] 目视：空队列、移除、当前曲高亮

### 批次 4 — MSettingRow + Settings

- [x] 新增 `MSettingRow.vue`
- [x] `SettingsPage` 行迁移
- [x] toggle 仍 ion-toggle 于 slot

### 批次 5 — Spec 与 DESIGN 轻量同步

- [x] `component-guidelines.md`：组件表 + ion 直连白名单
- [x] `directory-structure.md`：ui 文件列表
- [x] `DESIGN.md` Components 节补 MIconButton / MListRow / MSettingRow

### 批次 6 — 全量验证

- [x] `npm run lint`
- [x] `npm run build`
- [x] `npm run test:unit -- --run`
- [ ] 手动清单：列表 playing、Queue、Settings 开关、MiniPlayer、无阴影（需真机/模拟器目视）

## 建议 commit

1. `feat(ui): 新增 MIconButton 并迁移图标触控`
2. `feat(ui): 新增 MListRow 并迁移曲目/队列行`
3. `refactor(views): Queue 页收敛到 M* 组件`
4. `feat(ui): 新增 MSettingRow 并迁移设置页`
5. `docs(spec): 扩展 Muses UI 与 ion 直连边界`

## 回滚

按 commit 批次 revert；优先保证播放与队列逻辑零行为变化。
