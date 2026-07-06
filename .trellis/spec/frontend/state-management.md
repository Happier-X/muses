# State Management

> Local state, global state, and server-state conventions for this project.

---

## Overview

The current app has no global state management layer. There is no Pinia, Vuex, custom store directory, API client, or server-state library in the codebase.

State-related dependencies in `package.json` are limited to Vue, Vue Router, Ionic Vue, and Capacitor/Ionic packages.

Reference files:

- `package.json`
- `src/main.ts`
- `src/router/index.ts`
- `src/views/Tab1Page.vue`
- `src/views/TabsPage.vue`

---

## Current State Pattern

The current UI is effectively static and route-driven:

- Route state is managed by Vue Router through `src/router/index.ts`.
- Tab navigation is represented by Ionic tabs in `src/views/TabsPage.vue`.
- Pages render static labels and a shared presentational component.
- There is no app-level state initialized in `src/main.ts` beyond registering Ionic and the router.

---

## Route State

Keep navigation state in Vue Router and Ionic tab components.

Current route structure:

- `/` redirects to `/tabs/tab1`
- `/tabs/` renders `TabsPage`
- `/tabs/tab1`, `/tabs/tab2`, and `/tabs/tab3` lazy-load their page components

Reference file:

- `src/router/index.ts`

Avoid duplicating current tab selection in a separate store unless a task introduces a real cross-component state need.

---

## Local Component State

For future small UI interactions, prefer local Vue state inside `<script setup lang="ts">` using Vue Composition API primitives such as `ref` and `computed`.

Because current components are static, no existing file demonstrates local reactive state yet. Add it in the component that owns the interaction unless multiple components need the same value.

---

## Global State

Do not add a global store preemptively. Introduce a global state library only when the app has repeated cross-page or cross-component state that cannot be cleanly handled with local state, props, emits, router params, or a focused composable.

If a future task adds Pinia or another store, document the chosen pattern in this file and add concrete references to the first store modules.

---

## Server State and Persistence

Use the smallest persistence mechanism that satisfies the feature contract. For non-sensitive app metadata, feature-local helpers may use browser storage such as `localStorage` with explicit validation before data is trusted.

Sensitive values such as WebDAV passwords, tokens, or other credentials must not be written to `localStorage`, logs, route params, or task-visible metadata. On Android, store these values through a Capacitor secure storage plugin and keep only an opaque lookup key such as `credentialKey` in non-sensitive metadata.

Current source module contract:

- `localStorage` key `muses:sources` stores only source metadata.
- WebDAV source metadata stores `credentialKey`, never the password.
- WebDAV passwords are stored with `@aparajita/capacitor-secure-storage`.
- Removing a WebDAV source should also remove the corresponding secure-storage entry.

Avoid creating service/client/cache abstractions before the application has actual data access requirements.

---

## Anti-Patterns

Avoid:

- Adding Pinia/Vuex just to store tab/page labels.
- Mirroring router state in a global store.
- Creating API or persistence folders without actual API or persistence behavior.
- Storing credentials, tokens, or WebDAV passwords in `localStorage` or logs.
- Passing large untyped state objects through props.
