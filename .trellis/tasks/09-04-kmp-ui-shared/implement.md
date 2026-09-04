# 界面跨平台重构执行计划

## 有序清单

- [ ] U0 地基：新建 `:core:ui-shared` KMP 模块（commonMain/androidMain + 空编译门禁）；settings 接线；版本目录加 tabler desktop / coil-ktor 坐标（不动现有）。
- [ ] U1 T0 平移：SaltIconButton/SaltTextButton/SaltToggle/SaltListItem/SaltEmpty/SaltNavigationDrawer + 主题 + TablerIcons 包装器；安卓 `core:ui` 转发；截图对照。
- [ ] U2 平台接口：PlatformInsets/PlatformBlur/PlatformToast（+ FilePicker 预留）+ 双端实现。
- [ ] U3 T1 上收：SaltCover → SaltActionsSheet → SaltNavbar → MiniPlayerBar，每件独立提交 + 对照。
- [ ] U4 设置页共用化（首屏）：安卓设置页与桌面设置页同源；桌面复刻设置页下线。
- [ ] U5 曲目列表共用化：桌面复刻库房页下线。
- [ ] U6 播放页评估：Haze 降级可接受则迁，否则留待二期；桌面复刻播放页视结果下线。
- [ ] U7 收尾：AC 全勾 + 全量回归 + 归档。

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:ui-shared:assemble :core:ui-shared:allTests :app:assembleMusesDebug
```

## 风险文件

- `TablerIcons.kt`（包名一致，只换产物坐标；outline/filled 空基座陷阱见规范）。
- `SaltNavbar.kt` / `MiniPlayerBar.kt`（Haze + 边衬双依赖，先接口后实现）。
- `SaltShadows.kt` / `GlassSurface.kt`（T2 不动，勿顺手带入）。
