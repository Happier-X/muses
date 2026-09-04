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
    // U1 T0：Salt 纯组件 + 主题 + TablerIcons 包装器已上收至 ui-shared（commonMain KMP），
    // 此处 api 透传，feature:/app 消费者 import 路径不变（同包名 com.muses.player.core.ui.*）。
    api(project(":core:ui-shared"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.coil.compose)
    // 远程封面加载：Coil 3 网络引擎（okhttp 实现；不加则 AsyncImage 加载 https 静默失败）
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // 真磨砂：底部 MiniPlayer / 顶部导航的 Haze 背景模糊（api 透传给 feature:* 页面）
    api(libs.haze)
    api(libs.haze.blur)
    // Tabler Icons 已随 ui-shared 传递，无需重复声明（空基座陷阱）
}
