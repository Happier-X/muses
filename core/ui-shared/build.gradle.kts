// U0 地基（任务 09-04-kmp-ui-shared）：KMP 界面共享模块。
// 形态参考 :core:common（android.kmp.library，双 target），但只做界面：
//   - commonMain：Salt 纯组件 + 主题 + 平台接口（零安卓 import）
//   - androidMain：安卓实现（边衬/模糊/Toast 真实现）
//   - jvmMain：桌面占位实现（U2 完善真实现，供 composeApp/desktop 消费）
// 约束：不动 :core:ui / feature:* / app 现有代码；不升级版本线。
// Compose Multiplatform 插件与 composeApp 同版本（1.12.0-rc01）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.core.uishared"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            // Tabler Icons（KMP 坐标：outline + filled；注意空基座陷阱：不要单独声明 icons-tabler-cmp）
            implementation(libs.tabler.icons.cmp)
            implementation(libs.tabler.filled.icons.cmp)
            // Coil 3（KMP 图片加载；commonMain 提供 AsyncImage API）
            implementation(libs.coil.compose)
            // Haze（U2 纠偏：2.0 起为 KMP 工件——android/jvm 等变体已发布，Gradle 按 target 解析；
            // 原「Android only」注释过时）。commonMain 声明使 androidMain/jvmMain actual 均可引用真模糊 API
            implementation(libs.haze)
            implementation(libs.haze.blur)
            // Haze 公共类型（KMP：HazeState / HazeInput 等跨平台类型）
            implementation(libs.haze.utils)
            // miuix（小米 HyperOS 风格 CMP 组件库 + Preference 行；KMP 父件按 target 解析，
            // 版本线与本工程精确对齐见 toml 注释；SaltTheme 内桥接 MiuixTheme，明暗同源）
            implementation(libs.miuix.ui)
            implementation(libs.miuix.preference)
        }

        androidMain.dependencies {
            // Coil 3 网络引擎（okhttp 实现；不加则 AsyncImage 加载 https 静默失败）
            implementation(libs.coil.network.okhttp)
        }

        jvmMain.dependencies {
            // Coil 3 网络引擎（ktor3 实现；桌面端 HTTP 图片加载）
            implementation(libs.coil.network.ktor3)
        }
    }
}
