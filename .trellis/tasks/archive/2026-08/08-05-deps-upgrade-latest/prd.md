# 将项目依赖升级到最新版本

## Goal

把 **npm 依赖** 与 **Android Gradle / OkHttp / AndroidX** 升到当前可获取的 **稳定最新**；升级后根据版本变化修改业务/配置代码，使 `lint` / `build` 与 Android 可构建前提尽量成立。

## Decisions locked

| # | 决策 | 选择 |
|---|------|------|
| D1 | 升级策略 | **B 激进**：含 **TypeScript 7**、**vue-router 5**、**vue-tsc 3** 及全部可升 npm major/minor/patch |
| D2 | 原生范围 | **含 Android**：AGP / Kotlin / OkHttp / AndroidX / google-services / documentfile 等升稳定最新 |
| D3 | 生态硬钉 | **不**强升 Pixi 8（AMLL peer 钉 Pixi 7 模块）；**不**为 router5 引入 Pinia；`happier-ui` 已 latest 0.0.8 保持精确版 |
| D4 | 稳定渠道 | Android/Maven **优先 last stable**（跳过 alpha/beta/rc）；npm 跟 `latest` tag |
| D5 | 失败回退 | 若 TS7 或 AGP9 导致工具链/插件不可用：先修代码与配置；仍失败则 **记录原因并 pin 到可构建的最高版本**，不静默放弃其它已升包 |

## Background

### npm（2026-08-05）

| 包 | → 目标 |
|----|--------|
| Capacitor core/android/cli | **8.5.0** |
| file-picker / native-audio | patch |
| lucide / tanstack / vite / eslint / terser / plugin-legacy | latest within ncu |
| vue-router | **5.2.0**（官方：无 file-based 无 breaking） |
| vue-tsc | **3.3.x** |
| typescript | **7.0.2**（高风险） |
| 已 latest | vue、happier-ui、AMLL、多数 cap 插件、tailwind、esbuild、jss… |

### Android 稳定最新（Maven 查询）

| 组件 | 当前 | 目标 stable |
|------|------|-------------|
| AGP | 8.13.0 | **9.3.1**（注意 Capacitor 插件 proguard 兼容） |
| Kotlin Gradle Plugin | 2.1.21 | **2.4.10** |
| OkHttp | 4.12.0 | **5.4.0**（WebDAV/播放下载用；需审 `TimeUnit`/API） |
| androidx.core | 1.17.0 | **1.19.0** |
| activity | 1.11.0 | **1.13.0** |
| webkit | 1.14.0 | **1.16.0** |
| documentfile | 1.0.1 硬编码 | **1.1.0** |
| google-services | 4.4.4 | **4.5.0** |
| appcompat / fragment / coordinator / splash / espresso | 已是 stable 顶或仅 rc 更新 | 保持或仅升 stable |
| jaudiotagger | 3.0.1 | 已 latest |
| Gradle wrapper | 8.14.3 | 随 AGP9 兼容表调整（实现时查官方） |

### 代码侧预期修改点

1. **package.json + lockfile**；复核 `picomatch` overrides。
2. **TS7 + vue-tsc3**：修类型/tsconfig；若 API 不兼容则 pin 并写说明。
3. **vue-router 5**：预期零改；验证 `createRouter` / Tabs。
4. **Capacitor 8.5**：`npx cap sync android`。
5. **OkHttp 5**：检查 `AudioPlayerPlugin` / `WebDavPlugin` / `WebDavAudioCache`（`OkHttpClient`、`Request`、`TimeUnit`、body API）。
6. **AGP 9**：`proguard-android.txt` → `proguard-android-optimize.txt`（app + 受影响插件）；Kotlin 2.4；必要时升 Gradle wrapper。
7. **不改**产品功能与 versionName（仍 0.2.4，除非另开任务）。

## Requirements

1. 按 D1–D5 升级 npm 与 Android 依赖至稳定最新（或文档化 pin 原因）。
2. 版本变更引起的编译/类型/原生构建问题在代码或配置中修复。
3. `npm run lint` + `npm run build` 通过。
4. Capacitor 主版本对齐并 `cap sync`；Android 工程在本机能力内尽力 `./gradlew` 验证（环境缺失则记录）。
5. 不借升级大改产品交互；不引入 Pinia 仅满足 optional peer。

## Out of Scope

- 发版号 bump / Play 上架
- Pixi 8 / 重写 AMLL 渲染
- iOS 工程
- 改写 changelog 旧条目（可新增升级笔记到 journal）

## Acceptance Criteria

- [x] AC1：`npm outdated` 在约定范围内清空，或 prd/implement 注明故意 pin 及原因（**typescript pin 6.0.3**，见 implement.md）
- [x] AC2：lint + build 通过
- [x] AC3：Capacitor 相关包对齐；已执行 sync（或等价更新 android 拷贝）
- [x] AC4：Android variables/AGP/Kotlin/OkHttp 达稳定最新或有 pin 说明；OkHttp 调用点已适配
- [x] AC5：TS7/vue-tsc3/router5 相关代码或配置已处理（TS7 按 D5 pin 6.0.3；vue-tsc3 + router5 已落地）
- [x] AC6：journal 记录升级摘要与回退点

## Notes

- 复杂；必须有 `design.md` + `implement.md`。
- 建议提交分段：npm 工具链 → Capacitor sync → Android/OkHttp，便于回滚。
