# Frontend Development Guidelines

> Project-specific frontend conventions for this repository.

---

## Overview

This repository is currently a small Ionic Vue starter-style app built with:

- Vue 3 SFCs
- `@ionic/vue` and `@ionic/vue-router`
- TypeScript with `strict: true`
- Vite
- Vitest for unit tests
- Cypress for e2e tests

The guidance in this directory documents the codebase as it exists today. It is intentionally lightweight and should match current project reality rather than aspirational architecture.

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | Module organization and file layout | Filled |
| [Component Guidelines](./component-guidelines.md) | Component patterns, props, composition | Filled |
| [Hook Guidelines](./hook-guidelines.md) | Custom hooks, data fetching patterns | Filled |
| [State Management](./state-management.md) | Local state, global state, server state | Filled |
| [Quality Guidelines](./quality-guidelines.md) | Code standards, forbidden patterns | Filled |
| [Type Safety](./type-safety.md) | Type patterns, validation | Filled |

---

## Current App Shape

The current app is organized around Ionic pages and tab routing:

- App shell: `src/App.vue`
- App bootstrap: `src/main.ts`
- Router definition: `src/router/index.ts`
- Top-level pages: `src/views/*.vue`
- Shared UI component example: `src/components/ExploreContainer.vue`
- Theme variables: `src/theme/variables.css`

Representative files:

- `src/main.ts`
- `src/router/index.ts`
- `src/views/TabsPage.vue`
- `src/views/Tab1Page.vue`
- `src/components/ExploreContainer.vue`

---

## Scope Note

This spec reflects a minimal frontend codebase. Some template spec topics (such as advanced state management and custom hooks) are included mainly to document the current absence of those patterns, so future agents do not invent architecture that does not yet exist.
