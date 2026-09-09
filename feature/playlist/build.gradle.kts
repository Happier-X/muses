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
            // androidx.lifecycle 2.8+ 的 ViewModel/viewModelScope 为 KMP 工件
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；KMP sourceSets 不支持 platform(BOM)，toml 已显式挂版本）
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            // U19：Page 层 haze 玻璃（2.0 起 KMP 工件，双端同源）
            implementation(libs.haze)
            // 直引 miuix（ui-shared 为 implementation 不透传）
            implementation(libs.miuix.ui)
            implementation(libs.haze.blur)
        }
    }
}
