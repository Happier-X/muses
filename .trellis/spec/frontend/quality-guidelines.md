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
- `vite.config.ts`（仅构建配置；不得包含指向任何相邻仓库源码的 alias）

---

## Linting

The project uses ESLint 10 flat config with Vue 3 and TypeScript recommended rules.

Configuration file:

- `eslint.config.js`

Current notable rules and structure:

- `withVueTs(...)` + `eslint-plugin-vue` `flat/essential` + `vueTsConfigs.recommended`
- `no-console` warns only in production
- `no-debugger` warns only in production
- `vue/no-deprecated-slot-attribute` is disabled
- `@typescript-eslint/no-explicit-any` is disabled
- ignore 规则写在 flat config 的 `ignores` 中；不要再使用 `.eslintrc*` / `.eslintignore`

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

**防坑（08-21-webdav-directory-browser）**：验证命令禁止写成 `npm run build 2>&1 | tail` 这类管道形式——管道退出码取自最后一个命令（tail），vite build 失败（如 sass 报错，错误摘要呈现为 `errors: [Getter/Setter]`）会被吞掉导致后续 `cap sync`/打包继续用旧 dist。必须用 `npm run build; echo $?` 或分开执行并显式检查退出码。

**MuMu 模拟器 WebView 真机验证配方**：debug 构建的 Capacitor WebView 默认开启远程调试，可免视觉通道直接驱动 UI 验证：

```bash
adb forward tcp:9222 localabstract:webview_devtools_remote_$(adb shell pidof com.muses.player)
curl http://localhost:9222/json   # 拿 page target 的 webSocketDebuggerUrl
# Node ≥22 自带全局 WebSocket，写 ~15 行脚本发 Runtime.evaluate（awaitPromise+returnByValue）
# 即可在页面内查 DOM、点击按钮、读表单状态；原生 SAF 选择器则用 uiautomator dump 观察
```

注意 `loggingBehavior: 'none'` 会屏蔽前端 console 到 logcat，CDP 是唯一的前端运行时观测通道。

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
- asserts user-visible page content

For new e2e tests, exercise user-visible routes and UI behavior. Avoid brittle selectors tied to Vue internals.

Run:

```bash
npm run test:e2e
```

---

## Accessibility and User-Facing Quality

Preserve the accessibility practices already present in the UI:

- Decorative icons use `aria-hidden="true"` in `src/views/TabsPage.vue`.
- Navigation tabs include visible text labels（原生 `<span>` / 文案，不依赖 `IonLabel`）。
- External links opened in new tabs include `rel="noopener noreferrer"` in `src/components/ExploreContainer.vue`.

When adding controls, prefer visible labels or appropriate ARIA labels.

---

## Styling and Theme Quality

Keep CSS concerns separated:

- Global token / Tailwind imports stay in `src/main.ts`（`virtual:uno.css`、`./theme/tokens.css`、`./theme/tailwind.css`）；应用已完全脱离 Ionic，`main.ts` 不再有任何 `@ionic/vue/css/*` 导入。
- 全局 body 字体/颜色 fallback 写在 `src/theme/tailwind.css`；`src/theme/variables.css` 已清空（Ionic 桥接已移除）。
- Component-specific CSS uses `<style scoped>` in the component.

Reference files:

- `src/main.ts`
- `src/theme/tailwind.css`
- `src/components/ExploreContainer.vue`

## Tailwind v4 构建兼容性

Tailwind CSS v4（`@import 'tailwindcss'`）将所有输出包裹在 CSS `@layer` 块中：

- `@layer base` — Preflight 重置
- `@layer components` — 组件样式
- `@layer theme` — 主题变量
- `@layer utilities` — 工具类

⚠️ `@layer` 需要 **Chrome 99+（2022年3月）** 才支持。在旧版 Android WebView（Chrome < 99）上，浏览器会忽略所有 `@layer` 块内的样式。

### 解决方案

