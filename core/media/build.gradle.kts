plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.muses.player.core.media"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    }
}

// AGP 9 内置 Kotlin：jvmTarget 默认取 compileOptions.targetCompatibility

dependencies {
    api(project(":core:common"))
    implementation(project(":core:data"))
    // WebDAV 库扫描：复用 WebDavClient（PROPFIND/GET）与 WebDavAudioCache（下载缓存/播放预热）
    implementation(project(":core:webdav"))

    implementation(libs.kotlinx.coroutines.core)
    // P2a Koin（BOM 统一 4.2.0；android 供 androidContext()）
    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin.android.set)

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
