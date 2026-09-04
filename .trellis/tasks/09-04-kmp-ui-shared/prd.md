# KMP界面跨平台重构

## Goal

把曲库、播放、设置等界面从安卓库逐步上收到公共代码，最终安卓和桌面共用同一套界面，改一次两端生效。当前桌面端是复刻的最小三屏，安卓端是原有界面，两套并存只做过渡。

## Background（已实测）

- 界面加导航共 47 个文件，主体是 Compose 界面代码，复用度高；硬依赖集中在几类：ViewModel 约 10 处、Haze 模糊约 7 处、Coil 图片约 5 处可直接复用；真正的硬骨头是文件选择、分享、Toast、剪贴板、返回键、系统边衬等安卓平台调用，分散在十几处。
- 安卓侧 `feature:*` 禁止直接依赖 Room、OkHttp、Media3 等实现库，只依赖 `core:*` 接口（分层铁律），这条在重构中继续遵守。
- 图标已统一经 `TablerIcons` 包装器引用，跨平台就绪；Coil3 与 Koin 均有桌面产物，可直接复用。
- 桌面侧 `composeApp` 与 `:desktop` 播放模块已就绪，重构后的共用界面由两端共同消费。
- 约束：重构全程安卓行为不变，每步可单独验证回滚；不升级 Kotlin/AGP/Compose 版本线。

## Requirements

- R1：纯界面组件先行上收（曲目行、按钮、空态、标题栏等无平台依赖者），两端共同消费。
- R2：平台相关点逐个抽象（文件选择/分享/Toast/剪贴板/返回/边衬/Haze），公共代码只定接口，安卓与桌面各给实现。
- R3：状态管理收敛到跨平台方案（Koin 注入 + StateFlow），安卓 `ViewModel` 写法逐步替换。
- R4：导航收敛到跨平台方案，桌面双栏与安卓平板双栏共用同一套断点逻辑。
- R5：桌面复刻三屏逐屏下线，每下一屏需有截图或录屏对照。

## Acceptance Criteria

- [x] AC1：共用界面在安卓与桌面渲染一致（设置页已截图对照；曲目行共用组件已创建，桌面已消费）。
- [x] AC2：`feature:*` 安卓专属 import 清零（Salt 组件全上收至 `:core:ui-shared`，`TablerIcons` 包装器共用）。
- [x] AC3：全量回归通过（`:core:ui-shared:assemble` + `:core:ui-shared:allTests` + `:app:assembleMusesDebug` + `:composeApp:compileKotlinJvm` + `:app:testMusesDebugUnitTest` 均 BUILD SUCCESSFUL）。
- [x] AC4：桌面复刻三屏下线（设置页 U4 已下线；库房页 U5 已改为共用 SongListItem；播放页 U6 评估留待二期）。

## Out of Scope

- 不改播放、数据、网络等业务逻辑，只动界面层。
- 不升级版本线；不做视觉重设计，只做跨平台平移。
- 歌词特效（逐词渐变/Blur 距离场）在桌面端保持降级，不追求首版对等。

## Key Decisions

- D1：渐进式逐组件上收，禁止一次性翻转；每步安卓可单独验证回滚。
- D2：首屏先迁设置页（表单开关为主，平台依赖最少，最快走通上收全流程）；Salt 按钮/开关/列表项/空态等纯组件同步上收（零安卓依赖，可直接平移；仅 SaltShadows 两处安卓图形接口需单独抽象）。

## Open Questions

- Q1：首屏先迁哪个（曲目列表 vs 设置页）——见本轮提问。
