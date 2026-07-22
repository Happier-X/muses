# 技术设计：happier-ui（跨项目 Capacitor UI）

## 1. 锁定摘要

- **包名**：`happier-ui`
- **实现**：**纯 Vue 优先**，不 peer `@ionic/vue`
- **Token**：`--h-*`（如 `--h-color-primary`、`--h-space-md`、`--h-touch-target`）
- **首版视觉参照**：**HeroUI Native（移动端）** 默认主题与组件形貌——照抄可落地的 token / 圆角 / 触控 / 按钮·列表·表单行节奏；**不是** `npm install heroui-native`，也不是复刻其 RN 运行时
- **本任务**：**S0 + S1**；S2 monorepo 另议

### 1.1 样式首版原则（HeroUI Native）

| 照抄 | 不照抄 / 不引入 |
|------|------------------|
| Primary 色阶（已与 web HeroUI blue `#006FEE` 对齐，Native 主题语义色同系） | React Native / Uniwind / heroui-native 运行时依赖 |
| 语义色角色：accent、foreground、muted、surface、danger 等 **映射到 `--h-*`** | Material 3 elevation 与 MD 涟漪美学 |
| 移动端触控与间距节奏（舒适热区、列表行高、圆角偏软） | 运营向大色块、重阴影卡片 |
| Button / List item / Switch 行 的 **视觉比例与层级**（用纯 Vue + CSS 复现） | 1:1 拷贝 RN 组件 API 名 |

参考文档（实现时精读并摘 token）：
- https://www.heroui.com/docs/native/getting-started/theming
- https://www.heroui.com/docs/native/getting-started/colors
- 对应 Button / 列表与表单控件的 Native 组件页

与 PRODUCT 对齐：平台 Android、**非 Material 脸**；HeroUI Native 作为 **视觉样本**，系统手势/返回仍走 Capacitor + 宿主壳。

## 2. 包边界 v0.1

### 进包（通用）

| 导出名（建议） | 现 Muses | 备注 |
|----------------|----------|------|
| tokens（`tokens.css`） | `src/theme/tokens.css` | 权威前缀 `--h-*` |
| `HIconButton` 或保持 `MIconButton` 过渡 | `MIconButton` | 仅 prop icon，无 ion-lucide |
| `HEmptyState` | `MEmptyState` | |
| `HPage` | `MPage` | **纯 Vue 页壳**（header/content 槽）；Muses 可用薄适配包一层 ion-page **在 app 侧** |
| `HListRow` | `MListRow` | 可选 `playing`；默认不依赖 ion-item |
| `HSettingRow` | `MSettingRow` | end 槽放宿主 toggle |

命名：包内可用 `H*` 前缀避免与业务 `M*` 混淆；S1 可先硬化现 `M*` API，S2 再 rename 导出 alias。

### 留在 Muses

- `MCover`、MiniPlayer、Player、Queue 业务、Sources 重表单
- `src/icons/ion-lucide.ts`（宿主把 icon 传入 HIconButton）
- Ionic 路由壳、`ion-modal` / alert 等

## 3. Token 策略（`--h-*`）

```text
权威源（未来包）:  --h-color-primary, --h-space-*, --h-radius-*, ...
Muses 过渡:
  方案 A（推荐 S1）: tokens 改为定义 --h-*，并写
    --muses-color-primary: var(--h-color-primary);
    …（全量 alias，页面旧 var(--muses-*) 不炸）
  方案 B: 保留 --muses-* 为权威，额外
    --h-color-primary: var(--muses-color-primary);
    （包文档写宿主应覆写 --h-*；Muses 双写）
```

**S1 推荐方案 A**：权威迁到 `--h-*`，`--muses-*` 做兼容别名，符合「包叫 happier、前缀 h-」，同时不强迫本任务改所有视图字符串。

主色仍为 HeroUI `#006FEE` 数值，只改变量名层级。

## 4. 纯 Vue 与 Ionic 分层

```text
happier-ui (纯 Vue + --h-*)
    ↑
Muses app: 可选 MusesPage 适配 = ion-page + HPage 槽
           或业务页继续 ion-page 内嵌 HListRow
    ↑
@ionic/vue 仅 app 依赖
```

- **HIconButton / HListRow / HSettingRow / HEmptyState**：S1–S2 目标为 **不 import @ionic/vue**
- **HPage**：若现 `MPage` 深绑 ion-header，S1 可：
  - **S1a**：去掉 features/lucide 硬依赖，仍暂用 ion（标记 `// HOST-IONIC`）
  - **S1b**：纯 Vue header/content（推荐在 S1 能完成则做，避免包心残留 ion）

以「包心零 ionic」为 S1 完成定义；若 Page 风险大，implement 允许 Page 暂留 app。

## 5. 依赖

```text
happier-ui@0.1
  peerDependencies: { "vue": "^3.5.0" }
  无 @ionic/vue、无 @capacitor/*
  无 heroui-native / react-native（仅设计参照）
```

## 6. 阶段与 exit

| 阶段 | Exit |
|------|------|
| S0 | 本文档 + PRD 决策锁定 |
| S1 | ui 无 features；图标注入；`--h-*` 权威 + muses alias；lint/build/test 绿 |
| S2 | `packages/happier-ui`，Muses workspace 引用 |
| S3 | 第二 Capacitor 应用冒烟 |

## 7. 风险

| 风险 | 缓解 |
|------|------|
| expand 未合并 | 先收尾 expand |
| 去 ion-item 丢列表无障碍 | 纯 Vue row 补 button/role/focus；对照测 Songs 虚拟列表 |
| 双前缀混乱 | 单一权威 `--h-*` + 单向 alias |
| 改名 H* 大爆 | S1 可保留文件名 M*，package 导出再映射 |

## 8. 反模式

- 包内 `import '@ionic/vue'`（`ion-icon` 渲染 path 的过渡例外须在 S2 前去掉或隔离）
- 包内 `@/features`
- 把 MCover/播放器塞进 happier-ui v0.1

## 附录 A — HeroUI Native 语义 → `--h-*`（首版）

| HeroUI Native 角色 | `--h-*` |
|--------------------|---------|
| accent / primary | `--h-color-primary` / `--h-color-accent` |
| accent scale | `--h-primary-50`…`900` |
| surface | `--h-color-surface` |
| surface-secondary | `--h-color-surface-secondary` |
| foreground | `--h-color-ink` |
| muted / soft fg | `--h-color-ink-muted` |
| separator | `--h-color-separator` |
| focus | 使用 primary outline |
| radius（控件） | `--h-radius-control` / `--h-radius-md` |
| 触控 | `--h-touch-target` 48px |

阴影 token **不**引入业务列表（flat / 暗场听席优先）。
