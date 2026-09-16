// 播放列表 feature：KMP 双 target（android + jvm）。
// U19 播放列表全量上收：PlaylistsPage/PlaylistDetailPage/PlaylistDetailViewModel
// （原 androidMain）进 commonMain——播放依赖经 PlaybackPort 端口注入（不再依赖
// core:media），haze 2.0 为 KMP 工件，androidMain 无源码。
// 形态与约束同 :feature:library（android.kmp.library，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.feature.playlist"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui-shared"))
            // compose/miuix 经 ui-shared api 透传，此处不再重复声明
            // androidx.lifecycle 2.8+ 的 ViewModel/viewModelScope 为 KMP 工件
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；KMP sourceSets 不支持 platform(BOM)，toml bundles 已收敛版本）
            implementation(libs.bundles.koin.kmp)
        }
    }
}
