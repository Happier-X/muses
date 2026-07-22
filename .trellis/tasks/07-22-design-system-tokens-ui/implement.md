# 执行计划：Token 化与 Muses UI 组件库

## 前置门禁

1. **归档** `07-22-impeccable-init`（含 PRODUCT/DESIGN/AGENTS/主题 primary/sidecar 的 commit + `task.py archive`）
2. 用户审阅通过本任务的 `prd.md` + `design.md` + `implement.md`
3. `task.py start` 后才写业务代码

## Checklist

### 0. 前置

- [ ] impeccable-init 已 commit 并 archive
- [ ] 当前任务 `task.py start` → `in_progress`

### 1. Token 层

- [ ] 新增 `src/theme/tokens.css`（色/间距/圆角/字号/动效/z-index/断点/immersive）
- [ ] 重构 `src/theme/variables.css`：primary 与布局引用 token；保留 header 无阴影规则
- [ ] `src/main.ts` 在 variables 之前 import tokens.css
- [ ] 校验深色模式 primary 仍为 `#006FEE`

### 2. 组件层

- [ ] `src/components/ui/MEmptyState.vue`
- [ ] `src/components/ui/MCover.vue`
- [ ] `src/components/ui/MPage.vue`
- [ ] 可选 `index.ts` 导出
- [ ] 列表 playing/行高：token 或共享类

### 3. 迁移

- [ ] AlbumsPage / ArtistsPage：empty + cover（若适用）+ 尽量 MPage
- [ ] PlaylistsPage / PlaylistDetailPage：empty + cover
- [ ] SongsPage：empty + cover + playing 背景 token；MPage 若可行
- [ ] MiniPlayer：封面改 MCover
- [ ] SourcesPage empty（若结构一致）
- [ ] PlayerPage：仅 immersive 硬编码色/时长 → token（不拆大组件）

### 4. 文档与规范

- [ ] 更新 `.trellis/spec/frontend/directory-structure.md`（theme + components/ui）
- [ ] 更新 `component-guidelines.md`（ui 层约定、禁止 ion 1:1 封装）
- [ ] DESIGN.md 组件节补充路径（轻量）

### 5. 验证

- [ ] `npm run lint`
- [ ] `npm run build`（含 vue-tsc）或项目惯用 type-check
- [ ] 手动：歌曲列表播放高亮、空状态、MiniPlayer 封面、平板 720 宽、深浅色 primary

## 建议 commit 切分

1. `style(theme): 引入 muses tokens 并桥接 Ionic primary`
2. `feat(ui): 新增 MEmptyState / MCover / MPage`
3. `refactor(views): 列表页迁移空状态与封面组件`
4. `docs(spec): 记录 token 与 ui 组件约定`

## 回滚点

- 每步可独立 revert；优先保证 tokens 不破坏 ion-color-primary

## 实现时注意

- 读 `.trellis/spec/frontend` Pre-Development Checklist
- 图标继续走 `src/icons/ion-lucide.ts`
- 不引入 Material elevation
