plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.muses.player.feature.player"
    compileSdk = 37

    defaultConfig {
        // 卡拉OK 歌词渲染依赖 Compose BlurEffect（API 31+ 生效），下限 29
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
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:media"))
    // 09-05-lyrics-kmp X3：歌词域类随 :core:common jvmShared 同包名传递
    // 歌词解析（accompanist lyrics-core 0.4.7，无 Android target 以 JVM 变体解析）
    implementation(libs.accompanist.lyrics.core)
    // 歌词渲染：AMLL 官方渲染器已 vendor 进本模块
    // （src/main/kotlin/com/mocharealm/accompanist/lyrics/ui，不再依赖 lyrics-ui AAR）
    // 见 docs/THIRD_PARTY.md
    // 封面加载（Coil 3）
    implementation(libs.coil.compose)
    // Compose foundation（含 HorizontalPager 若需）
    implementation("androidx.compose.foundation:foundation")

    testImplementation(libs.junit)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // P2a Koin（BOM 统一 4.2.0；viewModel{} DSL 在 koin-core，koinViewModel() 在 compose-viewmodel）
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.compose.viewmodel)

    debugImplementation(libs.compose.ui.tooling)
}
