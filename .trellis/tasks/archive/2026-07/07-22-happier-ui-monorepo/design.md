# 技术设计：happier-ui 仓内 monorepo（S2）

## 1. 目标结构

```text
/
  package.json          # name: muses, private, workspaces: ["packages/*"]
  packages/
    happier-ui/
      package.json      # name: happier-ui, private: true, version 0.1.0
      src/
        index.ts        # 组件 + 样式入口约定
        tokens.css
        components/
          HEmptyState.vue   # 或保留 M* 文件名过渡
          HIconButton.vue
          HListRow.vue
          HSettingRow.vue
      README.md         # 简短：peer vue、如何 import tokens
  src/                  # Muses app
    components/ui/      # 薄 re-export + app-only（MCover、MPage）
    theme/
      tokens.css        # 可选：@import 'happier-ui/tokens' + muses 别名层
      variables.css     # ion 桥接仍在 app
```

## 2. 包边界（v0.1）

### 进包

| 资产 | 说明 |
|------|------|
| `tokens.css` | `--h-*` 权威 |
| EmptyState / IconButton / ListRow / SettingRow | 纯 Vue 语义壳 |

### 不进包

| 资产 | 原因 |
|------|------|
| MCover | 音乐领域 + ion-lucide |
| MPage | HOST-IONIC |
| MiniPlayer / views / features | 业务 |

### ion-icon 过渡

S1 的 IconButton 仍 `import { IonIcon } from '@ionic/vue'`。S2 选项：

**A（推荐本任务）**：包内改为 **默认 slot 图标**，或 prop `icon` 时用轻量内联 SVG/`<img>` 不依赖 Ionic；若短期保留 IonIcon，则 `peerDependenciesMeta.@ionic/vue.optional = true`，并在 README 写明「使用 path+ion-icon 时宿主需装 @ionic/vue」。

**B**：强制 peer `@ionic/vue` —— 违背「纯 Vue 优先」，仅作退路。

ListRow 默认内嵌 Cover：**进包后不要依赖 MCover**。  
做法：包内 ListRow **无默认封面组件**，仅 `start` 槽；Muses re-export 包装一层 `MListRow = HListRow + 默认 MCover` **或** 调用方继续传 cover（现 API 有 coverSrc）——若 coverSrc 需组件，app 包装：

```text
// app MListRow.vue — 可选薄封装
HListRow + 默认 slot start = MCover
```

更干净：**包 ListRow 只提供 start 槽；app 的 re-export 组件保留 coverSrc+MCover 兼容现网**。

## 3. 依赖与导出

```json
// packages/happier-ui/package.json
{
  "name": "happier-ui",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "exports": {
    ".": "./src/index.ts",
    "./tokens.css": "./src/tokens.css"
  },
  "peerDependencies": {
    "vue": "^3.5.0"
  },
  "peerDependenciesMeta": {
    "@ionic/vue": { "optional": true }
  }
}
```

根：

```json
{
  "workspaces": ["packages/*"],
  "dependencies": {
    "happier-ui": "workspace:*"
  }
}
```

npm 对 workspace 协议版本因 npm 版本而异：可用 `"happier-ui": "*"` + workspaces 提升，或 `"file:packages/happier-ui"`。实现时以本机 npm 能 `npm install` 为准。

## 4. Vite / TS

- `vite.resolve.alias` 可显式 `'happier-ui' -> packages/happier-ui/src`（双保险）
- `vue` 插件已处理 `.vue`；workspace 源码直引即可
- `optimizeDeps.include` 必要时含 `happier-ui`
- tsconfig paths：`"happier-ui": ["./packages/happier-ui/src/index.ts"]` 可选

## 5. App 兼容层

推荐保留 `src/components/ui/index.ts`：

```ts
export { HEmptyState as MEmptyState, ... } from 'happier-ui'
export { default as MCover } from './MCover.vue'
export { default as MPage } from './MPage.vue'
// 若 ListRow 需 Cover：export { default as MListRow } from './MListRow.vue' // app 包装
```

视图可继续 `@/components/ui`，**零改或少改 views**。

## 6. Token 加载

`main.ts` 现：`tokens.css` → `variables.css`。

方案：

1. `src/theme/tokens.css` 变为：
   ```css
   @import 'happier-ui/tokens.css';
   /* 若别名已在包内提供则省略；否则 app 写 --muses-* 别名 */
   ```
2. 或包 tokens 已含 muses 别名（S1 现状），整文件迁入包，app 只 `@import`。

**推荐**：S1 整文件迁入包（含 muses 别名），app `import 'happier-ui/tokens.css'`，删除重复权威定义。

## 7. 风险

| 风险 | 缓解 |
|------|------|
| workspace 安装失败 | file: 协议回退 |
| ListRow 丢默认封面 | app 包装层 |
| ESLint 扫不到 packages | eslint 配置 include packages |
| Capacitor 同步 | 仍只 build app dist，包打进 bundle |
| 双份 token | 单一权威在包内 |

## 8. 回滚

去掉 workspaces 依赖，把源码移回 `src/components/ui`；commits 按批次 revert。
