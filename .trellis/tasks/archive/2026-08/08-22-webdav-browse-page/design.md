# 技术设计：WebDAV 目录浏览独立页面

## 路由

```
/tabs/sources/webdav/browse   → 全屏目录浏览页（SourceWebDavBrowsePage.vue）
```

挂在 TabsPage children 下；TabsPage navItems 音源项 `childPrefixes` 增加该前缀。

## 跨页会话服务

新增 `src/features/sources/webdavBrowseSession.ts`（模块级单例，非响应式持久化，仅内存）：

```ts
interface WebDavBrowseSession {
  connection: WebDavConnectionInput
  mode: 'single' | 'multiple'
  initialPath: string
  resolve: (paths: string[]) => void   // 浏览页确认后回传给来源页
  cancel: () => void                   // 浏览页返回时通知来源页（可选清理）
}

export const setWebDavBrowseSession(s: WebDavBrowseSession): void
export const takeWebDavBrowseSession(): WebDavBrowseSession | null  // 取走即清空
```

- 来源页跳转前 `set`；浏览页 `onMounted` 时 `take`，拿不到（刷新/深链）→ toast + `router.replace` 回表单页。
- 选择结果用 Promise resolve 回传，来源页 await 后继续建源/回填逻辑——避免再开一条事件通道。
- 密码仅在内存流转，不进 URL / storage。

## 页面结构

### SourceWebDavPage（改造）

- 移除内嵌 `<WebDavDirectoryBrowser>` 区、`isBrowserOpen/browserRef/browserConnection/browserInitialPath/onBrowserConfirm` 等。
- 「连接并浏览」→ 现有 verify 通过后：
  - add：`openBrowseSession({mode:'multiple', ...})` → await 结果 → 复用现有批量建源逻辑
  - edit：`openBrowseSession({mode:'single', initialPath: 上级目录, ...})` → await → 回填 path 字段
  - 封装 `openBrowseSession`：set session + `router.push('/tabs/sources/webdav/browse')` + 返回 Promise

### SourceWebDavBrowsePage（新建）

- MNavbar 返回按钮；`<WebDavDirectoryBrowser>` 占满剩余高度。
- onMounted：take session → 设置组件 props 并 open()；无会话 → 兜底返回。
- confirm(paths) → `session.resolve(paths)` → `router.back()`（或 replace 回表单页）。
- error 事件 → 页面内 toast（沿用现有文案风格），不清空已浏览状态。

## 权衡

- **Promise 回传 vs 全局事件/store**：Promise 链路最短、类型安全，来源页逻辑保持顺序式；页面被系统杀掉等极端场景下 Promise 永不 settle——来源页表单仍在，用户可重新点击，可接受。
- **复用 WebDavDirectoryBrowser 整体 vs 只取列表**：整体复用零契约变更，浏览页只是换了宿主容器。选择整体复用。

## 回滚

单提交整体回滚；无存储/协议变更。
