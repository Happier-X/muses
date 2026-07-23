# 实施清单：全量改用 happier-ui

## 前置

- [ ] 已读 `prd.md` / `design.md` / 本文件
- [ ] 已读 frontend pre-dev 相关 spec（实现时以 design 覆盖过期 M* 描述）
- [ ] `implement.jsonl` / `check.jsonl` 已配置

## 阶段 0 — 依赖与工具链

1. `package.json`：`happier-ui` 改为 `0.0.1`（npm）；增加 `@lucide/vue`
2. 删除 `vite.config.ts`、`tsconfig.json` 中 `../happier-ui/src` alias
3. `npm install`
4. `main.ts`：确保 `import 'happier-ui/style.css'`（位置在 token/Ionic 主题合理顺序：先库 style/tokens，再 Ionic 桥）
5. `src/theme/tokens.css` 保持 `@import 'happier-ui/tokens.css'`（走包 exports）
6. 验证：能解析 `import { HButton } from 'happier-ui'`

## 阶段 1 — 拆除断裂兼容层

1. 重写 `src/components/ui/index.ts`：只 re-export 库真实导出 + `MCover` + `MPage`
2. 删除 `MListRow.vue`（或停止导出）；所有 `MListRow` 调用点改回可工作的 Ionic 列表行
3. `MEmptyState` → `HEmpty`；删除旧别名依赖
4. 移除对 `HIconButton`/`HSettingRow`/`HListRow`/`HEmptyState` 的一切 import
5. 更新 `gaps.md` 初稿：列表行/设置行/图标按钮等

## 阶段 2 — 图标基础设施

1. 新建或改写 `src/icons/`：导出 `@lucide/vue` 组件与语义别名（对齐原 `ion-lucide` 语义表）
2. 全仓替换 `ion-icon` → `HIcon`（含 `MCover`、Player、Tabs、Songs…）
3. 删除 `src/icons/ion-lucide.ts`（确认无引用后）
4. 评估移除 `lucide` 非 Vue 包；`ionicons` 仅保留 Ionic 间接需要

## 阶段 3 — 按页替换已有组件

顺序建议（先易后难）：

1. **SettingsPage**：`HSwitch`、`HButton`；设置行登记缺口
2. **SourcesPage**：`HInput`/`HCheckbox`/`HButton`/`HEmpty`；modal/alert/sheet/list/card/progress 保留并登记
3. **TabsPage**：导航图标 + 视情况 `HTabBar`（保护平板侧栏）
4. **Albums/Artists/Playlists/Songs/Queue/PlaylistDetail**：空态 `HEmpty`；文字按钮 `HButton`；列表行不硬换
5. **MPage / 简单页头**：评估 `HNavBar`（`fixed=false`）
6. **MiniPlayer / PlayerPage**：图标 `HIcon`；icon-only 按钮壳保留；`ion-range` 不动

每完成一页：更新 `gaps.md` 落点。

## 阶段 4 — 文档与收口

1. 补全 `gaps.md`（组件名、落点、API、优先级、阻塞范围）
2. 清理无用 import / 死代码
3. 需要时轻触 `DESIGN.md` 中「file: 路径 / 旧组件表」的过时句（若本任务范围包含文档；否则 finish 时 update-spec）

## 验证命令

```bash
npm run lint
npm run build
npm run test:unit -- --run
```

手测清单：

- [ ] 冷启动无白屏
- [ ] 底栏/侧栏导航
- [ ] 歌曲列表播放与队列
- [ ] 设置开关与检查更新按钮
- [ ] 音源编辑表单保存路径
- [ ] 沉浸页播放/暂停/切歌/拖动进度
- [ ] 空列表空态展示

## 回滚点

| 点 | 动作 |
|----|------|
| 阶段 0 失败 | 恢复 `file:` 与 alias，勿进入页面大改 |
| 某页回归 | 单页回退 Ionic 控件，保留全局依赖与图标层（若图标已稳） |
| 全面失败 | `git` 回退本任务提交；`gaps.md` 可保留为研究 |

## 完成定义（对照 prd AC）

- 纯 npm 依赖 + 无源码 alias
- 无旧导出名编译依赖
- 0.0.1 已有组件在适用点完成替换
- 图标全面 `HIcon` + `@lucide/vue`
- `gaps.md` 完整
- lint/build 通过；主路径手测通过
