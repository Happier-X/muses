# 技术设计：WebDAV 新增/编辑独立页面

## 路由

挂在 TabsPage children 下（与 PlaylistDetailPage 等详情页一致，保留 navbar/抽屉布局）：

```
/tabs/sources/webdav        → add 模式
/tabs/sources/webdav/:id    → edit 模式（id = source.id）
```

- 新页面组件：`src/views/SourceWebDavPage.vue`（命名对齐现有 `*Page.vue` 约定）。
- edit 模式按 id 从 `loadSources()` 查找音源；找不到（如失效深链）→ toast + 返回列表。

## 页面结构

```
MNavbar（返回按钮 + 标题「添加 WebDAV」/「编辑 WebDAV」）
└─ 表单区
   ├─ 显示名称（仅 edit）
   ├─ 服务器地址 / 用户名 / 密码（edit 密码留空=保留原密码）
   ├─ 目录（仅 edit，只读展示 + 由浏览器回填；浏览入口按钮）
   └─ 主按钮：add=「连接并浏览」 / edit=「连接并浏览」+「保存修改」
└─ 目录浏览区（连接成功后展开，占满剩余高度）
   └─ <WebDavDirectoryBrowser :mode="add ? 'multiple' : 'single'">
```

- 浏览器组件复用现有契约（props/emits/expose），页面作为宿主：
  - add：confirm(paths) → 复用 SourcesPage 迁出的批量建源逻辑
  - edit：confirm({paths:[p]}) → 写入表单 path 字段
  - error → 页面统一 toast

## 状态与数据流

- 表单逻辑从 SourcesPage 的 webDavForm 双模式改造**平移**到新页面，按路由模式初始化：
  - add：空表单；edit：预填 name/serverUrl/username/path，密码恒空。
- 提交逻辑平移：`submitWebDavEdit`（verify + updateSource + 密码兜底）与批量建源逻辑随迁；成功后 `router.replace('/tabs/sources')` 或 back。
- SourcesPage 删除：webDavMode、webDavForm、MSheet 双模式模板、内嵌浏览器、相关函数与样式。actionsheet「添加 WebDAV 文件夹」与卡片「编辑」改为 `router.push`。
- 列表刷新：确认 SourcesPage 每次进入是否重新 loadSources（onMounted）；若被 keep-alive 缓存需补 activated 刷新——实现时验证。

## 权衡

- **页面 vs 放大 sheet**：页面获得全屏高度与原生导航返回，目录浏览体验根本改善；代价是新路由与表单逻辑迁移。用户已明确选页面。
- **edit 模式浏览器交互**：保持 single 回填语义不变（点某行「选择」即回填），不新增「选定当前目录」等新交互，控制范围。

## 回滚

单提交整体回滚；无存储协议变更。
