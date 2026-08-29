

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
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
        versionName = (findProperty("android.injected.versionName") as String?) ?: "0.4.6"
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
    implementation(project(":core:model"))
    implementation(project(":feature:scrape"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:webdav"))
    implementation(project(":core:media"))
    implementation(project(":feature:library"))
    implementation(project(":feature:playlist"))
    implementation(project(":feature:player"))
    implementation(project(":feature:sources"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // WorkManager（Hilt Worker 集成）
    implementation(libs.work.runtime.ktx)

    // Coil 3（封面加载，阶段 1+ 实际使用）
    implementation(libs.coil.compose)

    // jaudiotagger（音频标签读取）
    implementation(libs.jaudiotagger)

    testImplementation(libs.junit)
}
