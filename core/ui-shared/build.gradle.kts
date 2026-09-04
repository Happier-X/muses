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
        }

        androidMain.dependencies {
            // PlatformInsets：ViewCompat / WindowInsetsCompat 读取系统边衬
            implementation(libs.androidx.core.ktx)
            // PlatformBlur：Haze 真模糊（U3 T1 组件层消费；此处声明让 androidMain actual 可引用）
            implementation(libs.haze)
            implementation(libs.haze.blur)
        }
    }
}
