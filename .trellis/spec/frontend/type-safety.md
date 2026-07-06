# Type Safety

> TypeScript and type organization conventions for this project.

---

## Overview

This project uses TypeScript in strict mode with Vue SFC support.

Reference files:

- `tsconfig.json`
- `tsconfig.node.json`
- `src/vite-env.d.ts`
- `src/router/index.ts`
- `src/App.vue`

Important compiler settings from `tsconfig.json`:

- `strict: true`
- `module: ESNext`
- `moduleResolution: Node`
- `isolatedModules: true`
- `resolveJsonModule: true`
- `noEmit: true`
- `paths` defines `@/*` as `./src/*`

---

## Vue SFC Type Pattern

Use `<script setup lang="ts">` in Vue components.

Reference files:

- `src/App.vue`
- `src/components/ExploreContainer.vue`
- `src/views/Tab1Page.vue`
- `src/views/TabsPage.vue`

Avoid plain `<script>` blocks for new components unless a task has a specific reason to use the Options API.

---

## Route Types

The router explicitly types route records:

```ts
import { RouteRecordRaw } from 'vue-router';

const routes: Array<RouteRecordRaw> = [
  // ...
]
```

Reference file:

- `src/router/index.ts`

Keep route definitions typed when adding routes. Prefer lazy imports for route page components, matching existing tab routes.

---

## Import Aliases

Use the `@/` alias for source imports from `src/`.

Examples:

- `import ExploreContainer from '@/components/ExploreContainer.vue'`
- `component: () => import('@/views/Tab1Page.vue')`

Reference files:

- `src/views/Tab1Page.vue`
- `src/router/index.ts`
- `tsconfig.json`

Relative imports are still used for nearby framework entry files, for example `src/main.ts` imports `./App.vue` and `./router`.

---

## Props and Local Types

The current code uses a simple runtime prop declaration in `src/components/ExploreContainer.vue`:

```ts
defineProps({
  name: String,
})
```

For similarly tiny presentational components, this matches the current style. For more complex props, prefer typed props so strict TypeScript can protect call sites:

```ts
defineProps<{
  name: string
}>()
```

Do not create a shared `types/` directory until types are genuinely shared across files.

---

## `any` Policy

ESLint currently allows explicit `any` via `@typescript-eslint/no-explicit-any: off` in `.eslintrc.cjs`. That means `any` is not lint-forbidden, but new code should still prefer concrete types where practical because TypeScript strict mode is enabled.

Acceptable uses of `any` should be narrow and isolated, such as integrating with untyped external data. Do not use broad `any` objects to bypass typing in normal component or router code.

---

## Environment and Vite Types

Keep Vite environment type references in `src/vite-env.d.ts`:

```ts
/// <reference types="vite/client" />
```

Do not duplicate this reference in application source files.

---

## Verification

Use the project build command for type checking:

```bash
npm run build
```

`npm run build` runs `vue-tsc` before `vite build`, so it is the primary type-safety verification command.
