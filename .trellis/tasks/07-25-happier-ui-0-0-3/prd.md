# PRD: 升级 happier-ui 0.0.3

## 目标

升级 `happier-ui` 从 0.0.2 到 0.0.3，将新增组件注册到 UI 索引，保持向后兼容。

## 新增组件

| 组件 | 用途 |
|------|------|
| `HBadge` | 小型状态徽章（variant/size/dot） |
| `HPagination` | 分页器 |
| `HTextarea` | 文本域 |
| `HTag` | 可关闭标签 |
| `HSelect` | 下拉选择器 |
| `HTable` | 表格（排序/斑马纹/固定表头） |

## 验收标准

1. `package.json` 锁定 `happier-ui@0.0.3`
2. `src/components/ui/index.ts` 注册所有新增组件及类型
3. `npm run build` 通过
4. 已有功能不受影响
