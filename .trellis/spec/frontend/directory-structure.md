# Directory Structure

> How frontend code is organized in this project.

---

## Overview

The repository is a single frontend app with a simple Ionic Vue layout. Code is currently organized by technical role rather than by feature folder.

通用 UI 库为 npm 依赖 **`happier-ui@0.0.2`**；禁止提交 `file:../happier-ui`，也禁止 Vite/TypeScript 指向相邻仓库源码 alias。

```text
src/
├── App.vue
├── main.ts
├── components/
│   ├── MiniPlayer.vue
│   └── ui/                     # 库导出与 app-only 边界层
│       ├── index.ts            # re-export happier-ui 真实导出 + app-only
│       ├── MCover.vue          # app-only 音乐封面
│       └── MPage.vue           # app-only HOST-IONIC 页壳
├── theme/
│   ├── tailwind.css            # @import tailwindcss + happier-ui/styles（TW4 管道）
│   ├── tokens.css              # @import happier-ui/tokens.css（兼容/桥接）
│   └── variables.css           # Ionic 桥接
└── views/
```

Related non-app folders:

```text
tests/
├── e2e/
│   ├── fixtures/
│   ├── specs/
│   └── support/
└── unit/
```

---

## Module Organization

Use the existing split unless the codebase grows enough to justify feature modules:

- `src/main.ts` owns app bootstrap, plugin registration, global CSS imports, and mount timing；必须先加载 `src/theme/tailwind.css`（内含 Tailwind v4 + `happier-ui/styles`），再加载 `tokens.css` / `variables.css`。
- `src/App.vue` is the root shell and should stay minimal.
- `src/router/index.ts` owns route records and redirects.
- `src/views/` contains route-level pages.
- `src/components/` contains reusable UI pieces used by pages.
- `happier-ui@0.0.2`：npm 发布包；默认以 registry 版本为准，本地联调只可临时 link，完成后恢复 npm 依赖。peer 需 `tailwindcss@^4`。
- `src/components/ui/`：边界层——只 re-export 库真实导出与 app-only 的 `MCover`/`MPage`；不新增通用 M* 平行组件。
- `src/icons/`：导出 `@lucide/vue` 语义组件；业务统一通过 happier-ui `HIcon` 渲染，禁止旧 `ion-lucide` 适配层。
- 库没有对应能力的 Ionic/业务落点记录在任务 `gaps.md`，未来回到 happier-ui 仓库开发。
- `src/theme/tailwind.css`：Tailwind v4 入口（`@import 'tailwindcss'` + `@import 'happier-ui/styles'`）；`vite.config.ts` 必须启用 `@tailwindcss/vite` 的 `tailwindcss()` 插件。
- `src/theme/tokens.css`：仅 `@import 'happier-ui/tokens.css'`（与 styles 内 token 同源，供桥接与单独引用）。
- `src/theme/variables.css`：Ionic 桥接 `--h-*`；`main.ts` 在 tailwind 入口之后再加载 tokens / 本文件。

Reference files:

- `src/main.ts`
- `src/App.vue`
- `src/router/index.ts`
- `src/views/TabsPage.vue`

---

## Routing-Centric Structure

This app currently follows Ionic’s page-container conventions:

- Route-level screens live in `src/views/`.
- The tab shell is implemented as a page component in `src/views/TabsPage.vue`.
- Child tab pages are lazy-loaded from the router using `() => import(...)`.

Reference files:

- `src/router/index.ts`
- `src/views/TabsPage.vue`

Avoid putting route definitions inside page components. Keep navigation structure centralized in `src/router/index.ts`.

---

## Naming Conventions

Current naming patterns in the repo:

- Vue SFC files use PascalCase: `App.vue`, `TabsPage.vue`, `ExploreContainer.vue`.
- Route-level views use a `*Page.vue` suffix: `Tab1Page.vue`, `Tab2Page.vue`, `Tab3Page.vue`.
- Router entry files use conventional names like `index.ts`.
- Use the `@/` alias for imports from `src/`.

Reference files:

- `src/views/Tab1Page.vue`
- `src/components/ExploreContainer.vue`
- `src/router/index.ts`
- `tsconfig.json`

---

## What Does Not Exist Yet

The current app has introduced lightweight feature support code for real feature behavior:

```text
src/features/sources/
├── storage.ts
├── types.ts
└── webdav.ts

src/features/playlist/
├── index.ts
├── storage.ts
└── types.ts
```

Use `src/features/<feature>/` only when a feature has actual non-view contracts such as persistence, native plugin integration, API/WebDAV clients, or shared feature types. Keep route-level screens in `src/views/` and keep route definitions in `src/router/index.ts`.

Playlist list/detail live in `PlaylistsPage.vue` / `PlaylistDetailPage.vue`; route `playlists/:id` is owned by the router.

Do not create broad `services/`, `api/`, or global store directories just because one feature needs small helpers.

---

## Examples

Good reference points for the current shape:

- Root shell: `src/App.vue`
- App bootstrap and CSS loading: `src/main.ts`
- Router-owned page composition: `src/router/index.ts`
- Tab page layout: `src/views/Tab1Page.vue`
- Reusable shared component: `src/components/ExploreContainer.vue`
