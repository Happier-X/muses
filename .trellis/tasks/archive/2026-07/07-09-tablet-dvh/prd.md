# 平板 Player 页横屏视口校准：vh → dvh

## 目标

将 `src/views/PlayerPage.vue` 中所有 `vh` 视口单位替换为 `dvh`（dynamic viewport height），使横屏下 Player 页高度自适应设备实际可用视口，不再因工具栏/导航栏占位导致内容被挤压或溢出。

归属于父任务 `07-09-tablet-adapt` 子任务 E。

## 背景

- `src/views/PlayerPage.vue` 中多处使用 `vh` 单位（`100vh`、`calc(100vh - 84px)`、`70vh`、`48vh`）
- `vh` 在横屏设备上被浏览器工具栏压缩（工具栏占用部分视口），导致内容实际可用高度变小
- `dvh`（dynamic viewport height）是 CSS 新标准单位，自动排除浏览器 UI 控件占用的区域，返回真正可用视口高度
- 用户选择纯 `dvh` 替换（不做 `vh` 回退），接受老设备上可能的降级

## 需求

| ID | 需求 |
|----|------|
| R1 | `src/views/PlayerPage.vue` 中所有 `vh` 替换为 `dvh`：`100vh` → `100dvh`；`calc(100vh - 84px)` → `calc(100dvh - 84px)`；`70vh` → `70dvh`；`48vh` → `48dvh` |
| R2 | `vw` 单位不变（`min(72vw, ...)`、`min(86vw, ...)`、`max-width: 88vw` 等不涉及横屏纵向问题） |
| R3 | 不新增/修改任何其他文件 |

## 技术约束

- 不改脚本/模板
- 不改 `vw` 或其他非 `vh` 单位
- 仅 CSS `<style scoped>` 中替换

## 边界情况

| 场景 | 预期 |
|------|------|
| 竖屏（手机）| `dvh` 与 `vh` 在竖屏下近似等效，无视觉变化 |
| 横屏（平板/手机旋转）| 内容占满实际可用高度，不溢出不被工具栏裁剪 |
| 老设备不支持 `dvh` | 该属性被视为无效，CSS 回退到元素默认高度（非最优但不会崩溃） |

## 验收标准

- [ ] PlayerPage 中所有 `vh` 已被 `dvh` 替换，无残留 `vh`
- [ ] `npm run build` 通过
- [ ] `npm run lint` 零错误
- [ ] 竖屏视觉与改动前一致

## 超出范围

- 全局所有页面 `vh` → `dvh`（仅 Player 页）
- `vw` 横屏校准
- JS 动态视口高度方案