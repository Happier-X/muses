// 应用壳 feature：KMP 双 target（android + jvm）。
// U22 全量上收：MusesApp（CMP Navigation 导航壳）/NavDestination/TabsLayout/
// MainViewModel（PlaybackPort 驱动）/SettingsScreen 进 commonMain——导航自 :app 的
// androidx.navigation 换为 CMP Navigation（U18 已验证同包名委托），权限申请与
// 平台动作（浏览器/剪贴板）抽 expect/actual（platform/），androidMain 仅余 actual。
// 依赖面 = :core:common + :core:ui-shared + 全 feature 屏（导航路由消费）。
// 形态同 :feature:library（android.kmp.library，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.feature.shell"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui-shared"))
            implementation(project(":feature:library"))
            implementation(project(":feature:player"))
            implementation(project(":feature:playlist"))
            implementation(project(":feature:scrape"))
            implementation(project(":feature:sources"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            // U22：CMP Navigation（org.jetbrains.androidx.navigation，android 变体委托 androidx 同包名）
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.haze)
            implementation(libs.haze.blur)
            // miuix 设置行（SwitchPreference；ui-shared 为 implementation 不透传，此处直连）
            implementation(libs.miuix.preference)
            // 第一阶段迁移：TabsLayout 等改用 miuix Text/Icon，需直引 miuix-ui
            implementation(libs.miuix.ui)
        }

        // U22：jvmShared 中间层由 jvmMain 与 androidMain 共同 dependsOn（core:common 同款模式），
        // 导航壳一份代码双端编译；URLEncoder/SimpleDateFormat 等 JVM API 在此可用。
        val jvmShared by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.animation)
                // TabsLayout 用 miuix Text/Icon，需直引 miuix-ui
                implementation(libs.miuix.ui)
            }
        }
        jvmMain.get().dependsOn(jvmShared)
        androidMain.get().dependsOn(jvmShared)
    }
}
