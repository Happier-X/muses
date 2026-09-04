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

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    // P2a Koin（BOM 统一 4.2.0；android 供 androidContext()）
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // M3-S1：kw provider JSON 解析测试（catalog 已有 okhttp-mockwebserver 条目）
    testImplementation(libs.okhttp.mockwebserver)
    // 08-27 限流：MockWebServer 在 JVM 单元测试中触发 Android Platform 检测需 robolectric shadow
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
