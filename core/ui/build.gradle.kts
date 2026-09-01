plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.muses.player.core.ui"
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

// AGP 9 内置 Kotlin：jvmTarget 默认取 compileOptions.targetCompatibility

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material.icons)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // 真磨砂：底部 MiniPlayer / 顶部导航的 Haze 背景模糊（api 透传给 feature:* 页面）
    api(libs.haze)
    api(libs.haze.blur)
}
