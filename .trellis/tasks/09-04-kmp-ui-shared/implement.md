# 界面跨平台重构执行计划

## 有序清单

- [x] U0 地基：新建 `:core:ui-shared` KMP 模块（commonMain/androidMain/jvmMain + 编译门禁）；settings 接线；版本目录加 tabler-cmp / coil-ktor3 坐标（不动现有）。
- [x] U1 T0 平移：SaltIconButton/SaltTextButton/SaltToggle/SaltListItem/SaltEmpty/SaltNavigationDrawer + 主题 + TablerIcons 包装器；安卓 `core:ui` api 转发；构建+测试通过。
- [x] U2 平台接口：PlatformInsets/PlatformBlur/PlatformToast 双端真实实现（安卓真值/桌面降级）。
- [x] U3 T1 上收：SaltCover → SaltActionsSheet → SaltNavbar → MiniPlayerBar（Haze 经 PlatformBlurModifier 抽象）；app 层最小改动。
- [x] U4 设置页共用化（首屏）：安卓设置页与桌面设置页同源；桌面复刻设置页下线。
- [x] U8 音源管理共用化：SourceListItem/SourceFormCard + 桌面独立音源页；安卓 SourcesScreen/WebDavFormScreen 改调共用组件。
- [x] U9 WebDAV 浏览页共用化：目录列表+面包屑+加载空态；安卓改调；桌面新增浏览页（音源管理子页）+ DesktopWebDavBrowseLoader。
- [x] U10 曲库主页共用化：LibraryTabBar/LibrarySongList/AlbumGrid/ArtistGrid/SearchField；安卓 Screens 改调；桌面 LibraryScreen 升级完整版。
- [x] U5 曲目列表共用化：`SongListItem` + `SongItem` 上收至 `:core:ui-shared` commonMain；桌面 `LibraryScreen` 改为消费共用组件（本地 `SongRow` 已删除），安卓 `feature:library` 暂不动（改动量大，放后续任务）。
- [x] U6 播放页评估：**结论——留待二期**。歌词特效（逐词渐变/Blur 距离场）Haze 降级效果未用户验收，跨平台成本高于收益；桌面复刻播放页暂保留，待 Haze 降级验收后定。
- [x] U7 收尾：AC 全勾 + 全量回归通过（`:core:ui-shared:assemble` + `:core:ui-shared:allTests` + `:app:assembleMusesDebug` + `:composeApp:compileKotlinJvm` + `:app:testMusesDebugUnitTest` 均 BUILD SUCCESSFUL）。

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:ui-shared:assemble :core:ui-shared:allTests :app:assembleMusesDebug
```

## 风险文件

- `TablerIcons.kt`（包名一致，只换产物坐标；outline/filled 空基座陷阱见规范）。
- `SaltNavbar.kt` / `MiniPlayerBar.kt`（Haze + 边衬双依赖，先接口后实现）。
- `SaltShadows.kt` / `GlassSurface.kt`（T2 不动，勿顺手带入）。
