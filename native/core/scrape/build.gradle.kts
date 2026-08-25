import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// M3 刮削引擎数据层（任务 08-25-native-m3-scrape-engine）：
// 仅依赖 core:model / OkHttp / kotlinx-serialization / DataStore / coroutines，
// 禁止依赖 Compose、Room 具体类与 Media3（分层铁律见 .trellis/spec/android/index.md）
plugins {
    alias(libs.plugins.android.library)
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
    api(project(":core:model"))

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
