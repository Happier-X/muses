import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.material3)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            api(project(":desktop"))
            // :desktop 用 implementation 不透传 :core:common，composeApp 需直接依赖
            api(project(":core:common"))
            implementation(project(":core:ui-shared"))
            // Room/SQLite/DataStore/JNA 不透传，桌面直接调用 DAO 时需显式声明
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.datastore.preferences)
            implementation(libs.jna)
            implementation(libs.jna.platform)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            // W4 桌面装配（任务 09-05-scrape-kmp）：JaudiotaggerTagPort 在 :core:common jvmShared，
            // 其 jaudiotagger 依赖为 implementation 作用域不透传，桌面消费 TagPort 需显式声明（同版本线）
            implementation(libs.jaudiotagger)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.muses.player.desktop.MainKt"
        // S4 打包：jpackage 需完整 JDK（含 jpackage.exe），Android Studio jbr 不带；
        // 本机 jdk-21.0.11+10 即打包用 JDK（与 :desktop jvmToolchain(21) 同版本线）。
        javaHome = System.getenv("MUSES_DESKTOP_JDK") ?: "C:/Users/zhf52/java/jdk-21.0.11+10"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Muses"
            // CI 发版经 -Pmuses.desktop.version 从 tag 注入，本地不传时回落 1.0.0
            packageVersion = (project.findProperty("muses.desktop.version") as String?) ?: "1.0.0"
            description = "Muses Music Player"
            vendor = "Muses"

            // v0.5.2 实测：不配置则 MSI 装完无任何入口，用户找不到应用。
            // upgradeUuid 固定 UpgradeCode，缺省时每次构建随机，后续版本无法覆盖升级。
            windows {
                menu = true
                menuGroup = "Muses"
                shortcut = true
                dirChooser = true
                upgradeUuid = "5d86c48d-082d-4cb1-911f-17f7fe6676c5"
                // 与安卓占位同设计语言（深色圆底 + 浅蓝播放三角），经 jpackage --icon 注入
                iconFile = file("icons/muses.ico")
            }
        }
    }
}
