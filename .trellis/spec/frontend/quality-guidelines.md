# Quality Guidelines

> Code standards, tests, linting, and accessibility conventions for this project.

---

## Overview

Quality checks are defined through npm scripts in `package.json`:

```bash
npm run lint
npm run build
npm run test:unit
npm run test:e2e
```

Reference files:

- `package.json`
- `.eslintrc.cjs`
- `tests/unit/example.spec.ts`
- `tests/e2e/specs/test.cy.ts`
- `cypress.config.ts`
- `vite.config.ts`

---

## Linting

The project uses ESLint with Vue 3 and TypeScript recommended configs.

Configuration file:

- `.eslintrc.cjs`

Current notable rules:

- `plugin:vue/vue3-essential`
- `eslint:recommended`
- `@vue/typescript/recommended`
- `no-console` warns only in production
- `no-debugger` warns only in production
- `vue/no-deprecated-slot-attribute` is disabled
- `@typescript-eslint/no-explicit-any` is disabled

Run:

```bash
npm run lint
```

---

## Type Check and Build

The build script is the main full frontend verification path:

```bash
npm run build
```

It runs:

1. `vue-tsc`
2. `vite build`

Use this after TypeScript, routing, component, or dependency changes.

---

## Unit Tests

Unit tests use Vitest and Vue Test Utils.

Current example:

- `tests/unit/example.spec.ts`

The existing test mounts `Tab1Page` and asserts rendered text:

- import the component from `@/views/Tab1Page.vue`
- use `mount(...)` from `@vue/test-utils`
- assert with `expect(wrapper.text()).toMatch(...)`

For new unit tests, prefer testing user-visible component output and behavior rather than implementation details.

Run:

```bash
npm run test:unit
```

---

## E2E Tests

E2E tests use Cypress.

Current example:

- `tests/e2e/specs/test.cy.ts`

The existing test:

- visits `/`
- asserts that `ion-content` contains `Tab 1 page`

For new e2e tests, exercise user-visible routes and Ionic UI behavior. Avoid brittle selectors tied to Vue internals.

Run:

```bash
npm run test:e2e
```

---

## Accessibility and User-Facing Quality

Preserve the accessibility practices already present in the UI:

- Decorative icons use `aria-hidden="true"` in `src/views/TabsPage.vue`.
- Navigation tabs include visible `IonLabel` text.
- External links opened in new tabs include `rel="noopener noreferrer"` in `src/components/ExploreContainer.vue`.

When adding controls, prefer visible labels or appropriate ARIA labels.

---

## Styling and Theme Quality

Keep CSS concerns separated:

- Global Ionic CSS imports stay in `src/main.ts`.
- Theme customization belongs in `src/theme/variables.css`.
- Component-specific CSS uses `<style scoped>` in the component.

Reference files:

- `src/main.ts`
- `src/theme/variables.css`
- `src/components/ExploreContainer.vue`

---

## Anti-Patterns

Avoid:

- Skipping `npm run build` after TypeScript or Vue SFC changes.
- Adding tests that only assert framework implementation details.
- Re-importing global Ionic CSS inside components.
- Removing Ionic page wrappers from route-level views.
- Introducing architecture not reflected by current requirements.

---

## 依赖升级约定

依赖升级必须按兼容组分层验证，不能只改版本号：

- Capacitor 核心与插件保持同一主版本；Ionic Vue 与 `ionicons` 采用其声明的兼容组合，避免安装两套图标运行时。
- Vite、Vue 插件、legacy 插件应作为一组升级；Vitest 与 jsdom、ESLint 与 Vue/TypeScript 配置链也应分别成组验证。
- 每组升级后运行 lint、build 和完整 unit test；最终执行 `npm ci` 验证锁文件可干净重建，并运行 `npx cap sync android` 检查原生插件同步。
- 跨主版本若失败，任务记录必须保留具体命令和兼容性证据，不得为了满足“最新”强行破坏可构建组合。
- 当前已验证组合包括 Vite 8 + plugin-vue 6 + plugin-legacy 8、Vitest 4 + jsdom 29、Cypress 15（Node 22/24）。TypeScript 7 与当前 vue-tsc/ESLint 配置链不兼容时应保留 TypeScript 5.9；ESLint 10 需独立迁移 flat config；Vue Router 5 需单独验证 Ionic Router 集成。
- Android APK 最终构建需要 JDK 21；本地无 Java 时必须通过 CI 验证，不能把 `cap sync` 等同于 APK 编译成功。

## Recommended Verification Before Finishing Frontend Work

For routine frontend edits:

```bash
npm run lint
npm run build
npm run test:unit
```

Run Cypress e2e tests when routing, navigation, app bootstrap, or user-visible page flows change:

```bash
npm run test:e2e
```
