

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.muses.player.core.webdav"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// AGP 9 内置 Kotlin：jvmTarget 默认取 compileOptions.targetCompatibility

dependencies {
    api(project(":core:common"))
    // WebDavAuthRegistry：读音源列表与凭据构造播放流播 Basic Auth（webdav=基础设施层 ← data=数据层，方向合法）
    implementation(project(":core:data"))

    implementation(libs.kotlinx.coroutines.core)
    // P2a Koin（BOM 统一 4.2.0）
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    // WebDAV 客户端：P2c 起 Ktor-client（CIO）手写（原 OkHttp 实现已替换）；
    // OkHttp 保留仅供 Media3 流播数据源 + AudioTagReader Range（P2c 豁免，见 WebDavModule）
    implementation(libs.okhttp)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.cio)

    testImplementation(libs.junit)
    // P2c：MockWebServer 已无使用者并移除（KtorWebDavClientTest 走 MockEngine）
    // P2c：Ktor MockEngine（WebDavClient/ScrapeHttp 429 单测，不再走真实 socket）
    testImplementation(platform(libs.ktor.bom))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
}
