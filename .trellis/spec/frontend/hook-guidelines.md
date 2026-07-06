# Hook Guidelines

> Custom hook and composable conventions for this project.

---

## Overview

This Vue project currently does not define custom hooks/composables. There is no `src/composables/`, `src/hooks/`, or similar directory in the repository.

Reference evidence:

- `src/` contains `components/`, `router/`, `theme/`, and `views/` only.
- Existing components use simple `<script setup lang="ts">` blocks without shared composables.

---

## Current Pattern

For the current app size, keep component logic local when it is only used by one component or page.

Examples of current local simplicity:

- `src/App.vue` only imports Ionic shell components.
- `src/views/Tab1Page.vue` only imports Ionic layout components and `ExploreContainer`.
- `src/views/TabsPage.vue` imports Ionic tab components and icon constants directly.

---

## When to Add a Composable

Add a composable only when a task introduces repeated logic that is shared across multiple components or pages.

If a composable becomes necessary, prefer Vue naming conventions:

- Put it under `src/composables/`.
- Name files and functions with `use*`, for example `useExample.ts` exporting `useExample()`.
- Keep it UI-framework agnostic unless it is explicitly tied to Ionic behavior.
- Return named values and functions rather than a broad untyped object.

This is a future-facing rule; no existing source file currently demonstrates it.

---

## What to Avoid

Avoid inventing composables for one-off logic in this small app. In particular:

- Do not move simple static tab/page setup into hooks.
- Do not add a composables directory solely for organization.
- Do not introduce data-fetching abstractions before there is an API layer or repeated server-state pattern.

---

## Verification

When adding a composable in the future:

- Make sure it is imported from at least two places, or document why centralization is needed.
- Cover non-trivial behavior with `vitest` tests under `tests/unit/` or colocated tests if the project later adopts that pattern.
- Run `npm run build` and `npm run test:unit`.
