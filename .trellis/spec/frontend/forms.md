# 表单约定（TanStack Form）

> 可提交表单的状态、校验与提交流程统一约定。

---

## Overview

本项目可提交表单默认使用 **`@tanstack/vue-form`**（Vue 3）。不要再为新表单用多个 `ref` + `@submit.prevent` 手写状态与校验。

- 包：`@tanstack/vue-form`（传递依赖 `@tanstack/form-core` 等随主包装上即可）
- 不引入 React 版 `@tanstack/react-form`
- 本阶段**不**引入 Zod / Valibot / yup 等 schema 校验库；字段校验只用 TanStack Form 内置 `validators`
- **不**抽通用 `FormField` 封装层；页面内直接用 `form.Field` 接 `HInput`

参考实现：

- `src/views/SourcesPage.vue`（编辑音源、WebDAV 连接）

---

## 标准用法

### 1. 创建表单

```ts
import { useForm } from '@tanstack/vue-form'

const form = useForm({
  defaultValues: {
    serverUrl: '',
    username: '',
    password: '',
  },
  onSubmit: async ({ value }) => {
    // 业务逻辑：value 已通过字段 onSubmit 校验
    // 异步/业务失败写表单级错误文案，不要塞进字段 meta.errors
  },
})
```

### 2. 字段绑定 `HInput`

默认**仅**挂 `validators.onSubmit`；不要默认挂 `onChange` / `onBlur` 校验（避免输入过程中弹出红字）。

```vue
<form @submit.prevent="form.handleSubmit">
  <form.Field
    name="serverUrl"
    :validators="{
      onSubmit: ({ value }) => (value.trim() ? undefined : '请填写服务器地址'),
    }"
  >
    <template #default="{ field }">
      <h-input
        :model-value="field.state.value"
        label="服务器地址"
        type="url"
        :error="typeof field.state.meta.errors[0] === 'string' ? field.state.meta.errors[0] : undefined"
        :invalid="field.state.meta.errors.length > 0"
        @update:model-value="field.handleChange"
        @blur="field.handleBlur"
      />
    </template>
  </form.Field>

  <h-button variant="primary" type="submit" :disabled="isSubmitting">
    提交
  </h-button>
</form>
```

提交中状态：

```ts
const isSubmitting = form.useSelector((state) => state.isSubmitting)
```

打开/关闭弹窗时同步默认值：

```ts
form.reset({ serverUrl: '', username: '', password: '' })
// 或单字段
form.setFieldValue('path', nextPath)
```

### 3. 校验与错误分层

| 类型 | 落点 | 触发 |
|------|------|------|
| 同步客户端校验（必填、trim 后为空等） | 字段级 → `HInput` 的 `error` / `invalid` | **仅提交时**（`validators.onSubmit`） |
| 异步/业务失败（连接失败、目录验证失败、保存失败、缺密码等） | 表单级文案（如 `errorMessage` / `editErrorMessage`） | 业务回调 |

规则：

1. **trim**：validator 内对 string 做 `trim()` 判空；写入业务前再 `trim()` 一次，避免半角空格入库。
2. **validators 返回 string**（或 `undefined`），保证 `meta.errors[0]` 可直接给 `HInput.error`。
3. **条件字段**（如 local 不展示 WebDAV 字段）：**不渲染则不挂 `form.Field`、不校验**，与 UI 一致。
4. **密码可选更新**（编辑 WebDAV）：字段可存在但不做必填；空字符串表示保留原密码，业务层处理 SecureStorage。
5. 外层可用原生 `<form @submit.prevent="form.handleSubmit">` + `type="submit"` 按钮；与 Ionic modal 焦点兼容即可。
6. **提交中关闭保护**：用 `form.useSelector((s) => s.isSubmitting)` 驱动提交按钮 `disabled`；若表单在 `ion-modal` 内，同时对 modal 设 `:backdrop-dismiss="!isSubmitting"`、关闭按钮 `:disabled="isSubmitting"`，并在 close handler 内 early return。仅在 close 函数里 return 不够——用户仍可点 backdrop 或关闭按钮触发 dismiss 抖动。
7. **loading 双源**：若同一 modal 还有非提交的异步（如 WebDAV 列目录），可保留独立 loading ref；连接/保存类提交态优先用 form `isSubmitting`，按钮 `disabled` 取并集。

---

## 禁止（新代码）

- 用多个 `ref` / 对象 `ref` 充当可提交表单的字段状态，再在 submit handler 里手写必填判断（应迁到 `useForm` + 字段 `onSubmit` validator）。
- 为表单引入 Zod / Valibot / yup 等 schema 库（除非任务明确批准）。
- 在输入过程中默认弹出字段红字（`onChange` 校验）。
- 把跨字段/服务端业务错误强行拆成字段级文案。
- 新建通用 `FormField` / `MFormField` 包装层（本阶段直接组合 `form.Field` + `HInput`）。
- 引入 `@tanstack/react-form`。
- modal 内可提交表单只在 close handler 里阻止关闭，却不禁用关闭按钮 / 不关 `backdrop-dismiss`。

---

## 非表单交互

仅开关、扫描选项等**非 submit 表单**的局部 UI 状态，可继续用 `ref` / `v-model`（例如 Sources 扫描设置的 `readTags`）。不要为了统一而硬套 `useForm`。

---

## 验证

改动表单相关代码后至少：

```bash
npm run lint
npm run build
```

`npm run build` 含 `vue-tsc`。
