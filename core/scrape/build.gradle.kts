import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// M3 刮削引擎数据层（任务 08-25-native-m3-scrape-engine）：
// 仅依赖 core:common / kotlinx-serialization / DataStore / coroutines，
// 禁止依赖 Compose、Room 具体类与 Media3（分层铁律见 .trellis/spec/android/index.md）
// W3 KMP 化（任务 09-05-scrape-kmp）：引擎全量已上收 :core:common commonMain
// （text/cover/queue/editmeta/writeback + ScrapeHttp + WebDavClient 接口 + TagPort），
// 本模块缩为安卓装配瘦壳（ScrapeModule Koin + LyricsPorts 适配），依赖 core:common 即可。
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
    // ScrapeModule 安卓装配残留：AudioTagReader 缓存失效器（audioTagCacheInvalidator）
    implementation(project(":core:data"))
    // editmeta 歌词维度的 Port 适配（L3）：scrape(编排) → lyrics(实现) 无环
    implementation(project(":core:lyrics"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    // P2a Koin（BOM 统一 4.2.0；android 供 androidContext()）
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
}
