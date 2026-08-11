# 修复添加 WebDAV 音源问题

## 背景

应用 v0.2.8（Konsta UI 迁移后）的音源页支持添加本地文件夹与 WebDAV 文件夹。用户反馈**添加 WebDAV 时有问题**，需要先复现并修复。

## 待确认（Phase 1.1）

- [x] 具体现象：**填写表单后填写的内容又消失**（用户反馈）
- [x] 消失时机：**切字段时前面的内容消失**（填了服务器地址，点到用户名/密码时前面的没了）；模拟器复现
- [x] 服务器类型：不依赖特定服务器（表单层问题，连接前即出现）
- [x] 已复现于最新 debug 版（CDP 输入→blur→切字段后值丢失）

## 根因

`k-list-input` 是非受控组件（Konsta 源码：input 元素未绑定 `:value`，值存于 DOM；`value` prop 仅用于浮动 label 判断）。@tanstack/vue-form 的 `field.handleChange` 更新表单状态后触发重渲染，与非受控 DOM 值存在竞态——blur/切字段等重渲染时机下偶发丢失输入值（实测复现：输入→blur→focus 其他字段后值变空）。

## 修复方案

改用 Konsta 官方受控集成方式：`k-list-input` 的 `#input` 槽自定义 `<input>`，`:value="field.state.value"` + `@input="field.handleChange(...)"` + `@blur="field.handleBlur"`——值以 TanStack 表单状态为唯一真源，重渲染不再丢值。自定义 input 沿用 Konsta 默认 input 样式类（`block text-base appearance-none w-full focus:outline-none bg-transparent`）。

已修：音源页 WebDAV 添加表单 3 字段 + 编辑音源表单 5 字段（同模式一并修）；删除废弃的 `onFormInput`。

## 验收标准（初稿，待确认后修订）

- [x] WebDAV 添加表单输入 → 切字段 → 值保留（三轮输入实测全部保留）
- [x] 提交（连接失败）后表单值保留
- [x] 表单视觉正常（label/占位符/输入值）
- [x] 编辑音源表单同模式受控化（build 通过）
- [ ] 用户在模拟器上实际输入验证

## 现状链路（初步排查）

1. **UI**：`SourcesPage.vue` → k-sheet 表单（服务器地址/用户名/密码）→「连接并浏览」→ 目录列表（checkbox 选中）→「添加选中的 N 个文件夹」
2. **连接/浏览**：`webdav.ts` → `WebDavNative.propfind`（Capacitor 插件 `WebDavPlugin.kt`，OkHttp PROPFIND + Basic Auth + Depth:1）→ `parseWebDavEntries`（DOMParser 解析 XML，提取 href/displayname/collection）
3. **路径处理**：`normalizeWebDavPath` / `buildWebDavUrl` / `stripServerBasePath`（应对服务器 base path）

## 验收标准（初稿，待确认后修订）

- [ ] 添加 WebDAV 音源全流程可用：连接 → 浏览目录 → 选中 → 添加 → 扫描
- [ ] 问题现象消失且无回归（本地文件夹添加仍正常）