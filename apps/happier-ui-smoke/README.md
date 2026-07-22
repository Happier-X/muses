# happier-ui-smoke

S3 冒烟应用：证明 **`happier-ui` 可在第二 Capacitor + Vue 宿主** 中使用（无 Muses 业务、无 Ionic 壳）。

## 命令（仓库根）

```bash
npm install
npm run dev:smoke    # http://localhost:5174
npm run build:smoke
```

或在本目录：

```bash
npm run dev
npm run build
```

## 覆盖组件

- `happier-ui/tokens.css`（`--h-*` / primary 色条）
- `HEmptyState`
- `HListRow`（含 `playing`）
- `HSettingRow` + 原生 checkbox
- `HIconButton`（**slot SVG**，不依赖 `@ionic/vue`）

## Capacitor（可选）

本目录已含 `capacitor.config.ts`（`webDir: dist`）。需要原生工程时：

```bash
cd apps/happier-ui-smoke
npm run build
npx cap add android   # 首次
npx cap sync
npx cap open android
```

**默认不提交** `android/` 目录（体积大）；按需本地生成。

## 说明

- 依赖仓内 `packages/happier-ui`（workspaces / file:）
- 不 import Muses `@/` 或业务模块
