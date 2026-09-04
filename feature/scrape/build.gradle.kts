plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.muses.player.feature.scrape"
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

// AGP 9 内置 Kotlin：jvmTarget 默认取 android.compileOptions.targetCompatibility

dependencies {
    // core:model 提供 Song/ScrapeCandidate 等领域模型；core:data 提供曲库仓库；
    // core:scrape 为 M3 数据层（队列/历史/匹配/写回/editmeta 编排，只消费不改内部）；
    // core:ui 为 Salt 组件体系。不碰实现库（jaudiotagger 等）。
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:scrape"))
    implementation(project(":core:ui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)

    // 封面候选网格远程图加载（与 library/app 同一版本线）
    implementation(libs.coil.compose)

    implementation(libs.kotlinx.coroutines.core)

    // ViewModel 注入
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    debugImplementation(libs.compose.ui.tooling)

    // S4：待审队列状态机单测（纯 JVM）
    testImplementation(libs.junit)
}
