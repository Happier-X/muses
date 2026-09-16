# 第三方源码声明

本项目当前没有以源码形式 vendored（内嵌）的第三方代码。

---

## Accompanist lyrics-ui（历史记录，U21 已下线）

沉浸式播放页曾以内嵌源码方式引入 AMLL 官方 Compose 渲染器（`com.mocharealm.accompanist:lyrics-ui`，Apache 2.0），
U21 已改写为自研渲染（`feature/player/.../lyric/LyricsPanel.kt` 起，另见文件头移植说明），内嵌源码全部删除。
U24 起连 `lyrics-core` 外部依赖也已摘除（唯一消费者随 U21 删除）。
当前源码中无任何 `com.mocharealm.*` 引用，无需保留第三方许可声明；此处仅保留历史记录供追溯。

---

## 其他依赖（非源码内嵌，通过 Gradle 引入）

- 其余依赖见 `gradle/libs.versions.toml` 与各模块 `build.gradle.kts`
