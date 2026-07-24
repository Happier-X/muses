# 实现计划：TanStack Form

## 前置

- [x] PRD 决策收敛（范围 B / 内置校验 / 错误分层 / 仅提交校验）
- [x] design.md
- [x] 用户审阅 prd + design + implement 后 `task.py start`

## 步骤

### 1. 依赖

- [x] `npm install @tanstack/vue-form`
- [x] 确认 `package.json` 无 zod/valibot 等误加依赖

### 2. Spec

- [x] 新增 `.trellis/spec/frontend/forms.md`（约定 + 反例 + HInput 接法）
- [x] 更新 `.trellis/spec/frontend/index.md` 索引
- [x] 如有必要，在 `component-guidelines.md` 增加一行交叉引用（不重复长文）

### 3. 迁移 WebDAV 连接表单

- [x] `useForm` 替换 `webDavForm` ref
- [x] 字段：serverUrl / username / password + onSubmit 必填
- [x] `connectWebDav` 逻辑迁入 form `onSubmit`
- [x] 字段错误 → `HInput`；连接失败 → `errorMessage`
- [x] 提交/加载禁用态与现网一致
- [x] 关闭 modal 时 reset 表单与连接态（与现 `closeWebDavModal` 对齐）

### 4. 迁移编辑音源表单

- [x] `useForm` 替换 `editSourceForm` ref
- [x] `openEditSource` / `closeEditSource` / `pickEditedLocalDirectory` 改操作 form 状态
- [x] local / webdav 条件字段与校验
- [x] `saveEditedSource` 业务迁入 `onSubmit`；业务错误仍 `editErrorMessage`
- [x] password 空=保留原密码；连接变更校验保留
- [x] 提交中：`backdrop-dismiss` + 关闭按钮 disabled + close early return

### 5. 清理

- [x] 删除无用的 form ref / 仅服务于旧校验的局部状态
- [x] 确认扫描设置、删除、列表逻辑未误伤

### 6. 验证

```bash
npm run build
# 或至少
npx vue-tsc --noEmit
npm run lint
```

- [x] lint / vue-tsc / build 通过（check 子代理）
- [ ] 手动检查清单（真机/浏览器）：
  - WebDAV：空提交 → 字段错误；正确连接 → 可浏览
  - 编辑 local：改名/改目录/重选目录/保存
  - 编辑 webdav：留空密码保存；错误密码连接变更失败 → 表单级错误
  - 提交中按钮与关闭行为

## 回滚点

1. 装依赖后、改页面之前：可只卸载包。
2. 迁完 WebDAV 未迁编辑：可只回退单表单。
3. 全量完成后：git 回退该任务相关文件。

## 完成定义

满足 `prd.md` Acceptance Criteria；spec 可检索；Sources 两表单无行为回退。
