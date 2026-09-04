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
include(":core:ui")
include(":core:data")
include(":core:webdav")
include(":core:media")
include(":core:scrape")
include(":core:lyrics")
include(":feature:scrape")
include(":feature:library")
include(":feature:player")
include(":feature:playlist")
include(":feature:sources")

// P3-S2：桌面播放端口纯 JVM 模块（VLCJ 只进本模块；composeApp 三屏/S4 打包另起任务）
include(":desktop")
