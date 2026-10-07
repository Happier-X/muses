// 首页 feature：KMP 双 target（android + jvm），形态同 :feature:sources / :feature:library。
//
// 首页：主题横幅、猜你喜欢/排行榜/随心听入口、音乐分类卡片。
// - 主题横幅：真实精选歌单轮播；分类：关键词交给在线搜索页；
// - 排行榜：:core:search 的平台榜单接口（QQ/酷狗/网易，免登录）；
// - 猜你喜欢：:core:ai 的曲库画像 → LLM → 平台精确匹配。
//
// 播放链路与在线搜索页一致：脚本可用性校验 → OnlineTrackSession 登记 → PlaybackPort。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.feature.home"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            // 榜单（OnlineChartService）+ 搜索结果同构模型
            implementation(project(":core:search"))
            // AI 推荐（画像 → LLM → 匹配）
            implementation(project(":core:ai"))
            // 播放前校验平台是否有可用音源脚本（与在线搜索页同口径）
            implementation(project(":core:lxsdk"))
            implementation(project(":core:ui-shared"))
            implementation(compose.components.resources)
            // compose/miuix 经 ui-shared api 透传，此处不再重复声明
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.datastore.preferences)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.bundles.koin.kmp)
        }
    }
}

compose.resources {
    packageOfResClass = "com.muses.player.feature.home.resources"
}
