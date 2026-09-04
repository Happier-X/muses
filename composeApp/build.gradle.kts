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
            // Room/SQLite/DataStore/JNA 不透传，桌面直接调用 DAO 时需显式声明
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.datastore.preferences)
            implementation(libs.jna)
            implementation(libs.jna.platform)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.muses.player.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Muses"
            packageVersion = "1.0.0"
            description = "Muses Music Player"
            vendor = "Muses"
        }
    }
}
