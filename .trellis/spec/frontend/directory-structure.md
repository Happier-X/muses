# Directory Structure

> How frontend code is organized in this project.

---

## Overview

The repository is a single frontend app on **Vue 3 + vue-router + happier-ui + Capacitor**（**无** `@ionic/vue` / `ionicons`）。Code is organized by technical role with lightweight `src/features/<name>/` for non-view contracts.

通用 UI 库为 npm 依赖 **`happier-ui@0.1.1`**；禁止提交 `file:../happier-ui`，也禁止 Vite/TypeScript 指向相邻仓库源码 alias。

```text
src/
├── App.vue
├── main.ts
├── components/
│   ├── MiniPlayer.vue
│   └── ui/                     # 库导出与 app-only 边界层
│       ├── index.ts            # re-export happier-ui 真实导出 + app-only
│       ├── MCover.vue          # app-only 音乐封面
│       └── MPage.vue           # app-only 页壳（非第三方 UI 框架）
├── theme/
│   ├── tailwind.css            # @import tailwindcss + happier-ui/styles（TW4 管道）
│   ├── tokens.css              # @import happier-ui/tokens.css + Muses 变量
│   └── variables.css           # 历史空壳 / 兼容说明（桥接已移除）
├── features/                   # 曲库、播放、云端元信息等非 view 契约
├── icons/                      # @lucide/vue 语义导出
└── views/
```

Related non-app folders may include tests under `tests/` when present.

---

## Module Organization

- `src/main.ts` owns app bootstrap、`vue-router` 注册、全局 CSS、`mount` 时机；必须先加载 `src/theme/tailwind.css`（Tailwind v4 + `happier-ui/styles`），再加载 `tokens.css`（及可选 `variables.css`）。
- `src/App.vue` is the root shell and should stay minimal（`RouterView` + MiniPlayer + 常驻 Player/Queue）。
- `src/router/index.ts` owns route records and redirects。
- `src/views/` contains route-level pages（`*Page.vue`）。
- `src/components/` contains reusable UI pieces used by pages。
- `happier-ui@0.1.1`：npm 发布包；peer 需 `tailwindcss@^4`。
- `src/components/ui/`：边界层——只 re-export 库真实导出与 app-only 的 `MCover`/`MPage`。
- `src/icons/`：导出 `@lucide/vue` 语义组件；业务统一 `HIcon`，禁止旧 ionicons 适配层。
- 库没有对应能力的业务落点可记任务 `gaps.md`，未来回到 happier-ui 仓库开发。
- `src/theme/tailwind.css`：Tailwind v4 入口；`vite.config.ts` 启用 `@tailwindcss/vite`。
- `src/theme/tokens.css`：happier-ui tokens + Muses 变量。
- `src/theme/variables.css`：Ionic 桥接已移除；勿再写入 `--ion-*`。

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
