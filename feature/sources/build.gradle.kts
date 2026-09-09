// 音源 feature：KMP 双 target（android + jvm）。
// U20 全量上收：音源列表页 SourcesScreen（原 androidMain）进 commonMain——
// SAF 选目录抽为 rememberLocalFolderPicker expect/actual（androidMain=SAF，
// jvmMain=Swing），扫描经 commonMain [LibraryScanPort] 注入（androidMain 绑定
// MediaStore/WebDAV 两扫描器），haze 2.0 为 KMP 工件。
// 形态与约束同 :feature:library / :feature:playlist（android.kmp.library，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.feature.sources"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:webdav"))
            implementation(project(":core:ui-shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；KMP sourceSets 依赖处理器不支持 platform(BOM)，toml 已显式挂版本）
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            // U20：列表页 haze 玻璃（2.0 起 KMP 工件，双端同源）
            implementation(libs.haze)
            // 直引 miuix Switch（ui-shared 为 implementation 不透传）
            implementation(libs.miuix.ui)
            implementation(libs.haze.blur)
        }

        androidMain.dependencies {
            // 扫描器（MediaStore/WebDAV，安卓媒体栈，AndroidLibraryScanPort 消费）
            implementation(project(":core:media"))
            // SAF 系统目录选择器（rememberLauncherForActivityResult）
            implementation(libs.androidx.activity.compose)
        }
    }
}
