# 技术设计：WebDAV 音源目录浏览器

## 现状与边界

- `src/features/sources/webdav.ts`：`listWebDavDirectories(connection, path)` 已封装 PROPFIND 列目录，纯数据层，直接复用，不改动。
- `src/views/SourcesPage.vue`（约 1100 行）：添加 WebDAV 的浏览器 UI/状态内联在模板中：
  - 状态：`isWebDavModalOpen`、`isWebDavConnected`、`currentWebDavPath`、`webDavDirectories`、`selectedWebDavPaths`、`isWebDavLoading`
  - 行为：`loadWebDavDirectories` / `openWebDavDirectory` / `goToParentDirectory` / `setWebDavSelection*` / `addSelectedWebDavSources`
  - UI：MSheet 内「返回上级 + 当前路径」导航条、目录行（checkbox + 进入按钮）、底部批量添加按钮

## 方案

### 1. 新组件 `WebDavDirectoryBrowser`

位置：`src/components/webdav/WebDavDirectoryBrowser.vue`（feature 级组件，非通用 UI；若项目约定 feature 组件另有目录则遵循 directory-structure.md）。

职责：给定连接信息与初始路径，渲染「路径导航 + 目录列表 + 模式化选择操作」，内部自管浏览状态（当前路径、列表、loading）。

Props / Emits 契约：

```ts
interface WebDavConnectionInput { serverUrl: string; username: string; password: string }

props:
  connection: WebDavConnectionInput   // 变化时由父级通过 open() 驱动，见下
mode: 'single' | 'multiple'           // single=编辑回填；multiple=添加勾选
initialPath?: string                  // 默认 '/'

emits:
  confirm(payload: { paths: string[] })  // multiple: 点确认时带勾选集合；single: 点某行「选择」时带单元素数组
  error(message: string)                 // 浏览失败向上冒泡（页面统一 toast）
```

暴露方法（defineExpose）：`open(): Promise<void>` —— 重置到 initialPath 并加载首屏；失败 throw 由父级捕获。

内部保留：当前路径、目录列表、loading、（multiple 模式）勾选集合。错误处理：propfind 抛错 → emit error + 组件内不清空已浏览状态。

### 2. 编辑流程接入

编辑对话框（WebDAV 类型）「目录」字段旁增加「浏览」按钮：

- 点击 → 组装凭据（表单 serverUrl/username + 密码空则 `getWebDavPassword(source)` 取原密码）→ 打开浏览器（MPopup 或 MSheet，z-index 高于编辑对话框所在的 dialog 层级阶梯：dialog 1200，浮层用 sheet 1200 同级但 DOM 后渲染自然覆盖；实现时以实际层级表现为准）
- `confirm({ paths })` → `editSourceForm.setFieldValue('path', paths[0])` → 关闭浏览器
- 凭据缺失（如原密码读取失败）→ toast 提示，不打开浏览器

### 3. 添加流程迁移

添加 sheet 中内联的浏览区替换为 `<WebDavDirectoryBrowser mode="multiple">`：

- 「连接并浏览」验证成功后调用 `browser.open()`
- `confirm({ paths })` → 复用现有 `addSelectedWebDavSources` 的建源逻辑（按 paths 批量创建）
- 移除 SourcesPage 中被替代的状态与函数（webDavDirectories、currentWebDavPath、parentWebDavPath、goToParentDirectory 等），控制页面体积

### 4. 数据流与兼容

- 浏览器组件只依赖 `listWebDavDirectories`，不感知表单/存储，保持可测试。
- 路径格式沿用 `normalizeWebDavPath` 约定（'/' 开头），回填后保存流程零改动。
- 不改 secure storage 密码存取协议。

## 权衡

- **抽组件 vs 页面内复制一份单选逻辑**：抽组件改动面稍大，但消除双份浏览逻辑、页面瘦身约百行，符合 code-reuse 规范。选择抽组件。
- **编辑浏览器浮层形态**：复用 MSheet（与添加一致、视觉统一）；若与编辑 dialog 叠放出现层级问题，备选 MPopup(1100→需提级) 或调整 DOM 顺序，实现时验证。

## 回滚

单次提交整体回滚即可；无存储/协议变更，无数据迁移。
