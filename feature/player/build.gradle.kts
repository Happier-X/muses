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
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui-shared"))
            // compose/miuix/coil 经 ui-shared api 透传，此处不再重复声明
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；KMP sourceSets 不支持 platform(BOM)，toml bundles 已收敛版本）
            implementation(libs.bundles.koin.kmp)
        }

        // U21：jvmShared 中间层由 jvmMain 与 androidMain 共同 dependsOn（core:common 同款模式），
        // 播放屏/歌词面板一份代码双端编译；引用 core:common jvmShared 的 AmllLyricLine 等类型。
        val jvmShared by creating {
            dependsOn(commonMain.get())
            dependencies {
                // compose 基座经 commonMain 的 ui-shared api 透传；animation/lifecycle/coil 系 ui-shared 未透传或平台相关，仍需直引
                implementation(compose.animation)
                // U21：collectAsStateWithLifecycle（lifecycle 2.11 KMP 工件，双端可用）
                implementation(libs.androidx.lifecycle.runtime.compose)
            }
        }
        jvmMain.get().dependsOn(jvmShared)
        androidMain.get().dependsOn(jvmShared)
    }
}
