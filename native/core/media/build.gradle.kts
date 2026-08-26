plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.muses.player.core.media"
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
    implementation(project(":core:data"))
    // WebDAV 库扫描：复用 WebDavClient（PROPFIND/GET）与 WebDavAudioCache（下载缓存/播放预热）
    implementation(project(":core:webdav"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.coroutines.core)

    // Media3 播放栈（PlaybackService 在阶段 3 实现）
    api(libs.media3.exoplayer)
    api(libs.media3.session)
    api(libs.media3.common)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.datasource)

    // 本地库扫描：MediaStore + jaudiotagger 标签；WorkManager 后台任务
    implementation(libs.jaudiotagger)
    implementation(libs.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
