// 曲库 feature：KMP 双 target（android + jvm）。
// U9 曲库 ViewModel/Screen 上收 + U16 Page 层全量上收：SongsPage/AlbumsPage/ArtistsPage、
// 五个 ViewModel 与 KoinModule 全部进 commonMain（播放连接经 PlaybackPort 端口注入，
// Toast 走 PlatformToast；haze 2.0/Coil3 均为 KMP 工件），androidMain 无源码。
// 形态与约束同 :core:ui-shared（android.kmp.library，双 target，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.feature.library"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            // ViewModel 直依赖的 DAO/Repository/Relations/Mappers 均在 :core:common commonMain（P2b）
            implementation(project(":core:common"))
            implementation(project(":core:ui-shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            // androidx.lifecycle 2.8+ 的 ViewModel/viewModelScope 为 KMP 工件
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；viewModel{} DSL 在 koin-core，koinViewModel() 在 compose-viewmodel，
            // 双平台工件；KMP sourceSets 不支持 platform(BOM)，toml 已显式挂版本）
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            // U16：Page 层（多选/haze 玻璃/跳转 FAB/网格封面）全量上收——haze 2.0 KMP +
            // Coil3 KMP + AddToPlaylistSheet（playlist commonMain），androidMain 不再有源码
            implementation(project(":feature:playlist"))
            implementation(libs.coil.compose)
            implementation(libs.haze)
            // 直引 miuix（ui-shared 为 implementation 不透传）
            implementation(libs.miuix.ui)
            implementation(libs.haze.blur)
        }
    }
}