通过 `postcss.config.js` 添加 PostCSS 插件在构建阶段解包 `@layer` 块：

```javascript
// postcss.config.js — 解包 Tailwind v4 @layer，兼容旧版 Android WebView
const layerCompat = {
  postcssPlugin: 'layer-compat',
  AtRule: {
    layer: (atRule) => {
      if (atRule.nodes && atRule.nodes.length > 0) {
        atRule.replaceWith(...atRule.nodes)
      }
    },
  },
}

export default {
  plugins: [layerCompat],
}
```

**原理**：Vite 的 CSS 管道顺序为 `@tailwindcss/vite` → PostCSS → 输出。PostCSS 插件看到的是已展开的 Tailwind CSS（含 `@layer` 块），解包后移除了 `@layer` 包装，使样式在旧版浏览器上正常生效。

**注意点**：
- 此方案保留开发阶段（桌面浏览器）的 `@layer` 行为，仅在构建阶段解包
- 不切换回 `@tailwindcss/postcss`，继续使用 `@tailwindcss/vite`
- 只处理带内容的 `@layer xxx { ... }` 块，不影响 `@layer xxx;` 声明

验证方法：

```bash
# 构建后检查 CSS 中无 @layer 出现
grep -c '@layer' dist/assets/index-*.css
# 应输出 0
```

---

## Vite 手动分块与循环依赖

`manualChunks` 只能用于依赖边界明确、产物无环的模块组。不要强制把 `@applemusic-like-lyrics/*` 与 `@pixi/*` 合并为独立 vendor chunk；Vite 8 / Rolldown 可能把 CommonJS 互操作辅助函数放入其他共享 chunk，形成顶层循环求值，导致 Android WebView 启动时报 `TypeError: t is not a function` 并白屏。

当前约定：

- `vite.config.ts` 不为 `@applemusic-like-lyrics` / `@pixi` 配置 `manualChunks`，由 Rolldown 自动处理；AMLL/PIXI 会保留在异步 `PlayerPage` chunk，不进入首屏 `index` chunk。
- 修改 `manualChunks` 后，不能只看 `npm run build` 是否通过；必须扫描 modern chunk 的静态 import 图，确认无环，并在 Capacitor Android WebView 中冷启动验证。
- 若出现仅生产包白屏，优先用 CDP 检查未捕获异常与实际加载 URL；`loggingBehavior: 'none'` 时 JS console 不会进入 logcat。

错误示例：

```ts
// 错误：可能制造 amll-pixi <-> storage 的跨 chunk 顶层循环
if (id.includes('@applemusic-like-lyrics') || id.includes('@pixi')) {
  return 'amll-pixi'
}
```

正确示例：

```ts
// 正确：不手动切 AMLL/PIXI；保留边界稳定的 vendor 规则
manualChunks(id) {
  if (id.includes('@ionic/vue') || id.includes('ionicons')) {
    return 'ionic'
  }
  if (id.includes('node_modules/vue/') || id.includes('node_modules/@vue/')) {
    return 'vue-vendor'
  }
}
```

验证至少包括：

```bash
npm run lint
npm run build
npx cap sync android
```

产物断言：无 `amll-pixi*.js`，modern chunk import 图无环，冷启动后 URL 到达 `/tabs/songs`，CDP 无 `TypeError: t is not a function`。

---

## Anti-Patterns

Avoid:

- Skipping `npm run build` after TypeScript or Vue SFC changes.
- Adding tests that only assert framework implementation details.
- 重新引入 `@ionic/*` 依赖、`ion-*` 标签或 Ionic 全局 CSS（应用已完全脱离 Ionic）。
- 移除路由页的自建 `MPage` / `MContent` 骨架而回退到裸 `<div>` 无滚动容器。
- Introducing architecture not reflected by current requirements.

---

## 依赖升级约定

依赖升级必须按兼容组分层验证，不能只改版本号：

