// Muses 原生工程（M1）— 模块结构见父任务 design.md 第 1 节
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "muses-native"

include(":app")
include(":core:common")
// U24：:core:ui 空壳透传模块删除（U1 T0 起组件全在 :core:ui-shared，消费方直连）
// U0 地基（09-04-kmp-ui-shared）：KMP 界面共享模块
include(":core:ui-shared")
include(":core:data")
include(":core:webdav")
include(":core:media")
include(":core:scrape")
// 洛雪自定义音源脚本引擎（纯引擎层：QuickJS + Ktor，不碰 UI/播放）
include(":core:lxsdk")

// 在线搜索（各平台官方接口直连，为在线音源提供 songmid）
include(":core:search")
// AI 推荐（曲库画像 → LLM → 歌名/歌手 → 平台匹配，供首页「猜你喜欢」）
include(":core:ai")
// 首页（搜索框 + 排行榜 + 猜你喜欢）
include(":feature:home")
include(":feature:scrape")
include(":feature:library")
include(":feature:player")
include(":feature:playlist")
include(":feature:sources")

// U22：应用壳（MusesApp/TabsLayout/MainViewModel/SettingsScreen 双端共享，CMP Navigation）
include(":feature:shell")

// P3-S2：桌面播放端口纯 JVM 模块（VLCJ 只进本模块；composeApp 三屏/S4 打包另起任务）
include(":desktop")

// P3-S3：composeApp(desktop) Compose Multiplatform UI 壳
include(":composeApp")
