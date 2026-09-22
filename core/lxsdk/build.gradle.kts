// 洛雪自定义音源脚本引擎（LX Music custom source）：
// 纯引擎层 —— 只依赖 quickjs-kt + Ktor + serialization，禁止依赖 Compose/Room/Media3/播放器。
// 分层约束：本模块不碰 UI 与播放链路，便于单测；消费方按需接入。
// 形态与约束同 :core:common（android.kmp.library，不升级版本线）。
// 引擎可行性结论见 spike-lx/README.md（QuickJS 选型 + 3 个必踩坑）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.core.lxsdk"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        // 单测需显式开启宿主测试（AGP9 KMP）；Android 单测按 quickjs-kt 官方要求
        // 经 dependencySubstitution 替换为 -jvm 构件（见文件末尾），故此处可跑真实引擎
        withHostTest { }
    }

    sourceSets {
        // jvmShared 中间层：androidMain 与 jvmMain 共同 dependsOn，
        // JCE 系实现（LxCryptoJvm）一份代码双端编译
        val jvmShared = create("jvmShared") {
            dependsOn(commonMain.get())
            dependencies {
                // Koin 装配（lxSdkModule）；KMP sourceSets 不支持 platform(BOM)，
                // 版本经 toml 显式挂（与 :core:common jvmShared 同款接法）
                implementation(libs.koin.core)
            }
        }
        jvmMain.get().dependsOn(jvmShared)
        androidMain.get().dependsOn(jvmShared)

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            // QuickJS 绑定（KMP 父件；Gradle 按 target 解析 android/jvm 变体）。
            // implementation：引擎细节不外泄，消费方只需 :core:common 的端口
            implementation(libs.quickjs.kt)
            // HTTP 传输：宿主 lx.request 落在此客户端上（core 经 :core:common 版本线一致）
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            // OnlineTrackResolver / OnlineTrackRef 端口与引用模型在 :core:common
            api(project(":core:common"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        // 引擎集成测试放 jvmTest：真实 QuickJS native 库 + Ktor MockEngine（纯 JVM 环境）
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

// QuickJS 原生库绑定：Android 单元测试跑在桌面 JVM 上，无法加载 android 变体的 .so，
// 按 quickjs-kt 官方 README 要求把 android 构件替换为 jvm 构件（插桩测试在真机上无需替换）。
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("io.github.dokar3:quickjs-kt-android"))
            .using(module("io.github.dokar3:quickjs-kt-jvm:${libs.versions.quickjs.get()}"))
    }
}

// koin-core（jvmShared lxSdkModule 消费）版本经 Koin BOM 统一（4.2.0，同 :core:common）
dependencies {
    add("jvmSharedImplementation", platform(libs.koin.bom))
}