- **应用已完全脱离 Ionic 与自研 UI 库**：`package.json` 无 `@ionic/vue` / `@ionic/vue-router` / `ionicons` / `happier-ui`；路由用 `vue-router`，页面骨架用自建 `MPage` / `MContent`，UI 用 Konsta `k-*` 组件。业务侧图标全面使用 `@lucide/vue` 组件直渲染，禁止 `ion-icon`、`@/icons/ion-lucide` 与 `import ... from 'ionicons/icons'`。Capacitor 核心与插件保持同一主版本。
- `happier-ui` 默认固定使用 npm **精确版本** `happier-ui@0.1.1`（不用 `^`）；不得提交 `file:../happier-ui` 或相邻源码 alias。必须接入 Tailwind CSS v4（`tailwindcss` + `@tailwindcss/vite`），全局样式经 `src/theme/tailwind.css` 走 `@import 'tailwindcss'` + `@import 'happier-ui/styles'` 管道，禁止直接引旧 `style.css`。库没有的组件保留业务实现并登记任务 `gaps.md`，不得在 Muses 新造通用平行 M* 组件。
- Vite、Vue 插件、legacy 插件应作为一组升级；Vitest 与 jsdom、ESLint 与 Vue/TypeScript 配置链也应分别成组验证。
- 每组升级后运行 lint、build 和完整 unit test；最终执行 `npm ci` 验证锁文件可干净重建，并运行 `npx cap sync android` 检查原生插件同步。
- 跨主版本若失败，任务记录必须保留具体命令和兼容性证据，不得为了满足“最新”强行破坏可构建组合。
- 当前已验证组合包括 Vite 8 + plugin-vue 6 + plugin-legacy 8、ESLint 10 flat config + eslint-plugin-vue 10 + `@vue/eslint-config-typescript` 14、`vue-router` 5、`vue-tsc` 3、TypeScript **6.0.x**。TypeScript 7（`latest` 7.0.2）仍不可用：`vue-tsc` 无法解析 `typescript/lib/tsc`，且 `typescript-eslint@8` peer 为 `>=4.8.4 <6.1.0`，故 **pin TypeScript 6.0.3**（不要为了 latest 强升到 7）。应用已脱离 Ionic，`vue-router` 可按自身兼容性独立升级验证。
- Android APK 最终构建需要 JDK 21；本地无 Java 时必须通过 CI 验证，不能把 `cap sync` 等同于 APK 编译成功。
- **发布时必须同步 `package-lock.json` 的根 `version`**：仅改 `package.json` 的 `version` 而不改锁文件，会在 GitHub Actions（Linux + Node 22 `npm ci`）失败。
- **`picomatch` 多版本并存**：Vite 8 / vitest / tinyglobby 需要 `picomatch@4`，`micromatch` 需要 `picomatch@2`。Windows 上本地 `npm ci` 可能通过，但 Linux CI 会对锁文件报 `Invalid: lock file's picomatch@2.3.2 does not satisfy picomatch@4.0.5`。发布或依赖升级后应用 `package.json` `overrides` + 直接 `devDependencies.picomatch@4.0.5` 固定解析，并在干净目录再跑一次 `npm ci`；最终以 Release workflow 的 `npm ci` 为准。

## 发布约定（v* tag）

1. 同步三处版本：`package.json`、`package-lock.json` 根 version、`android/app/build.gradle` 的 `versionName`；`versionCode` 严格递增。
2. 新增 `changelog/vX.Y.Z.md`（中文，只写已合并内容）。
3. 本地：`npm ci`、`npm run lint`、`npm run build`、`npm run test:unit -- --run`、`git diff --check`。
4. 提交 `chore(release): vX.Y.Z` 后打 tag 并推送；确认 GitHub Release 含 `muses-vX.Y.Z.apk` 与 `muses-vX.Y.Z-mi.apk`。
5. 若 Actions 在 `npm ci` 失败，先修锁文件/overrides，再移 tag 到修复提交后重推，不要假设本地 Windows `npm ci` 等同于 Linux CI。

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
