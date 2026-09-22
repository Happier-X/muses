// 在线搜索（各平台官方接口直连，为在线音源提供「拿到 songmid」的能力）。
//
// 背景：洛雪自定义源脚本只有 musicUrl/lyric/pic，**没有 search**——
// 搜索必须自行对接各平台接口（与洛雪客户端内置搜索同思路）。
//
// 分层约束：本模块只做「关键词 → 统一结果（含 musicInfoJson）」，不碰 UI 与播放；
// 结果可直接构造 :core:common 的 OnlineTrackRef 交给播放链路。
// 依赖形态同 :core:common（android.kmp.library，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.core.search"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        withHostTest { }
    }

    sourceSets {
        // jvmShared 中间层：androidMain 与 jvmMain 共同 dependsOn（Koin 装配一份双端共用）
        val jvmShared = create("jvmShared") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.koin.core)
            }
        }
        jvmMain.get().dependsOn(jvmShared)
        androidMain.get().dependsOn(jvmShared)

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            // 搜索结果直接产出 OnlineTrackRef/musicInfo，需 :core:common 引用模型
            api(project(":core:common"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
// koin-core（jvmShared SearchModule 消费）版本经 Koin BOM 统一（4.2.0，同 :core:common）
dependencies {
    add("jvmSharedImplementation", platform(libs.koin.bom))
}
