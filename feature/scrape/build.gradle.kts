// 刮削 feature：KMP 双 target（android + jvm）。
// U14 全量上收：五个 VM（Scrape/Review/EditMeta/QueueAccess + ReviewQueueTracker）与
// 三屏（ScrapeScreen/ScrapeReviewScreen/EditMetaSheet）进 commonMain——依赖面全为
// :core:common KMP 产物（W1-W3 刮削引擎 + Salt 组件 + Coil3），零平台差异。
// 桌面复用共享刮削体验（原 composeApp 手搓装配层删除）；Koin 装配
// scrapeFeatureModule 双端共用（SavedStateHandle 桌面端由宿主模块显式供给）。
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
        namespace = "com.muses.player.feature.scrape"
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
            // SavedStateHandle 随 lifecycle-viewmodel 2.8+ KMP 工件（ScrapeReviewViewModel 构造）
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.coil.compose)
            // 直引 miuix（ui-shared 为 implementation 不透传）
            implementation(libs.miuix.ui)
            // P2a Koin（统一 4.2.0；KMP sourceSets 不支持 platform(BOM)，toml 已显式挂版本）
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
