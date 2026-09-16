// WebDAV 基础设施：KMP 双 target（android + jvm）。
// U11 上收：WebDavClient 接口/限流器早已在 :core:common commonMain；本模块 KtorWebDavClient
// 实现（含 java.io 落盘）+ AuthRegistry + 跨平台 Koin 装配进 jvmShared（android+桌面双端共用），
// WebDavUtils（java.net）同层；DiskWebDavAudioCache/OkHttp 流播绑定（Media3 豁免）留 androidMain。
// 对 :core:data 的依赖移除——ErrorLogStore/repository 均为 :core:common 同包名（P2b 产物）。
// 形态与约束同 :core:common（android.kmp.library，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.core.webdav"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        // U11：androidUnitTest（Ktor MockEngine 429 单测等）需显式开启宿主测试（AGP9 KMP）
        withHostTest { }
    }

    sourceSets {
        // U11：jvmShared 中间层由 jvmMain 与 androidMain 共同 dependsOn（同 :core:common 接法），
        // KtorWebDavClient/AuthRegistry/WebDavUtils（java.io/java.net/runBlocking）一份代码双端编译
        val jvmShared by creating {
            dependsOn(commonMain.get())
        }
        jvmMain.get().dependsOn(jvmShared)
        androidMain.get().dependsOn(jvmShared)

        commonMain.dependencies {
            api(project(":core:common"))
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；KMP sourceSets 不支持 platform(BOM)，toml 已显式挂版本）
            implementation(libs.koin.core)
            // P2c：Ktor-client（CIO）双平台引擎；ktor-client-core 经 core:common api 透传，此处只留引擎
            implementation(libs.ktor.client.cio)
        }
        // U11：withTransactionCompat 同款问题不存在于此，但 androidMain 保留占位供 P2 actual 用
        androidMain.dependencies {
            // OkHttp 仅供 Media3 流播数据源 + AudioTagReader Range（P2c 豁免，见 WebDavModule）
            implementation(libs.okhttp)
        }
        // U11：AGP9 把 androidUnitTest 更名 androidHostTest 且默认关闭（withHostTest 显式开启）；
        // 源集由 withHostTest 创建，此处 by getting 挂依赖
        val androidHostTest by getting {
            dependencies {
                implementation(libs.junit)
                // Ktor MockEngine（WebDavClient 429 单测，不再走真实 socket）
                implementation(libs.ktor.client.mock)
                implementation(libs.robolectric)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.androidx.test.core)
            }
        }
    }
}
