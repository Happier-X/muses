> ⚠️ **已废弃（2026-08-26）**：Web/Vue 层已随纯原生重写完成而删除。本目录文档仅作
> 历史规格参照（PlayerPage.vue / ScrapePage.vue 等行为语义的原始出处），**不是现行开发规范**。
> 现行规范见 [../android/index.md](../android/index.md)。

# Frontend Development Guidelines

> Project-specific frontend conventions for this repository.

---

## Overview

This repository is a Vue 3 music app built with:

- Vue 3 SFCs
- `vue-router`（`createRouter` + `createWebHistory`）
- `konsta`（Konsta UI v5，iOS 主题，`k-*` 组件）+ 自建 `MPage` / `MContent` 页面骨架
- Tailwind CSS v4（`@tailwindcss/vite`）
- TypeScript with `strict: true`
- Vite
- Vitest for unit tests
- Cypress for e2e tests
- Capacitor 原生壳（状态栏/返回键/键盘）

应用**已完全脱离 Ionic**（见 `07-25-migrate-off-ionic-core`）：零 `@ionic/*` 依赖、零 `ion-*` 标签、零 `ionicons`。

The guidance in this directory documents the codebase as it exists today. It is intentionally lightweight and should match current project reality rather than aspirational architecture.

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | Module organization and file layout | Filled |
| [Component Guidelines](./component-guidelines.md) | Component patterns, props, composition | Filled |
| [Forms](./forms.md) | 可提交表单：TanStack Form、校验与 HInput 绑定 | Filled |
| [Hook Guidelines](./hook-guidelines.md) | Custom hooks, data fetching patterns | Filled |
| [State Management](./state-management.md) | Local state, global state, server state | Filled |
| [Quality Guidelines](./quality-guidelines.md) | Code standards, forbidden patterns | Filled |
| [Type Safety](./type-safety.md) | Type patterns, validation | Filled |
| [Features·Player](./features-player.md) | 播放器/原生通知/媒体会话规范 | Filled |

---

## Current App Shape

The app is organized around self-built page skeletons and tab routing:

- App shell: `src/App.vue`（自建 `div.app-shell` + `<RouterView>`）
- App bootstrap: `src/main.ts`
- Router definition: `src/router/index.ts`（`vue-router`）
- Page skeleton: `src/components/ui/MPage.vue` / `src/components/ui/MContent.vue`
- Top-level pages: `src/views/*.vue`
- Theme tokens: `src/theme/tokens.css` / `src/theme/tailwind.css`

Representative files:

- `src/main.ts`
- `src/router/index.ts`
- `src/views/TabsPage.vue`
- `src/views/SongsPage.vue`
- `src/components/ui/MPage.vue`

---

## Scope Note

This spec reflects a minimal frontend codebase. Some template spec topics (such as advanced state management and custom hooks) are included mainly to document the current absence of those patterns, so future agents do not invent architecture that does not yet exist.
