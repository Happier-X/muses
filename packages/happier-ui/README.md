# happier-ui

跨 Capacitor + Vue 应用的语义 UI 与设计 token（`--h-*`）。

## 安装（仓内）

根项目 npm workspaces 依赖本包；应用侧：

```ts
import 'happier-ui/tokens.css'
import { HIconButton, HListRow, HEmptyState, HSettingRow } from 'happier-ui'
```

### 第二宿主冒烟

见 **`apps/happier-ui-smoke`**（S3）：

```bash
npm run dev:smoke
npm run build:smoke
```

## Peer

- **必选**：`vue` ^3.5
- **`HIconButton`**：优先 **slot** 自定义图标；`icon` path 使用 Web Component `ion-icon`，宿主需自行加载 Ionic core（包 **不** import `@ionic/vue`）

## 不包含

- 音乐封面、播放器、Ionic 页壳（`ion-page`）
- Material elevation / 整库 Ionic 复刻

## 视觉

首版参照 HeroUI Native 主题角色与触控节奏；主色 HeroUI blue `#006FEE`。
