

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.muses.player"
    compileSdk = 37

    defaultConfig {
        // 卡拉OK 歌词渲染依赖 Compose BlurEffect（API 31+ 才生效），下限由 26 抬到 29
        minSdk = 29
        targetSdk = 36
        // CI 发布经 -Pandroid.injected.version* 注入（tag 名/提交总数）；
        // AGP 9 中 DSL 显式赋值会覆盖 injected 属性，故必须在此主动读取
        versionCode = (findProperty("android.injected.versionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("android.injected.versionName") as String?) ?: "0.5.1"
    }

    // 渠道维度：主包 com.muses.player（覆盖安装旧 Web 版）+ MIUI 定制包
    flavorDimensions += "channel"
    productFlavors {
        create("muses") {
            dimension = "channel"
            applicationId = "com.muses.player"
        }
        create("miui") {
            dimension = "channel"
            applicationId = "com.miui.player"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// AGP 9 内置 Kotlin 支持：jvmTarget 默认取 android.compileOptions.targetCompatibility

dependencies {
    // 模块依赖：app 聚合全部 core/feature
    implementation(project(":core:common"))
    implementation(project(":feature:scrape"))
    // U24：:core:ui 空壳删除，消费方直连 :core:ui-shared
    implementation(project(":core:ui-shared"))
    implementation(project(":core:data"))
    implementation(project(":core:webdav"))
    implementation(project(":core:media"))
    // P2a：AppKoinModule 直接聚合 lyrics/scrape 的 Koin 模块，需直连依赖（implementation 非传递）
    // 09-05-lyrics-kmp X3：lyricsModule 已上收 :core:common jvmShared 同包名，随 core:common 依赖可达
    implementation(project(":core:scrape"))
    implementation(project(":feature:library"))
    implementation(project(":feature:playlist"))
    implementation(project(":feature:player"))
    implementation(project(":feature:sources"))
    // U22：应用壳（MusesApp/TabsLayout/SettingsScreen + CMP Navigation 导航）双端共享
    implementation(project(":feature:shell"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose（U24：app 无自有 UI，仅 MainActivity setContent 壳需要 runtime/ui 基线；
    // MusesTheme/组件经 :core:ui-shared 与 :feature:shell 提供）
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    // P2a Koin（BOM 统一 4.2.0，无散装版本号）
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)

    // WorkManager（ScanWorker 为 KoinComponent 懒注入，见 P2a R3）
    implementation(libs.work.runtime.ktx)

    // jaudiotagger（MusesApplication 启动期 TagOptionSingleton.setAndroid(true) 强制安卓分支）
    implementation(libs.jaudiotagger)

    testImplementation(libs.junit)
}
