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
include(":core:model")
include(":core:data")
include(":core:webdav")
include(":core:media")
include(":feature:library")
include(":feature:player")
include(":feature:playlist")
include(":feature:sources")
