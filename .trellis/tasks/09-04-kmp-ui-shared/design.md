# 界面跨平台重构技术设计

## 1. 目标结构

```text
:core:ui-shared（新建 KMP 模块，commonMain + androidMain）
  ├─ commonMain：Salt 纯组件 + 主题 + 图标包装器 + 平台接口（边衬/模糊/Toast/文件选择）
  ├─ androidMain：安卓实现（Haze 真模糊 + 系统边衬 + Toast + 文件选择）
  └─ composeApp/desktop：桌面实现（模糊降级 + 零边衬 + 桌面文件选择）
:core:ui（现安卓库）：瘦身为转发层，逐组件标记 Deprecated 指向 ui-shared
feature:* / app：不动业务逻辑，只换 import
```

- 新模块而非改造 `core:ui`：`core:ui` 是安卓库形态，改类型风险大；新建 KMP 模块后老模块做转发，安卓可逐组件切换、可回滚。
- 图标：`icons-tabler-*-cmp-android` → 桌面端换 `icons-tabler-*-desktop`（同版本 2.2.1，同包名 `com.composables.icons.tabler`，只换坐标）；`TablerIcons` 包装器平移，语义名不变。
- Coil3：`coil-compose` 已是 KMP，桌面直接复用；`coil-network-okhttp` 换 `coil-network-ktor`（桌面）/保留 okhttp（安卓）。

## 2. 组件分档

| 档 | 组件 | 依赖 | 动作 |
|---|---|---|---|
| T0 直接上收 | SaltIconButton/SaltTextButton/SaltToggle/SaltListItem/SaltEmpty/SaltNavigationDrawer/主题色字形/TablerIcons | 零安卓依赖 | 逐文件平移，包名不变 |
| T1 条件上收 | SaltCover（Coil）/SaltActionsSheet（边衬）/SaltNavbar（边衬+Haze）/MiniPlayerBar（Haze） | Coil/Haze/边衬均有跨平台解 | Coil 换 KMP 产物；边衬抽参数；Haze 经平台接口（安卓真模糊/桌面降级） |
| T2 暂留 | GlassSurface（Build.VERSION）/SaltShadows（android.graphics）/MusesHaze（降级策略） | 安卓图形/系统判断 | 单独抽象或降级，不在首批 |

## 3. 平台接口（commonMain 只定接口）

- `PlatformInsets`：状态栏/导航栏高度（安卓读 WindowInsets，桌面给 0/标题栏高）。
- `PlatformBlur`：模糊开关 + 强度（安卓 Haze 真模糊，桌面纯色降级）。
- `PlatformToast`：短提示（安卓 Toast，桌面状态栏文案/小浮层）。
- `PlatformFilePicker`：文件选择（安卓 ActivityResult，桌面 AWT FileDialog）——设置页暂不用，预留。

## 4. 迁移顺序

1. 建 `:core:ui-shared` 空模块 + 编译门禁（T0 地基）。
2. T0 平移 + 安卓转发 + 截图对照（按钮/开关先行）。
3. T1 逐个上收（封面→动作表→导航→迷你条），每件独立提交。
4. 设置页共用化（首屏，表单开关 mostly T0）。
5. 曲目列表共用化 → 桌面复刻设置页/库房页下线。
6. 播放页视 Haze 降级效果定（歌词特效保持降级，不硬上）。

## 5. 兼容与回滚

- 安卓行为冻结：每件迁移前后截图对照 + `assembleMusesDebug`；老 `core:ui` 组件保留转发，删转发前需双端截图。
- 回滚：单组件单提交，`git revert`；新模块删除即回滚。
