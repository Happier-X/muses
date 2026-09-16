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
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui-shared"))
            // compose/miuix/coil 经 ui-shared api 透传，此处不再重复声明
            // SavedStateHandle 随 lifecycle-viewmodel 2.8+ KMP 工件（ScrapeReviewViewModel 构造）
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；KMP sourceSets 不支持 platform(BOM)，toml bundles 已收敛版本）
            implementation(libs.bundles.koin.kmp)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
