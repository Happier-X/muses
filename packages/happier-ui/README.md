# happier-ui

跨 Capacitor + Vue 应用的语义 UI 与设计 token（`--h-*`）。

## 安装（仓内）

根项目 npm workspaces 依赖本包；应用侧：

```ts
import 'happier-ui/tokens.css'
import { HIconButton, HListRow, HEmptyState, HSettingRow } from 'happier-ui'
```

## Peer

- **必选**：`vue` ^3.5
- **可选**：`@ionic/vue` — 仅当使用 `HIconButton` 的 `icon` path（内部 `ion-icon`）时需要宿主已注册 Ionic

## 不包含

- 音乐封面、播放器、Ionic 页壳（`ion-page`）
- Material elevation / 整库 Ionic 复刻

## 视觉

首版参照 HeroUI Native 主题角色与触控节奏；主色 HeroUI blue `#006FEE`。
