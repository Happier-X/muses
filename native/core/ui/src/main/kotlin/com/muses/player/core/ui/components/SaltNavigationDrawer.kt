package com.muses.player.core.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 导航抽屉打开回调（对照 Web 层 navigationDrawerKey provide/inject）：
 * TabsPage 主框架 provide，MNavbar 内自动渲染汉堡按钮消费。
 * null = 当前无抽屉（平板 aside 形态 / 覆盖路由）。
 */
val LocalSaltOpenDrawer = staticCompositionLocalOf<(() -> Unit)?> { null }
