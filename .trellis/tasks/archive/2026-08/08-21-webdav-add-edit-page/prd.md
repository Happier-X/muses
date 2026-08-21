# PRD：WebDAV 新增/编辑改为独立页面

## 背景

WebDAV 音源的新增与编辑目前共用音源页底部的 MSheet 表单（08-21-webdav-edit-sheet-form 引入双模式）。sheet 空间局促，目录结构、路径导航展示不开，浏览体验差。用户要求改为独立页面。

## 需求

### R1 新增独立页面

- 新路由页面承载 WebDAV 表单 + 目录浏览，全屏空间展示目录结构与路径导航。
- 两种进入方式：
  - 新增：音源页 actionsheet「添加 WebDAV 文件夹」→ 跳转新页面（add 模式）
  - 编辑：WebDAV 音源卡片「编辑」→ 跳转新页面（edit 模式，按音源 id 加载数据预填）
- 页面带返回导航（navbar 返回按钮），保存/取消后回到音源列表。

### R2 页面内完整流程

- 表单字段：
  - add 模式：服务器地址、用户名、密码
  - edit 模式：显示名称、服务器地址、用户名、密码（留空保留原密码）、目录
- 「连接并浏览」成功后展开全屏目录浏览区（复用 `WebDavDirectoryBrowser`）：
  - add 模式 multiple 勾选批量建源（现状语义）
  - edit 模式单选回填目录字段
- 保存逻辑沿用现有实现（verify 连接、更新/建源、toast 反馈）。

### R3 移除 sheet 形态

- SourcesPage 中 WebDAV 的 MSheet 双模式表单及其状态机整体移除，跳转改用路由。
- 本地音源新增（系统选择器）/编辑（m-dialog）不变；删除确认等其余浮层不变。
- 数据层（storage/webdav feature）与 `WebDavDirectoryBrowser` 组件契约不变。

## 约束

- 遵循 `.trellis/spec/frontend/` 规范；新页面接入现有 TabsPage 布局（navbar/抽屉手势）。
- UI 文案简体中文。

## 验收标准

1. 添加 WebDAV → 进入新页面 → 填写连接 → 浏览目录（空间充裕）→ 勾选批量添加 → 回到列表看到新音源。
2. 编辑 WebDAV → 进入新页面预填正确 → 浏览选目录回填 → 修改名称/密码（或留空）保存生效。
3. 返回/取消不产生脏数据；保存后列表即时刷新。
4. 本地音源流程与其余功能不回归。
5. lint / vue-tsc / 单测通过；MuMu 实测通过。
