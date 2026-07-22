# 技术设计：happier-ui S3 冒烟应用

## 1. 目标

```text
packages/happier-ui     # 被测库
apps/happier-ui-smoke   # 第二宿主：仅依赖 vue + happier-ui（+ 可选 capacitor）
muses (src/)            # 第一宿主：不变业务
```

证明：换宿主后 token 与组件仍可用。

## 2. 冒烟应用结构

```text
apps/happier-ui-smoke/
  package.json          # name: happier-ui-smoke, private
  index.html
  vite.config.ts
  tsconfig.json
  capacitor.config.ts   # appId: app.happier.ui.smoke
  src/
    main.ts
    App.vue             # 单页：展示 HEmptyState / HListRow / HSettingRow / HIconButton
    style.css           # 可极简 reset
  README.md
```

### App.vue 内容（验收画面）

1. 引入 tokens 后的 primary 色块或标题色
2. `HEmptyState` 标题+说明
3. `HListRow` 两行（一条 `playing`）
4. `HSettingRow` + 原生 `input type=checkbox` 于 end（无 ion-toggle）
5. `HIconButton` 用 **slot 内 SVG**（不测 ion-icon path）

## 3. Workspaces

根 `package.json`：

```json
"workspaces": ["packages/*", "apps/*"]
```

冒烟依赖：

```json
"dependencies": {
  "vue": "^3.5.40",
  "happier-ui": "file:../../packages/happier-ui"
}
```

在 workspaces 下也可用 `"happier-ui": "*"`；以 `npm install` 成功为准。

根脚本可选：

```json
"build:smoke": "npm run build -w happier-ui-smoke"
```

## 4. Capacitor

- `capacitor.config.ts`：`webDir: 'dist'`
- **本任务默认**：安装 `@capacitor/core` + `@capacitor/cli`（dev），执行一次 `npx cap add android` **可选**  
  - 若 android 目录过大/噪声：仅保留 config + web build，README 写「需要时 cap add」
- **推荐默认**：加 config + 文档步骤；**不**强制提交完整 `android/` 工程（体积大），除非用户要求完整 cap 工程

**折中（实现默认）**：

- 提交 `capacitor.config.ts` + README 中的 `cap add android && cap sync`
- **不**提交 `android/` 除非用户明确要求

## 5. Vite 解析

冒烟 app 的 vite alias（与 Muses 类似）：

```ts
'happier-ui' -> ../../packages/happier-ui/src/index.ts
'happier-ui/tokens.css' -> ../../packages/happier-ui/src/tokens.css
```

保证源码直引 `.vue`。

## 6. 与 Ionic

冒烟 **不** 引入 `@ionic/vue`，验证「纯 Vue 宿主」路径。  
Muses 仍走 Ionic + optional ion-icon。

## 7. 风险

| 风险 | 缓解 |
|------|------|
| workspaces 多 app 安装变慢 | 仅一个 smoke app |
| HIconButton 无 Ionic 时 ion-icon 失败 | 冒烟只用 slot SVG |
| CSS @import 路径 | vite 别名 tokens.css |
| 误把 smoke 打进 muses build | 独立 package scripts |

## 8. 回滚

删除 `apps/happier-ui-smoke`，workspaces 改回 `packages/*`。
