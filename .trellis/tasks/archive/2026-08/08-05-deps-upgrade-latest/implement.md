# 实现清单：deps-upgrade-latest

## npm

- [x] N1：`npx npm-check-updates` 应用 patch/minor（排除已决定 pin 的包）
- [x] N2：Capacitor core/cli/android → 8.5.x；相关插件 latest；`npx cap sync android`
- [x] N3：vue-router → 5.x
- [x] N4：vue-tsc → 3.x；typescript → **pin 6.0.3**（见下方 Pin 说明）；`moduleResolution: bundler` + `ignoreDeprecations: "6.0"`
- [x] 复核 lucide `@/icons` 导出；picomatch overrides（保持）
- [x] `npm run lint` && `npm run build`

## Android

- [x] A1：`variables.gradle` / `app/build.gradle` 升 stable（core 1.19、activity 1.13、webkit 1.16、documentfile 1.1、okhttp 5.4、gms 4.5…）；**compileSdk 37**（core 1.19 硬要求），targetSdk 仍 36
- [x] A2：OkHttp 5 适配三处 Kotlin 插件（`Response.body` 非空）
- [x] A3：Kotlin 2.4.10；AGP 9.3.1；Gradle wrapper **9.5.0**；proguard-android-optimize.txt；`android.builtInKotlin=false` + `android.newDsl=false` 兼容 Capacitor 旧 variant API
- [x] A4：`gradlew :app:assembleDebug` **BUILD SUCCESSFUL**
- [x] 故意不升项写入 prd/implement 备注（Pixi7、happier-ui、jaudiotagger…）

## Pin / 故意不升

| 项 | 版本 | 原因 |
|----|------|------|
| typescript | **6.0.3**（非 7.0.2） | TS7 下 `vue-tsc` 无法解析 `typescript/lib/tsc`；`typescript-eslint@8` peer 为 `>=4.8.4 <6.1.0`，直接拒绝 TS7 |
| happier-ui | 0.0.8 精确 | D3 生态硬钉 |
| @pixi/* / AMLL | 7.x / 现网 | D3 不升 Pixi 8 |
| jaudiotagger | 3.0.1 | 已是 latest |
| appcompat / fragment / coordinator / splash | 现 stable 顶 | 仅 rc 可更新，按 D4 不跟 pre-release |
| targetSdk | 36 | 仅升 compileSdk 满足 AAR；不强制新运行时 |

## 收尾

- [x] AC 勾选；journal 摘要
- [x] 提交分段：`d024eb3` npm / `aaedc84` android / archive 待本轮

## 验证

```bash
npm outdated   # 仅剩 typescript wanted=6.0.3 latest=7.0.2（故意 pin）
npm run lint   # 通过
npm run build  # 通过
npx cap sync android  # 已执行
cd android && ./gradlew :app:assembleDebug  # BUILD SUCCESSFUL
```
