# 所有依赖升级到最新版

## Goal

将项目依赖升级到最新版（npm + Android Gradle 依赖），构建/检查全绿，播放器核心功能不回归。

## Background（升级范围侦察）

### npm（11 个有新版，10 个可升）

| 包 | 当前 → 最新 | 类型 |
|----|------------|------|
| @lucide/vue | 1.28.0 → 1.31.0 | minor |
| @tanstack/vue-form | 1.33.3 → 1.33.5 | patch |
| @vitejs/plugin-legacy | 8.2.2 → 8.2.3 | patch |
| esbuild | 0.28.1 → 0.28.2 | patch |
| eslint | 10.8.0 → 10.8.1 | patch |
| motion-v | 2.3.0 → 2.4.0 | minor |
| terser | 5.49.1 → 5.50.0 | minor |
| typescript | 6.0.3 → 7.0.2 | **major，不升**（见下） |
| vite | 8.2.0 → 8.2.1 | patch |
| vue | 3.5.40 → 3.5.41 | patch |
| vue-tsc | 3.3.9 → 3.3.10 | patch |

### TypeScript 7 兼容性研究结论（2026-08 网络调研）

- TS 7.0（2026-07-08 GA）为 Go 原生编译器，**无程序化 API（programmatic API 计划 7.1）**；
- vue-tsc / typescript-eslint 等依赖 TS 的 JS API（LanguageService），**Vue 项目暂不能使用 TS 7**；
- 结论：`typescript` **保持 6.0.3**（6.x 最新），其余 10 个包全部升级；后续 TS 7.1 + vue-tsc 支持后再评估。

### 已是最新（无需动）

@capacitor/*（8.5.x）、@capgo/*（native-audio 8.4.19、media-session 8.0.29）、@applemusic-like-lyrics/*、@pixi/*、@vueuse/core、vue-router、@capawesome/* 等（npm outdated 未列出 = 已最新/范围满足）。

### Android Gradle 依赖（需核查 Maven 最新版）

AGP 9.3.1 / Kotlin 2.4.10 / okhttp 5.4.0 / androidx.*（variables.gradle）——与 Capacitor 模板同步，核查后仅升级有新版且兼容的。

## Acceptance Criteria

- [ ] npm：除 typescript 外全部升到最新（`npm outdated` 仅剩 typescript 7 一项，且标注为有意保留）
- [ ] Android：可升级的 gradle 依赖已升（AGP/Kotlin/androidx/okhttp 等，如 Maven 有新版且兼容）
- [ ] `npm run lint`（src 0 错误）+ `npm run build`（vue-tsc + vite）全绿
- [ ] `./gradlew :app:compileDebugKotlin` + `assembleDebug` 通过
- [ ] 播放器/歌词/WebDAV/通知等核心功能代码无破坏性变更（diff 审查）
- [ ] debug APK 重建

## Out of Scope

- typescript 7 升级（等待 7.1 programmatic API + vue-tsc 支持）
- iOS 平台
- 功能开发
