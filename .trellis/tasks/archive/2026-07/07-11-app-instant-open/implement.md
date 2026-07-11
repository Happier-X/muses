# 执行计划：应用秒开优化

## 实施顺序

### Step 1 —— 异步化 PlayerPage / QueuePage（R1，核心）
文件：`src/App.vue`
- 顶部 import 增加：`import { defineAsyncComponent, onMounted } from 'vue'`（把现有 `onMounted` 合并进来）。
- 把：`import PlayerPage from '@/views/PlayerPage.vue'` / `import QueuePage from '@/views/QueuePage.vue'`
  改为：
  ```ts
  const PlayerPage = defineAsyncComponent(() => import('@/views/PlayerPage.vue'))
  const QueuePage = defineAsyncComponent(() => import('@/views/QueuePage.vue'))
  ```
- 模板中 `<PlayerPage>` / `<QueuePage>` 用法不变；现有 `<Transition v-if=...>` 控制时机不变。
- 校验点：`vue-tsc` / `npm run build` 无类型错；构建产物中 PlayerPage 相关代码移出主入口 JS。

### Step 2 —— manualChunks 分包（R3）
文件：`vite.config.ts`
- 在 `build.rollupOptions.output` 增 `manualChunks` 函数：`amll-pixi` / `ionic` / `vue-vendor` 三个 chunk（见 design.md §3）。
- 校验点：`npm run build` 后 `dist/assets/` 出现 `amll-pixi-*.js`（与 legacy 伴生），主入口 JS 体积显著下降。

### Step 3 —— 构建 + 类型 + lint 验证（AC4）
- `npm run build`（含 `vue-tsc`）通过。
- `npm run lint` 通过。
- 记录构建前后主入口 JS 体积、`grep -c "pixi"` 结果到本文件验证区。

### Step 4 —— 手测功能不 Regression（AC3 / AC5）
- 安卓 WebView 冷启动：首屏 TabBar + 列表可见，无 white screen / console error。
- 点开 MiniPlayer → PlayerPage：背景动画、歌词、播放/暂停/上下首/拖动进度/队列、回退按钮逻辑正常。
- 冷启动（清缓存）再次确认无 white screen。

## 验证命令

```bash
npm run build
npm run lint
# 体积核查
ls -la dist/assets/ | grep -E "index-|amll-pixi-|ionic-|vue-vendor-"
grep -c "pixi" dist/assets/index-*.js     # 期望 0（不含 -legacy 的入口名取现代产物）
```

## 风险点 / 回滚点

| 风险 | 应对 | 回滚 |
|---|---|---|
| 异步组件首次加载导致 `<Transition>` 动画时序异常 | 本轮不动 `<Transition>`；若出现，后续给 `defineAsyncComponent` 加 `loadingComponent`/`delay`，不在本次 | 改回静态 import |
| manualChunks 路径匹配在 Windows 上偏移 | Vite 传入 id 用 POSIX `/`，`includes` 兼容；构建后人工核对 `amll-pixi-*.js` 是否生成 | 删除 `build.rollupOptions` 段 |
| legacy 产物仍双份（预期，本期不动） | 属 Out of Scope，验收不依赖此点 | — |

## task.py start 前自检

- [x] prd.md 完成（已收敛，无空 Open Questions）
- [x] design.md 完成
- [x] implement.md 完成
- [x] 等用户 review / 同意后执行 `task.py start`（已执行）

## 验证记录（实施后）

### 构建产物体积对比

| chunk | 优化前 | 优化后 |
|---|---|---|
| 首屏主入口 `index-*.js` | 1,550,640 B（1.5MB） | 38,309 B（38KB）→ gzip 13.67KB |
| `amll-pixi-*.js`（异步） | 混在主 bundle | 405,434 B（独立，仅点开播放器时加载） |
| `PlayerPage-*.js`（异步） | 混在主 bundle | 6,485 B |
| `QueuePage-*.js`（异步） | 混在主 bundle | 2,227 B |
| `ionic-*.js`（vendor） | 混在主 bundle | 1,110,125 B（浏览器缓存后下次启动零成本） |
| `vue-vendor-*.js` | 混在主 bundle | 101,245 B |

- 主入口 `grep -c "pixi"` = 1，残留的是异步 chunk 清单文件名引用（`amll-pixi-*.js` / `PlayerPage-*.js`），非 Pixi 代码本体；Pixi 代码已拆到独立 chunk。
- `index.html` 不含 `amll-pixi` 同步引用（`grep -c` = 0），按需加载链路正确。
- MiniPlayer 仍在主入口（首屏底栏始终挂载，符合 spec）。

### AC 达标情况

- [x] **AC1** 主入口 JS 38KB ≤ 700KB；Pixi 代码已在独立 chunk（主入口残留 1 处为文件名引用，非代码）。
- [x] **AC2** `amll-pixi-*.js` / `PlayerPage-*.js` / `QueuePage-*.js` 独立 chunk 已生成，不在 index.html 同步链路。
- [x] **AC4** `npm run build`（vue-tsc 通过）、`npm run lint` 均通过。
- [ ] **AC3 / AC5** 需手测：安卓 WebView 冷启动首屏可见、点开播放器全屏页功能、冷启动无 white screen / console error。
