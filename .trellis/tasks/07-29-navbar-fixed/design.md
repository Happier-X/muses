# Design: Navbar 顶栏钉住

## 问题机制

期望模型（spec 已写明）：

```text
.m-page (column, height:100%, overflow:hidden)
  ├─ HNavBar (:fixed=false，文档流，不滚)
  ├─ 可选 subnavbar
  └─ .m-content (flex:1, overflow-y:auto)  ← 唯一纵向滚动
```

实际破坏点：

1. **`TabsPage` `md+` main**  
   `md:fixed` + `md:overflow-auto` 使 main 成为滚动容器；其子树整页（含 navbar）随 main 滚动。  
   文件：`src/views/TabsPage.vue`。

2. **高度链可能断裂**  
   `.m-page { height:100% }` 依赖祖先有确定高度。若 `html/body/#app` 未形成 100% 链，`.m-page` 随内容增高，`overflow:hidden` 无法裁出内部滚动，滚动落到更外层。  
   需核验：Tailwind preflight、现有 `App.vue` 的 `h-full`、Tabs 根节点。

3. **非根因**  
   `HNavBar` 的 `fixed` 默认与 CSS 正常；Muses 用 `fixed=false` 是契约选择，不是库坏了。

## 方案

### 选定：恢复内容区滚动归属（A）

| 项 | 做法 |
|----|------|
| Tabs main | `md+` 保持定位铺满侧栏右侧，但 **`overflow` 改为 hidden**（或等价：不在 main 上滚动）；高度用 top/bottom 或 `h-full`+`min-h-0` 约束 |
| 高度链 | 在全局样式补齐 `html, body, #app { height: 100%; }`（或 `100dvh` 若实测移动浏览器需要），保证 `App` → `TabsPage` → `.m-page` 的 `h-full`/`height:100%` 生效 |
| NavBar | **保持** `:fixed="false"`（MPage 与各页） |
| 虚拟列表页 | 维持 `.m-content { overflow:hidden }` + 内部 list `overflow:auto` |

### 未选：HNavBar `fixed=true`（B）

- 需要为每页补顶栏占位/`padding-top`，与侧栏 `left` 偏移、safe-area、Queue overlay 叠加。
- 与 component-guidelines 明确契约相反。
- 仅当 A 无法在合理改动内修好时再考虑。

### 未选：向 happier-ui 提 issue（C）

- 库行为符合文档；问题在宿主滚动父级。

## 边界与契约

| 层 | 职责 |
|----|------|
| `happier-ui` HNavBar | 提供 fixed/非 fixed 两种模式；本任务不改库 |
| `MPage` / `.m-page` | 页壳高度与 overflow 裁剪；顶栏文档流 |
| `MContent` / `.m-content` | 业务页默认滚动容器 |
| `TabsPage` main | 路由视口：约束尺寸，**不**吞掉子页滚动 |
| `App.vue` | 应用壳 `h-full overflow-hidden`；MiniPlayer/Player/Queue 兄弟层 |

## 数据流 / 布局流

```text
#app (height 100%)
  App root (h-full overflow-hidden)
    RouterView → TabsPage (h-full flex col)
      aside (md fixed sidebar)
      main (flex-1 min-h-0; md: 铺满右侧 + overflow-hidden)
        RouterView → 业务页 .m-page
          HNavBar
          .m-content → 滚动
    MiniPlayer (fixed)
    Player/Queue overlays
```

## 兼容性

- 不改路由、不改 happier-ui 版本约定。
- 手写 `class="m-page"` 的页面与 `<m-page>` 组件共享 `.m-page`/`.m-content` 全局类，高度链修复对两者同时生效。

## 回滚

- 还原 `TabsPage.vue` main 的 overflow/定位 class。
- 还原全局 `html/body/#app` 高度规则（若本任务新增）。
- 无需数据迁移。

## 验证关注点

- 窄屏：长列表滚、顶栏钉、TabBar+MiniPlayer 仍在。
- 宽屏：侧栏固定、main 内顶栏钉、内容滚。
- Queue/Player 开闭后列表页顶栏仍钉。
- 虚拟列表（Songs）内部滚动与 `scroll-mt` 跳转当前曲不回归。
