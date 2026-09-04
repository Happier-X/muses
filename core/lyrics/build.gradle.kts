import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 歌词在线搜索数据层（任务 08-25-native-lyrics-online）：
// 仅依赖 core:common / OkHttp / kotlinx-serialization / coroutines，
// 禁止依赖 Compose、Room、Media3（分层铁律见 .trellis/spec/android/index.md）
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.muses.player.core.lyrics"
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

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    // P2a Koin（BOM 统一 4.2.0）
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // P2c：Ktor MockEngine（provider 解析单测；MockWebServer 已无使用者并移除）
    testImplementation(platform(libs.ktor.bom))
    testImplementation(libs.ktor.client.mock)
}
