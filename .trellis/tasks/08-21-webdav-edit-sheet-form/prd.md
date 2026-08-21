# PRD：WebDAV 编辑改用添加式 sheet 表单

## 背景

上一任务（08-21-webdav-directory-browser）为编辑流程的「目录」字段加了浏览按钮，但编辑仍走 `m-dialog` 小对话框，与添加流程的 MSheet 表单（服务器地址/用户名/密码 + 连接并浏览 + 目录浏览器）形态割裂。用户要求统一：**添加和编辑都用添加的这个 sheet 形式，废弃编辑对话框**。

## 现状

- 添加 WebDAV：actionsheet → MSheet「添加 WebDAV」表单（serverUrl/username/password）→ 连接并浏览 → `WebDavDirectoryBrowser` multiple 模式勾选 → 批量建源。无「显示名称」字段（名字从路径自动取）。
- 编辑 WebDAV：`m-dialog`「编辑音源」（显示名称/服务器地址/用户名/新密码/目录+浏览按钮），保存走 verify + 更新逻辑，密码留空保留原密码。
- 编辑本地音源：同一个 m-dialog 的 local 分支（显示名称/路径+重新选择目录）。**本次不改**。

## 需求

### R1 编辑 WebDAV 改用 sheet 表单

- 点 WebDAV 音源「编辑」→ 打开与添加相同的 MSheet 表单，预填该音源的 serverUrl / username / path 对应信息。
- 表单标题区分模式：「添加 WebDAV」/「编辑 WebDAV」。
- 编辑态额外显示「显示名称」输入框（预填当前名称，可改）。
- 密码字段沿用现有语义：留空 = 保留原密码；填写 = 更新密码。
- 浏览选目录：编辑态保持**单选回填**语义（复用 `WebDavDirectoryBrowser` single 模式的选择结果写入表单目录），交互外壳与添加一致。
- 保存：走现有编辑更新逻辑（verify 连接、更新名称/路径/密码），成功后 toast 并关闭 sheet。

### R2 移除编辑对话框

- WebDAV 分支从 m-dialog 移除；m-dialog 保留给本地音源编辑与其他用途（删除确认等）。
- 清理不再使用的代码（editSourceForm 中 webdav 专属字段处理、浏览按钮、相关样式），避免死代码。

### R3 不改动边界

- 本地音源编辑对话框不变。
- 添加流程行为不变。
- 底层数据层（storage/webdav feature）不变。

## 验收标准

1. 编辑 WebDAV 音源 → 打开 sheet 表单，各字段预填正确 → 可连接浏览并单选回填目录 → 保存成功，列表即时刷新。
2. 密码留空保存不破坏原密码（改目录/名称后扫描播放仍正常）；填新密码则更新。
3. 显示名称可修改并生效。
4. 本地音源编辑、添加流程均不回归。
5. lint / vue-tsc / 单测通过；MuMu 实测通过。

## 实现要点（轻量设计，替代 design.md）

- 将现有 MSheet 表单改造为双模式（mode: 'add' | 'edit'）：编辑态注入初始值、显示名称字段、提交分流到更新逻辑。
- 复用上一任务的凭据组装（密码空 → `getWebDavPassword(credentialKey)` 兜底）与浏览器打开逻辑。
- 提交按钮文案：添加态「连接并浏览」，编辑态建议「保存修改」；浏览区在编辑态由「浏览目录」入口或连接成功后展开，实现时以交互最简为准。
