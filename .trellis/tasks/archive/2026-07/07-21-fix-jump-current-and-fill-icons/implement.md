# 实现清单

## 顺序

1. **fill 图标（#45）**
   - [x] 扩展 `src/icons/ion-lucide.ts`：`lucideToIonIcon` 支持 outline/fill
   - [x] `play` / `pause` / `playSkipBack` / `playSkipForward` → fill
   - [x] `playOutline` 保持/恢复为 outline（与 `play` 解耦）
   - [x] 确认 MiniPlayer、PlayerPage 主控 import 使用 fill 导出
   - [x] `rg` 检查无意外依赖「主控必须是 outline」

2. **跳转遮挡（#43）**
   - [x] `SongsPage.vue`：为歌曲行增加 `scroll-margin-top`（覆盖双 toolbar，约 112–128px，按实测微调）
   - [x] 保持 `scrollIntoView({ behavior: 'smooth', block: 'start', inline: 'nearest' })`
   - [x] 若 CSS 方案不够（condense 大标题仍挡），再改为测量偏移（备选）

3. **规范**
   - [x] 更新 `.trellis/spec/frontend/component-guidelines.md`（FAB 滚动 + 主控 fill）

4. **验证**
   - [x] 更新/补充单测（跳转断言；可选图标 fill）
   - [x] `npm run lint` / `npm run test:unit`（或项目既有等价命令）
   - [ ] 手工 spot-check 列表中/末 + 两处播放控件（建议真机确认 120px）

5. **收尾**
   - [x] 关联关闭/评论 GitHub #43 #45
   - [x] commit + archive（Phase 3）

## 验证命令

```bash
npm run lint
npm run test:unit
# 或项目 package.json 中的等价脚本
```

## 回滚点

- 仅图标：还原 `ion-lucide.ts` 与相关 import
- 仅滚动：去掉 `scroll-margin-top`
- 整任务：`git revert` 对应 commit

## 审查门

- PRD 验收项全部勾选
- 无 #44 范围蔓延
- 无 ionicons 业务直引回归
