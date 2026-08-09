# Directory Structure

> How frontend code is organized in this project.

---

## Overview

The repository is a single frontend app on **Vue 3 + vue-router + Konsta UI v5（iOS 主题）+ Capacitor**（**无** `@ionic/vue` / `ionicons` / `happier-ui`）。Code is organized by technical role with lightweight `src/features/<name>/` for non-view contracts.

通用 UI 库为 npm 依赖 **`konsta@5.3.0`**（精确版本）；`k-*` 组件经 `src/components/ui` re-export。

```text
src/
├── App.vue
├── main.ts
├── components/
│   ├── MiniPlayer.vue
│   └── ui/                     # 库导出与 app-only 边界层
│       ├── index.ts            # re-export Konsta k* + app-only
│       ├── MCover.vue          # app-only 音乐封面
│       ├── MEmpty.vue          # app-only iOS 空状态
│       └── MPage.vue           # app-only 页壳（非第三方 UI 框架）
├── theme/
│   ├── tailwind.css            # @import tailwindcss + konsta/vue/theme.css（TW4 管道）
│   ├── tokens.css              # 已删除（无 --h-*/--muses-* 语义层，数值内联）
│   └── variables.css           # 已删除（Ionic 桥接早已移除）
├── features/                   # 曲库、播放、云端元信息等非 view 契约
├── icons/                      # @lucide/vue 语义导出
└── views/
```

Related non-app folders may include tests under `tests/` when present.

---

## Module Organization

- `src/main.ts` owns app bootstrap、`vue-router` 注册、全局 CSS、`mount` 时机；加载 `src/theme/tailwind.css`（Tailwind v4 + `konsta/vue/theme.css`），并调用 `useSystemDark()`（matchMedia → `.dark` class）。
- `src/App.vue` is the root shell and should stay minimal（`<k-app theme="ios">` + `RouterView` + MiniPlayer + 常驻 Player/Queue）。
- `src/router/index.ts` owns route records and redirects。
- `src/views/` contains route-level pages（`*Page.vue`）。
- `src/components/` contains reusable UI pieces used by pages。
- `konsta@5.3.0`：npm 发布包；peer 需 `tailwindcss@^4`。
- `src/components/ui/`：边界层——只 re-export Konsta `k-*` 真实导出与 app-only 的 `MCover`/`MPage`/`MEmpty`。
- `src/icons/`：导出 `@lucide/vue` 语义组件；业务用 `<component :is>` 直渲染，禁止旧 ionicons 适配层。
- `src/theme/tailwind.css`：Tailwind v4 入口；`vite.config.ts` 启用 `@tailwindcss/vite`；含 body 背景、m-page 骨架、PlayerPage 沉浸样式等宿主 CSS。
- `src/theme/tokens.css` / `variables.css`：已删除（无 `--h-*`/`--muses-*` 语义层，数值内联）。

Reference files:

- `src/main.ts`
- `src/App.vue`
- `src/router/index.ts`
- `src/views/TabsPage.vue`

---

## Routing-Centric Structure

- Route-level screens live in `src/views/`。
- Tab shell：`src/views/TabsPage.vue` + 子路由 lazy `() => import(...)`。
- Navigation structure centralized in `src/router/index.ts`。

---

## Naming Conventions

- Vue SFC：PascalCase（`App.vue`、`SongsPage.vue`）。
- Route-level views：`*Page.vue` 后缀。
- 使用 `@/` 别名从 `src/` 导入。

---

## Feature modules

Use `src/features/<feature>/` when a feature has non-view contracts（persistence、native plugin、WebDAV、shared types）。Keep screens in `src/views/`。

Examples present in tree：`library`、`player`、`lyrics`、`metadata`、`cover`、`sources`、`playlist`、`editMeta` 等。

Do not create broad `services/`、`api/`、or global store directories just because one feature needs small helpers。

---

## Forbidden residual

- **禁止** 恢复 `ionic.config.json`、`package.json` 的 `ionic:*` scripts、`@ionic/*` / `ionicons` 依赖或源码 `ion-*` 标签（`08-05-remove-ionic-residue`）。
- Capacitor（`@capacitor/*`）是原生壳，**不是** Ionic UI 残留，必须保留。
