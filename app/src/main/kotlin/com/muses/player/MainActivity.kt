package com.muses.player

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.muses.player.navigation.MusesApp
import com.muses.player.core.ui.theme.MusesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 底部安全区（手势条 / 三键导航）默认会被系统叠一层「对比度 scrim」——
        // 浅色页面下就是一条白/浅色带，看起来像「为了适配 inset 凭空多出一块白」。
        // 关掉它，导航栏区域就直接显示 app 内容（悬浮 dock 下方就是页面底色）。
        // 对比度 scrim 是 API 29+ 的开关，minSdk 也是 29，无需版本分支。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            MusesTheme {
                MusesApp()
            }
        }
    }
}
