# 表单统一使用 TanStack Form

## Goal

将项目内表单状态、校验与提交流程统一到 TanStack Form（Vue：`@tanstack/vue-form`），避免继续用散落的 `ref` + `@submit.prevent` 手写模式，并为后续表单提供可复用约定。

## 背景（已核实）

- 技术栈为 Vue 3 + Ionic Vue + happier-ui（`HInput` / `HButton` 等），**非 React**。
- 依赖中尚无 `@tanstack/vue-form`；仅有 `@tanstack/vue-virtual`。
- 无 zod / valibot / yup / vee-validate。
- 当前仓库内可见表单仅在 `src/views/SourcesPage.vue`：
  - 编辑音源（`edit-source-form` + `saveEditedSource`）
  - WebDAV 连接（`webdav-form` + `connectWebDav`）
- 两处均为原生 `<form @submit.prevent>` + 多个 `ref`/`v-model`，提交逻辑内做 `trim` 与必填校验。
- 表单级错误：`editErrorMessage` / `errorMessage` 文本展示；非字段级。
- `HInput` 已支持 `error` / `invalid` / `description` props，可接字段错误。
- 扫描设置区（`readTags` 开关）不是提交型表单，**不在本任务迁移范围**。

## Requirements

- 新增并固定使用 `@tanstack/vue-form` 作为表单状态与提交的标准方案。
- 与现有 `HInput` / `HButton` 等 UI 组件可组合（字段绑定、错误展示、提交禁用态）。
- **本任务范围 = B**：依赖 + frontend spec 约定 + 迁移 `SourcesPage` 两处真实表单（编辑音源、WebDAV 连接）。
- **校验 = A**：仅用 TanStack Form 内置 validators（本项目默认挂在 **onSubmit**），**不**新增 Zod / Valibot。
- **错误展示 = A**：
  - 同步客户端校验（必填、trim 后为空等）→ 字段级，经 `HInput` 的 `error` / `invalid`。
  - 异步/业务失败（WebDAV 连接失败、目录验证失败、保存失败、缺密码等）→ 保留表单级 `editErrorMessage` / `errorMessage`。
- **错误出现时机 = A**：仅在提交时跑字段校验并展示字段错误；提交前不因输入弹出红字。
- 现有 Sources 页相关表单行为不回退：连接/编辑/密码与路径规范化、连接变更时校验目录、本地「重新选择目录」、提交中禁用关闭/按钮等业务语义保持。
- 在 `.trellis/spec/frontend` 留下可执行约定，后续新表单默认走 TanStack Form。
- 不在本任务抽通用 `FormField` 封装层；字段直接用 TanStack Form API 接 `HInput`。

## 非目标

- 不引入 React 版 `@tanstack/react-form`。
- 不引入 Zod / Valibot / yup 等 schema 校验库（本任务）。
- 不重做表单视觉设计（仍遵循 PRODUCT/DESIGN 与 happier-ui）。
- 不借机重写 Sources 业务（WebDAV 浏览、扫描选项、删除音源等非表单状态保持现状）。
- 不迁移扫描设置区（非 submit 表单）。
- 不抽可复用 FormField 组件层。
- 不把跨字段/服务端业务错误强行拆成字段级文案。

## Acceptance Criteria

- [ ] 依赖中加入 `@tanstack/vue-form`（与 Vue 3 兼容；当前 registry 最新为 1.33.x 量级，以安装时解析为准）。
- [ ] 前端 spec 写明：默认用 `@tanstack/vue-form`、`useForm`、onSubmit 校验、字段接 `HInput`、同步字段错误 + 业务表单级错误。
- [ ] 客户端字段校验仅用内置 validators，无 schema 库依赖。
- [ ] 同步校验错误出现在对应 `HInput` 下，且主要在提交后展示。
- [ ] 业务/异步错误仍走表单级错误区（文案语义与现网等价）。
- [ ] `SourcesPage` 编辑音源表单与 WebDAV 连接表单均改用 TanStack Form。
- [ ] 迁移后行为不回退：必填、trim、WebDAV 密码可选更新、连接变更校验、提交中禁用、本地重选目录写回 path 等。
- [ ] `vue-tsc` / 相关 lint 通过；无新增 schema 依赖。

## Decisions（已收敛）

| # | 议题 | 结论 |
|---|------|------|
| 1 | 范围 | B：依赖 + 规范 + 迁 Sources 两处；不抽 FormField |
| 2 | 校验库 | A：仅 TanStack Form 内置 validators |
| 3 | 错误展示 | A：同步字段级 + 异步/业务表单级 |
| 4 | 触发时机 | A：仅提交时展示字段错误 |

## Notes

- 复杂任务：`task.py start` 前需有 `design.md` + `implement.md`。
- `prd.md` 只写需求与验收，不写实现清单。
