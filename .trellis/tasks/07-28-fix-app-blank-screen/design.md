# 设计：修复 amll-pixi chunk 循环依赖导致白屏

## 边界与改动范围

仅改动 `vite.config.ts` 的 `manualChunks` 段。不动任何业务源码、不动依赖版本、不动 `index.html` / Capacitor 配置。

## 根因复述（详见 prd.md）

`amll-pixi` 与 `storage` 两 chunk 互引：
- `amll-pixi` 顶层 `np = t(...)` 调用的 `t` = 来自 `storage` 的导出 `S`。
- `storage` 的 `S` 实为它从 `amll-pixi` 再 import 的 `ut` 的 re-export。
- Rolldown/Vite 8 把 eventemitter3（CJS）的 `__commonJS` 桥接 helper 放进被广泛复用的 `storage` 共享 chunk，而 eventemitter3 本体（被 `@pixi/utils` 引入）落进 `amll-pixi` chunk，形成跨 chunk 顶层循环求值 → `t` 为 undefined → `t is not a function` → 挂载阻断 → 白屏。

## 设计：方案 1（首选，实测未通过）

把当前合并的 `amll-pixi` chunk 拆为两个独立 chunk：

```ts
manualChunks(id) {
  if (id.includes('@applemusic-like-lyrics')) return 'amll'
  if (id.includes('@pixi')) return 'pixi'
  // 其余 ionic / vue-vendor 保持不变
  ...
}
```

### 为何能打破循环

- eventemitter3 仅被 `@pixi/utils` 引用（已实测）。把 `@pixi/*` 单独成 `pixi` chunk 后，eventemitter3 及其 `__commonJS` 桥接 helper 都会落在 `pixi` chunk 内部（因 helper 的唯一消费者就在同一 chunk），不再需要跨 chunk 引用 `t`。
- `@applemusic-like-lyrics/*` 成独立 `amll` chunk，它 import `@pixi/*` 时是单向依赖（amll → pixi），无反向引用，循环消失。
- 首屏不预加载 `amll`/`pixi`（它们只在 `PlayerPage` 异步组件里用到），白屏消除。

### 兼容性

- Vue 组件 import 路径不变（`@applemusic-like-lyrics/vue`、`@pixi/*`），仅 chunk 名变。
- `index.html` 的 `modulepreload` 列表会自动从 `amll-pixi` 变为 `amll` / `pixi`（或因为 PlayerPage 异步加载而不进首屏 preload），由 Vite 自动生成，无需手动改 `index.html`。
- legacy chunk 同样按新名生成 `amll-legacy` / `pixi-legacy`，plugin-legacy 自动处理。

### 风险

- 若 Rolldown 仍把 eventemitter3 的桥接 helper 单独抽到某个共享 chunk（而非放进 `pixi`），循环可能以新名号复发。这是方案 1 的不确定性，靠验收步骤 2（CDP 验证）捕捉，必要时退到方案 2。

## 设计：方案 2（failback，最终采用）

方案 1 构建后仍存在 `amll-bBfLaOv2.js <-> storage-DjiYma42.js` 循环：`amll` 顶层依然 `import { S as t } from storage` 并立即执行 `np = t(...)`。因此按预案切换到方案 2。

最终移除 `@applemusic-like-lyrics` 与 `@pixi` 的 manualChunks 规则，让 Rolldown 自动切分。Rolldown 自动把 AMLL/PIXI 保留到异步 `PlayerPage` chunk，并消除跨 chunk 循环。

```ts
manualChunks(id) {
  // 不再对 @applemusic-like-lyrics / @pixi 指定 chunk
  if (id.includes('@ionic/vue') || id.includes('ionicons')) return 'ionic'
  if (id.includes('node_modules/vue/') || ...) return 'vue-vendor'
}
```

### 实测结果

- `PlayerPage-Bn1ZETC5.js` 约 413 kB，承载 AMLL/PIXI 相关代码。
- `index-DsGaSMFh.js` 约 12 kB，首屏入口未被 AMLL/PIXI 放大。
- 34 个 modern JS chunk 静态 import 图：`cycles: NONE`。

### 风险

- chunk 结构变化，缓存失效一次。

## 设计决策记录

- 起初选方案 1 而非直接方案 2：保留既有的 vendor 切分意图（缓存友好、首屏体积可控），改动最小、影响可分析；方案 1 实测仍有循环，最终按 failback 采用方案 2。
- 不升级依赖：已调研确认 amll/pixi 子包均已是最新、pixi v8 不兼容 amll peerDeps，纯升级无解（见 prd.md）。
- 不临时禁用 AMLL 背景（舍弃方向 C）：用户选择治本（B），且方案 1 保留背景功能、改动只在构建配置。

## 验证契约

- `npm run build` 成功（vue-tsc + vite build）。
- 产物里不再存在 `amll-pixi<->storage` 互引的 chunk（用脚本扫：新 `amll`/`pixi` chunk 不应 `import` 自彼此且顶层 `import` 链无环）。
- 部署到 MuMu/Capacitor，CDP reload 后无 `TypeError: t is not a function`，首屏 `/tabs/songs` 渲染。
