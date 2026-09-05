import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// M3 刮削引擎数据层（任务 08-25-native-m3-scrape-engine）：
// 仅依赖 core:common / OkHttp / kotlinx-serialization / DataStore / coroutines，
// 禁止依赖 Compose、Room 具体类与 Media3（分层铁律见 .trellis/spec/android/index.md）
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.muses.player.core.scrape"
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
    // 写回编排：曲库/音源/凭据仓库 + WebDAV 客户端接口 + TagWriter（分层规则见 spec/android/index.md）
    implementation(project(":core:data"))
    implementation(project(":core:webdav"))
    implementation(project(":core:media"))
    // editmeta 歌词维度的 Port 适配（L3）：scrape(编排) → lyrics(实现) 无环
    implementation(project(":core:lyrics"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    // P2a Koin（BOM 统一 4.2.0；android 供 androidContext()）
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // P2c：Ktor MockEngine（429/解析单测，不再走真实 socket）
    testImplementation(platform(libs.ktor.bom))
    testImplementation(libs.ktor.client.mock)
    // W2 纯逻辑上收：text/cover/queue/editmeta 测试随实现迁 core:common commonTest（robolectric/androidx.test.core 已零使用，移除）
}
