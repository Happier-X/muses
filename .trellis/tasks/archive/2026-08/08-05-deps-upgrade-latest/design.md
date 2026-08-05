# 设计：依赖升级（B + Android）

## 原则

1. **先升声明，再修断裂**：`ncu` / 手改 version → `npm install` → lint/build → 原生。
2. **稳定优先于 alpha**：AndroidX/AGP 用 last stable。
3. **生态钉死如实记录**：Pixi7、happier-ui 0.0.8 不硬跨。
4. **Capacitor 与 AGP9**：升 cap 8.5 后尝试 AGP 9.3.1；proguard 默认文件名按 AGP9 要求改；第三方插件若仍引用 `proguard-android.txt` 优先升插件，否则 app 侧/文档 pin AGP 到 8.13.x 最高可用。

## npm 批次

| 批次 | 内容 | 验证 |
|------|------|------|
| N1 | patch/minor（vite、eslint、tanstack、lucide、cap 插件…） | lint/build |
| N2 | Capacitor 8.5 全家 | lint/build + cap sync |
| N3 | vue-router 5 | 路由编译；Tabs 导入 |
| N4 | vue-tsc 3 + typescript 7 | `vue-tsc` / build；失败则按 D5 pin |

## Android 批次

| 批次 | 内容 | 验证 |
|------|------|------|
| A1 | variables：core/activity/webkit/documentfile/okhttp… | IDE/gradle 解析 |
| A2 | OkHttp 5 代码适配 | 编译 WebDAV/下载路径 |
| A3 | Kotlin 2.4.10 + AGP 9.3.1 + Gradle wrapper 兼容 + gms 4.5.0 | `assembleDebug` 若环境允许 |
| A4 | proguard 文件名与 cap 插件 | 构建不因 missing proguard 失败 |

## 代码触点地图

- `package.json` / lock / 可能 `vite.config` overrides
- `tsconfig*.json`（TS7 选项）
- `src/router`、`TabsPage`（router5 冒烟）
- `android/build.gradle`、`variables.gradle`、`app/build.gradle`、`gradle-wrapper.properties`
- `AudioPlayerPlugin.kt`、`WebDavPlugin.kt`、`WebDavAudioCache.kt`

## 回滚

- git 按提交批次回退
- TS7 失败 → typescript@5.9 + 可配合的 vue-tsc
- AGP9 失败 → AGP 8.13.x + 已升的 AndroidX/OkHttp 可保留（若兼容）
