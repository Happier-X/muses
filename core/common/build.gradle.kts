// P1 KMP 公共模块（任务 09-04-kmp-p1-common）：android + jvm 双 target。
// AGP 9 起 KMP 模块必须用 com.android.kotlin.multiplatform.library；Android 配置收敛在
// kotlin { android { ... } } 内（AGP 把扩展挂在 Kotlin 扩展上，不再有顶层 android {}）。
// commonMain 只收严格平台无关代码；androidMain/jvmMain 暂空占位供 P2 actual 用。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.core.common"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
