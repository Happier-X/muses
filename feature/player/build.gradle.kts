// 播放 feature：KMP 双 target（android + jvm）。
// U21 播放页/歌词链全量上收 jvmShared：PlayerScreen/QueueScreen/FlowingLightBackdrop/
// LyricsPanel（AMLL 渲染）双端一份——原生 glyph 绘制（nativeCanvas+BlurMaskFilter）重写为
// 跨平台 TextMeasurer+Shadow 方案，SystemClock/系统分享抽 expect/actual（platform/）；
// androidMain 仅余平台 actual。队列展示字段由 VM 按曲库组合（QueueRow），端口只暴露
// songId 有序集。形态同 :feature:library（android.kmp.library，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.feature.player"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui-shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；KMP sourceSets 不支持 platform(BOM)，toml 已显式挂版本）
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            // U17：歌词外围组件（Stubs.Artwork 封面 AsyncImage）进 commonMain
            implementation(libs.coil.compose)
            // 直引 miuix（ui-shared 为 implementation 不透传）
            implementation(libs.miuix.ui)
        }

        // U21：jvmShared 中间层由 jvmMain 与 androidMain 共同 dependsOn（core:common 同款模式），
        // 播放屏/歌词面板一份代码双端编译；引用 core:common jvmShared 的 AmllLyricLine 等类型。
        val jvmShared by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.animation)
                // U21：collectAsStateWithLifecycle（lifecycle 2.11 KMP 工件，双端可用）
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.coil.compose)
            }
        }
        jvmMain.get().dependsOn(jvmShared)
        androidMain.get().dependsOn(jvmShared)
    }
}
