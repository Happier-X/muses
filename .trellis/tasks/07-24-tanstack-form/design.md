# 设计：TanStack Form 接入与 Sources 表单迁移

## 目标与边界

- **目标**：统一「可提交表单」的状态、校验与提交流程；用真实页面验证与 `HInput`/`HButton` 的接法。
- **边界**：仅 `SourcesPage` 两处 submit 表单 + frontend spec；扫描设置、列表、WebDAV 目录浏览状态机不改架构。

## 依赖

| 包 | 用途 |
|----|------|
| `@tanstack/vue-form` | Vue 3 表单 API（`useForm` / `form.Field`） |
| 传递依赖 | `@tanstack/form-core`、`@tanstack/vue-store`（随主包装上即可） |

不引入 Zod / Valibot / yup。

## 运行时模型

```
用户输入 → form.Field 状态
         → 提交 → onSubmit validators（同步字段错误）
         → 通过 → onSubmit 业务（async）
              → 成功：关 modal / 加载目录 / 成功文案
              → 失败：表单级 errorMessage / editErrorMessage
```

### WebDAV 连接表单

| 字段 | 同步校验（onSubmit） | 备注 |
|------|----------------------|------|
| `serverUrl` | trim 后非空 | 可保留 type=url 展示 |
| `username` | trim 后非空 | |
| `password` | trim 后非空 | 连接时必填 |

- `onSubmit` 成功路径：沿用 `loadWebDavDirectories('/')` 等现有逻辑。
- 连接/列目录失败：`errorMessage`（表单级）。
- 提交中：`isWebDavLoading` 或 form 的 `isSubmitting` 与按钮禁用对齐（优先单一真相：提交态由 form 驱动，若现有 loading 还覆盖「浏览目录」则保留 `isWebDavLoading` 供浏览阶段使用）。

### 编辑音源表单

| 字段 | local | webdav | 同步校验 |
|------|-------|--------|----------|
| `name` | ✓ | ✓ | trim 非空 |
| `path` | ✓ | ✓ | trim 非空 |
| `serverUrl` | — | ✓ | webdav 时 trim 非空 |
| `username` | — | ✓ | webdav 时 trim 非空 |
| `password` | — | ✓ | **不**做必填；空=保留原密码 |

- 打开编辑：`form.reset` / `setFieldValue` 写入当前 `SourceItem`（password 恒为 `''`）。
- 本地「重新选择目录」：写回 form 的 `path` 字段，不走独立 `ref`。
- 连接变更校验、凭据读取、`updateSource` 等仍在提交成功后的业务回调中，失败写 `editErrorMessage`。
- 关闭保护：提交中禁止关闭（与现 `isEditSaving` 语义一致）。

## UI 绑定约定

字段模板模式（示意，实现以官方 Vue API 为准）：

```vue
<form.Field name="serverUrl">
  <template #default="{ field }">
    <h-input
      :model-value="field.state.value"
      label="服务器地址"
      type="url"
      :error="field.state.meta.errors[0]"
      :invalid="field.state.meta.errors.length > 0"
      @update:model-value="field.handleChange"
      @blur="field.handleBlur"
    />
  </template>
</form.Field>
```

- 错误文案：取 `meta.errors` 第一条字符串（validators 返回 string）。
- 提交按钮：`type="submit"`；`disabled` 绑定提交中状态。
- 外层仍可用原生 `<form @submit.prevent="form.handleSubmit">`（或库推荐的 `form.Subscribe` + handleSubmit），避免与 Ionic modal 焦点冲突即可。

## 校验策略

- 默认 **仅 onSubmit** 字段 validator；不挂 onChange 校验（与 PRD 时机一致）。
- trim：在 validator 内对 string 做 `trim()` 判断；写入业务前再 `trim()` 一次，避免半角空格入库。
- 条件字段：webdav 专用字段在 local 类型下可不渲染；validator 可用 form 级/字段级逻辑跳过，或仅在渲染时注册——实现时选**不渲染则不校验**的路径，与现 UI 一致。

## Spec 落点

- 新增或扩写 `.trellis/spec/frontend/` 中表单约定：
  - 推荐挂在 `component-guidelines.md` 一小节「表单」，并在 `index.md` 索引可检索；**或**独立 `forms.md` 并在 index 登记。
  - 决策倾向：**独立 `forms.md` + index 登记**，避免 component 文件继续膨胀；内容含：包名、useForm、onSubmit 校验、HInput 绑定、错误分层、禁止手写 ref 表单状态（新代码）。

## 风险与兼容

| 风险 | 缓解 |
|------|------|
| modal 打开时 form 默认值不同步 | open 时 reset；close 时清 password |
| local/webdav 字段集不同 | 条件渲染 + 条件校验 |
| `isWebDavLoading` 与 form isSubmitting 双源 | 连接提交用 form；列目录仍用 loading ref |
| HInput error 类型为 string | 保证 errors 映射为 string，不用对象 |
| 包体积 | 仅 vue-form + core，可接受 |

## 回滚

- 移除依赖与 Sources 改动、删 forms spec 小节即可；业务 API（`updateSource` / `listWebDavDirectories`）不变。

## 非设计范围

- FormField 包装组件、schema 库、扫描设置表单化、跨页表单状态共享。
