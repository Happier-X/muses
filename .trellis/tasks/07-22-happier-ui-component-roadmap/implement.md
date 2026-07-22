# 执行计划：happier-ui 组件路线图

## 本任务性质

规划/路线图任务，不改代码。完成标准是 `prd.md` 中清单可执行。

## 后续建议任务拆分

### 任务 1：P0 打磨已有组件

- `HIconButton` variants / loading / docs
- `HListRow` density / selected / slot 文档
- `HSettingRow` interactive / lines
- `HEmptyState` icon/action/compact
- playground 补示例

### 任务 2：新增按钮与列表组

- `HButton` / `HTextButton`
- `HListSection`
- 替换 Muses 中 Settings / Sources / 列表页部分 `ion-button`、`ion-list`

### 任务 3：表单与反馈

- `HFormField`
- `HNotice`
- `HSurface/HCard`
- 首迁 SourcesPage

### 任务 4：低频控件

- `HToggle` / `HCheckbox` / `HProgress` / `HRange`
- 仅在 Muses 重复出现后做

## 验证方式

每个实现任务都应：

1. 在 `C:\code\happier-ui` playground 展示组件
2. 在 Muses 至少替换 1–2 个真实位置
3. 跑独立库 build + Muses lint/build/unit

## 当前结论

先实现：**P0 打磨 + `HButton` + `HListSection`**。
