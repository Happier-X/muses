plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.muses.player.core.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// AGP 9 内置 Kotlin：jvmTarget 默认取 compileOptions.targetCompatibility

dependencies {
    api(project(":core:common"))

    // P2a Koin（BOM 统一 4.2.0）
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)

    // Room 曲库 + DataStore 设置/凭据密文（P2b：entities/DAO/MusesDatabase/Migrations 已迁 :core:common；
    // 本模块仅留平台接线 DatabaseModule，故删 ksp(room-compiler)/schemaLocation/room-testing）
    api(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    // OkHttp（AudioTagReader 下载 WebDAV 文件）
    implementation(libs.okhttp)
    
    // jaudiotagger（音频标签读取）
    implementation(libs.jaudiotagger)
    
    // WorkManager（ScanWorker 为 KoinComponent 懒注入，见 P2a R3）
    implementation(libs.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
