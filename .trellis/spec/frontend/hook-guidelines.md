# Hook Guidelines

> Custom hook and composable conventions for this project.

---

## Overview

This Vue project currently does not define a large shared composables layer. Prefer local `<script setup>` logic until logic is shared across multiple pages.

Reference evidence:

- Business logic often lives under `src/features/*` pure modules rather than Vue composables.
- Pages use Composition API without a mandatory `src/composables/` tree.

---

## Current Pattern

For the current app size, keep component logic local when it is only used by one component or page.

Examples:

- `src/App.vue`：壳层 + Capacitor 状态栏 / 返回键；`RouterView` + MiniPlayer + 常驻 Player/Queue。
- 各 `*Page.vue`：页面局部状态 + 调用 `src/features/*`。

---

## When to Add a Composable

Add a composable only when a task introduces repeated logic shared across multiple components or pages.

If a composable becomes necessary:

- Put it under `src/composables/`.
- Name files and functions with `use*`，例如 `useExample.ts` exporting `useExample()`.
- Keep it UI-framework agnostic（Vue / happier-ui / Capacitor 边界清晰）。
- Return named values and functions rather than a broad untyped object.

---

## What to Avoid

- Do not move simple one-off page setup into hooks.
- Do not add a composables directory solely for organization.
- Do not introduce data-fetching abstractions before there is a repeated server-state pattern.
- **Do not** reintroduce Ionic-specific lifecycle/composable patterns（`onIonView*` 等）。

---

## Verification

When adding a composable in the future:

- Make sure it is imported from at least two places, or document why centralization is needed.
- Cover non-trivial behavior with unit tests if the project adopts that pattern for the module.
- Run `npm run lint` and `npm run build`.
