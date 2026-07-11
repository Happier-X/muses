# 技术设计：应用秒开优化

## 1. 架构与边界

### 1.1 问题根因（一句话）
`App.vue` 静态 `import PlayerPage`（`src/App.vue:23`）把 `@applemusic-like-lyrics/core` + `@pixi/*` 整套 WebGL 库拖进首屏必须同步加载的主 bundle（1.5MB），而首屏根本不渲染 PlayerPage。

### 1.2 解决链路
1. **R1 异步化 PlayerPage**：把 `PlayerPage` 改成 `defineAsyncComponent(() => import('@/views/PlayerPage.vue'))`，Vite/Rollup 才会把 PlayerPage 及其依赖（AMLL / Pixi）切成独立异步 chunk，不进主 bundle。
2. **R3 manualChunks 兜底分包**：`vite.config.ts` 配 `build.rollupOptions.output.manualChunks`，确保即便 PlayerPage 外的其他静态入口也被打包进合适 chunk（ionic / vue / amll-pixi 分别成块），并锁住缓存粒度。
3. QueuePage 同样改异步（`App.vue:24` 也改为 `defineAsyncComponent`），与 PlayerPage 对称、保持一致性，但 QueuePage 依赖很轻，收益较小，主要是封装对称。

### 1.3 修改文件清单
| 文件 | 改动 |
|---|---|
| `src/App.vue` | `import PlayerPage/QueuePage` → `defineAsyncComponent(() => import(...))`；引入 `defineAsyncComponent` from `vue` |
| `vite.config.ts` | 增 `build.rollupOptions.output.manualChunks` 分包函数 |

注：`App.vue` 已用 `<Transition v-if="playerOverlayVisible">` 控制 PlayerPage 挂载，配合 `defineAsyncComponent` 后，首次 `v-if=true` 时才触发异步 import —— 正是按需加载语义。原 `<Transition>` 行为不变。

## 2. 数据流 / 组件契约

- `playerOverlayVisible`（`src/features/player/overlay.ts`）控制 PlayerPage 显隐，`defineAsyncComponent` 不改变 `v-if` 触发时机，仅把组件定义从静态对象变成异步工厂。
- 异步组件首次解析会有极短延迟（chunk 下载）；为避免此时 `<Transition>` 动画卡顿，配合 `defineAsyncComponent` 的 `loadingComponent` / `delay` 选项留可选位（本轮先不引入 loading 组件，保持 `<Transition>` 与现状一致；若有体感问题后续单独调）。
- MiniPlayer 仍在主 bundle（依赖很轻），首屏可见逻辑不受影响。

## 3. manualChunks 设计

```ts
build: {
  rollupOptions: {
    output: {
      manualChunks(id) {
        if (id.includes('@applemusic-like-lyrics') || id.includes('@pixi')) {
          return 'amll-pixi'
        }
        if (id.includes('@ionic/vue') || id.includes('ionicons')) {
          return 'ionic'
        }
        if (id.includes('node_modules/vue/') || id.includes('node_modules/@vue/') || id.includes('node_modules/vue-router/') || id.includes('node_modules/@ionic/vue-router/')) {
          return 'vue-vendor'
        }
      },
    },
  },
}
```

要点：
- `amll-pixi` chunk 只在 PlayerPage 异步加载时被引入，不在首屏主入口同步链路。
- 路径匹配用 `node_modules/xxx` 形式适配 Windows / POSIX 两种分隔（Vite 传入的 id 用 POSIX 风格 `/`，故用 `includes` 而非 `path.sep`）。
- 仅做「分到指定 chunk」，不做强制拆分到极细；保持现有 `@vitejs/plugin-legacy` 双份产物结构不变。

## 4. 兼容性 / 迁移

- 无数据结构变更，无运行时存储影响。
- legacy 产物同样会产出对应异步 chunk（`*-legacy-*.js`），浏览器能力检测逻辑不变。
- 回退按钮逻辑（`App.vue` onMounted 里 `App.addListener('backButton')`）不依赖组件加载方式，保持原样。

## 5. Rollback

- `src/App.vue` 改回静态 import 两行即可回退 R1。
- `vite.config.ts` 删除 `build.rollupOptions` 段即可回退 R3。
- 两者独立，回退不互相影响。

## 6. 验证手段

- 构建产物体积对比：`npm run build` 后比较 `dist/assets/` 中主入口 JS 体积与 `grep -c "pixi"`。
- 手测：安卓 WebView 冷启动首屏可见速度；点开播放器全屏页功能（背景动画/歌词/控制/队列/回退）。
- 类型 + lint：`npm run build`（含 `vue-tsc`）、`npm run lint`。
