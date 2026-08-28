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
        // lyrics-ui（AMLL 官方 Compose 渲染器）要求 29+；本机设备均 ≥29
        minSdk = 29
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
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:media"))
    // 歌词解析（accompanist lyrics-core 0.4.7，无 Android target 以 JVM 变体解析）
    implementation(libs.accompanist.lyrics.core)
    // 歌词渲染（accompanist lyrics-ui 1.0.19 = AMLL 官方 Compose 实现：逐词渐变/字符弹跳/距离模糊/间奏呼吸点）
    implementation(libs.accompanist.lyrics.ui)
    // 封面加载（Coil 3）
    implementation(libs.coil.compose)
    // Compose foundation（含 HorizontalPager 若需）
    implementation("androidx.compose.foundation:foundation")

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

    debugImplementation(libs.compose.ui.tooling)
}
