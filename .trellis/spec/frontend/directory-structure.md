# Directory Structure

> How frontend code is organized in this project.

---

## Overview

The repository is a single frontend app with a simple Ionic Vue layout. Code is currently organized by technical role rather than by feature folder.

Current structure（npm workspaces monorepo）：

```text
packages/
└── happier-ui/                 # 通用 token + 语义组件（可跨项目）
    ├── package.json
    ├── README.md
    └── src/
        ├── index.ts
        ├── tokens.css          # 权威 --h-*（含 --muses-* 别名）
        └── components/
            ├── HEmptyState.vue
            ├── HIconButton.vue
            ├── HListRow.vue
            └── HSettingRow.vue
src/
├── App.vue
├── main.ts
├── components/
│   ├── MiniPlayer.vue
│   └── ui/                     # app 兼容层
│       ├── index.ts            # re-export happier-ui + app-only
│       ├── MListRow.vue        # 包装 HListRow + MCover
│       ├── MCover.vue          # app-only
│       └── MPage.vue           # HOST-IONIC
├── theme/
│   ├── tokens.css              # @import happier-ui/tokens.css
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

- `src/main.ts` owns app bootstrap, plugin registration, global CSS imports, and mount timing.
- `src/App.vue` is the root shell and should stay minimal.
- `src/router/index.ts` owns route records and redirects.
- `src/views/` contains route-level pages.
- `src/components/` contains reusable UI pieces used by pages.
- `packages/happier-ui`：跨项目包；根 `workspaces` + 依赖 `file:packages/happier-ui`。
- `src/components/ui/`：兼容层——从 `happier-ui` re-export；`MListRow` 包装默认封面；`MCover`/`MPage` 仅 app。
- `src/icons/`：调用方把 icon data 传给 `MIconButton`/`HIconButton`。
- `src/theme/tokens.css`：仅 `@import 'happier-ui/tokens.css'`。
- `src/theme/variables.css`：Ionic 桥接 `--h-*`；`main.ts` 先 tokens 再本文件。

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
