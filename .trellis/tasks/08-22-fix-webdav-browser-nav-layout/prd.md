# PRD：修复目录浏览面包屑导航条被按钮挤压

## 背景

浏览页（`/tabs/sources/webdav/browse`）目录导航条「返回上级 + 当前路径」中，`返回上级` 是 `MButton`（默认 `width: 100%`），实测占 332px，当前路径 `__path` 被挤到 49px，长路径截断成「/夸克网盘」，无法辨识完整层级。

与 08-21-fix-webdav-browser-row-layout 同根因（MButton 撑满），当时仅约束了目录行 `__row`，遗漏导航条 `__nav`。

## 需求

1. `WebDavDirectoryBrowser.vue` 的 `__nav` 行内 MButton 约束为 `width: auto; flex: 0 0 auto`。
2. `__path` 改为 `flex: 1; min-width: 0` 吃满剩余空间，保留省略号截断兜底。
3. 全组件排查其余 MButton 并排场景无同类遗漏。
4. 其他使用 MButton 的页面不受影响。

## 验收标准

1. MuMu 实测：浏览页导航条中路径显示宽度占满剩余空间，浅层路径完整可读。
2. 进入子目录后路径随之更新且不把「返回上级」按钮挤变形。
3. lint / vue-tsc / 单测通过。
