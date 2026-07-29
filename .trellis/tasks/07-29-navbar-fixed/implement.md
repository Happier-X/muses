# Implement: Navbar 顶栏钉住

## 清单

1. **核验高度链**  
   - 检查运行时/构建后 CSS：`html`、`body`、`#app` 是否有可用高度。  
   - 若缺失：在 `src/theme/tailwind.css`（全局入口）补：
     ```css
     html, body, #app {
       height: 100%;
     }
     ```
     如移动端需动态视口，再评估 `min-height: 100dvh` 与现有 fixed 底栏关系（优先最小改动）。

2. **修 `TabsPage` 滚动归属**（`src/views/TabsPage.vue`）  
   - `main` 在 `isTabsRoute` 且 `md+`：保留侧栏右侧铺满（fixed/inset 或等价），**去掉 `md:overflow-auto`**，改为 `md:overflow-hidden`（并保证 `min-h-0` / 高度约束，使子级 `.m-page` 能吃到确定高度）。  
   - 窄屏：保持 `flex-1 min-h-0`，避免 main 自己变滚动容器。  
   - 非 tabs 路由分支不引入整页滚动吞 navbar。

3. **确认页壳**  
   - `MPage.vue`：维持 `HNavBar :fixed="false"` + `MContent`。  
   - 手写页（Songs/Playlists/…）：维持 `:fixed="false"` 与 `.m-page`/`.m-content`；不批量改成 `fixed=true`。  
   - 若某页缺少 `.m-content` 包住长内容，补滚动容器（仅当核验发现该页仍整页滚）。

4. **回归点**  
   - Songs 虚拟列表：`overflow:hidden` 的 `.m-content` + 内部 list。  
   - Queue overlay：`fixed inset-0` + 内栏 `:fixed="false"`。  
   - MiniPlayer / HTabBar fixed 层。

5. **文档**  
   - 若行为与 spec 有细微差（例如 main 的 overflow 约定），收尾时用 `trellis-update-spec` 补一句到 `component-guidelines.md` 页面骨架节。

## 验证命令

```bash
npm run lint
npm run build
```

手动（dev）：

- 窄屏 Songs/Settings：滚列表，顶栏不动。  
- ≥768px 同测 + 侧栏仍在。  
- 打开/关闭 Player、Queue 后再滚列表。

## 风险文件

| 文件 | 风险 |
|------|------|
| `src/views/TabsPage.vue` | main overflow/定位改坏宽屏高度或侧栏 |
| `src/theme/tailwind.css` | 全局 height 影响 overlay / body lock |
| `src/App.vue` | 仅当高度链需改壳时才动 |

## 回滚点

- 单 commit 还原上述文件即可；无 DB/原生配置。

## JSONL（sub-agent）

`implement.jsonl` / `check.jsonl` 在 `task.py start` 前写入真实 spec 条目（替换 seed `_example`）。
