plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.muses.player.feature.player"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            // AMLL 歌词 WebView 前端（amll/index.html + assets/*）经 WebViewAssetLoader
            // 以 https://appassets.androidplatform.net/assets/amll/ 加载；
            // 自定义目录名 androidAssets 需显式注册，否则不打进 APK（
            // WebView 报 net::ERR_INVALID_RESPONSE，歌词面板白屏错误页）
            assets.srcDir("src/main/androidAssets")
        }
    }
}

// AGP 9 内置 Kotlin：jvmTarget 默认取 compileOptions.targetCompatibility

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:media"))
    // M2：歌词解析（仅解析不渲染；0.4.7 无 Android target，以 JVM 变体解析）
    implementation(libs.accompanist.lyrics.core)
    // M2：AMLL WebView 桥接
    implementation(libs.androidx.webkit)
    // 封面加载（PlayerScreen AsyncImage 所需，与 library/app 同一版本线）
    implementation(libs.coil.compose)

    testImplementation(libs.junit)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Hilt ViewModel
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coil 3（封面加载）
    implementation(libs.coil.compose)

    debugImplementation(libs.compose.ui.tooling)
}
