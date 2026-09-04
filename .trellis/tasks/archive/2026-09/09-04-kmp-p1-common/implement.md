# P1 执行计划

## 有序清单

- [x] S0 环境探针：KMP 2.4.10 可解析；AGP 9 下旧 library+KMP 不可用，改官方新插件（偏离 D1）。
- [x] S1 骨架：`:core:common` 空模块双 target 一次通过。
- [x] S2 搬迁 model：6 文件移入，删 `:core:model`；repoint 提前（配置期校验要求）。
- [x] S3 搬迁纯文件：审计后实搬 5 文件 + `FailureCopyTest`（1 文件 6 用例，双端通过）；10 个留守（偏离 D2，证据见 PRD）。
- [x] S4 新建 `PlayerPort`（签名偏离 D3，`playerConfig` 已认领，P2 承接）。
- [x] S5 消费者 repoint（11 处）→ `:app:assembleMusesDebug` + 全仓单测通过。
- [x] S6 CI 纯度门禁；`core:model` kts 引用清零；`core/model/build` 残留已删。

## 验证命令（一键）

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:common:assemble :core:common:allTests :app:assembleMusesDebug testDebugUnitTest
grep -rn "core:model" --include="*.kts" app core feature gradle settings.gradle.kts
```

## 风险文件

- `settings.gradle.kts`、`gradle/libs.versions.toml`（构建入口，改坏全仓红）。
- 消费者 build 脚本批量 repoint（漏改即编译错，`grep` 兜底）。
- `LyricsProviderUtil`/`TextMetaProvider` 若被留守文件反向依赖，编译期暴露，届时二选一：跟随留守或连带上移（以编译为准，记录决策）。
