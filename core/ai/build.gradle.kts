// AI 推荐（首页「猜你喜欢」）：曲库画像 → LLM（OpenAI 兼容）→ 歌名/歌手 → 平台精确匹配。
//
// 分层约束：本模块只做「曲库 → 建议 → 可播曲目」的数据链路，不碰 UI 与播放；
// 落地结果复用 :core:search 的 OnlineSearchResult，与搜索/榜单共用同一条播放链路。
//
// 隐私口径：发送给 AI 的内容 = **聚合统计 + 抽样曲目**（用户已在设置页知情选择），
// 不发送文件路径、来源地址、凭据。见 LibraryProfile.toPromptText()。
//
// 依赖形态同 :core:search（android.kmp.library，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.core.ai"
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
            // 曲库画像读 SongDao、推荐配置读 SettingsRepository（均在 :core:common）
            api(project(":core:common"))
            // 推荐落地：AI 给的歌名/歌手 → 平台搜索匹配真实曲目
            implementation(project(":core:search"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
// koin-core（jvmShared AiModule 消费）版本经 Koin BOM 统一（4.2.0，同 :core:common）
dependencies {
    add("jvmSharedImplementation", platform(libs.koin.bom))
}
