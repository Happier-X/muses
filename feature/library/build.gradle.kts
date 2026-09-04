plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.muses.player.feature.library"
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
}

// feature:* 只依赖 core:* 接口与 core:model，禁止直接依赖 Room/OkHttp
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    // 「加入播放列表」长按菜单复用 AddToPlaylistSheet（feature→feature 例外，后续如需解耦可下沉共享模块）
    implementation(project(":feature:playlist"))
    implementation(project(":core:media"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
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
