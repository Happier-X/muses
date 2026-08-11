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

### 0. Konsta k-list-input：必须用 `#input` 槽受控绑定

**坑（已踩）**：`k-list-input` 是非受控组件——Konsta 源码中 input 元素**不绑定 `:value`**（值存于 DOM，`value` prop 仅用于浮动 label 判断）。与 TanStack Form 的 `field.handleChange` 组合时，blur / 切字段等重渲染时机下**偶发丢失输入值**（实测复现：输入 → blur → focus 其它字段后值变空）。

**正确姿势**：用 `k-list-input` 的 `#input` 槽自定义 `<input>`，值以 `field.state.value` 为唯一真源：

```vue
<form.Field name="serverUrl" :validators="{ onSubmit: ... }">
  <template #default="{ field }">
    <k-list-input label="服务器地址" :error="firstFieldError(field.state.meta.errors)">
      <template #input>
        <input
          :value="field.state.value"
          type="url"
          placeholder="https://example.com/dav"
          autocomplete="username"
          class="block text-base appearance-none w-full focus:outline-none bg-transparent h-10 placeholder-black/30 dark:placeholder-white/30"
          @input="(e: Event) => field.handleChange((e.target as HTMLInputElement).value)"
          @blur="field.handleBlur"
        />
      </template>
    </k-list-input>
  </template>
</form.Field>
```

要点：

- 自定义 input 样式类必须**完整复制 Konsta iOS 默认 input 类**（`block text-base appearance-none w-full focus:outline-none bg-transparent h-10 placeholder-black/30 dark:placeholder-white/30`），漏掉 `h-10` 会让输入行退化成 24px、漏掉 placeholder 色会全黑——都是视觉回归。
- `label` / `error` / `info` 留在外层 `k-list-input`；`placeholder` / `type` / `autocomplete` 放自定义 input。
- 禁用 `@input="onFormInput(field.handleChange)"` 这种包一层再传 value 的老写法——它正是非受控丢值的来源。

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

### 2. 字段绑定（参考：HInput 已废弃，新代码用 k-list-input `#input` 槽）

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
5. 外层可用原生 `<form @submit.prevent="form.handleSubmit">` + `type="submit"` 按钮。
6. **提交中关闭保护**：用 `form.useSelector((s) => s.isSubmitting)` 驱动提交按钮 `disabled`；若表单在 `k-sheet` / `k-popup` / `k-dialog` 等浮层内，须在 close handler 内 early return。仅在 close 函数里 return 不够——用户仍可点遮罩或关闭控件触发抖动。
7. **loading 双源**：若同一浮层还有非提交的异步（如 WebDAV 列目录），可保留独立 loading ref；连接/保存类提交态优先用 form `isSubmitting`，按钮 `disabled` 取并集。

---

## 禁止（新代码）

- `k-list-input` 直接 `:value` + `@input` 非受控绑定 TanStack Form 字段（必须 `#input` 槽自定义受控 input，见 §0）
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
