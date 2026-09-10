// 应用壳 feature：KMP 双 target（android + jvm）。
// U22 全量上收：MusesApp（导航壳）/NavDestination/TabsLayout/
// MainViewModel（PlaybackPort 驱动）/SettingsScreen 进 commonMain；导航为 miuix-nav
// 类型化路由（替代 CMP Navigation），权限申请与平台动作抽 expect/actual（platform/）。
// 依赖面 = :core:common + :core:ui-shared + 全 feature 屏（导航路由消费）。
// 形态同 :feature:library（android.kmp.library，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    // miuix-nav 路由 @Serializable 编译期支持（commonMain MusesRoute 密封层级）
    alias(libs.plugins.kotlin.serialization)
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
            // miuix-nav 自研导航运行时（连续栈深度 + HyperOS 转场 + 跟手返回，替代 CMP Navigation）；
            // kotlinx-serialization-json 供路由栈 savedstate 序列化（Saver 经 json 实现）
            implementation(libs.miuix.nav)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            // miuix-blur（TabsLayout layerBackdrop 捕获背景；磨砂消费层在 ui-shared）
            implementation(libs.miuix.blur)
            implementation(libs.miuix.squircle)
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
