package com.muses.player.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * 模糊背景供给（miuix-blur）。
 *
 * - app 层（TabsLayout）在内容容器上 `Modifier.layerBackdrop(backdrop)` 捕获背景，
 *   经本 CompositionLocal 下发；磨砂表面（迷你条等）经
 *   `Modifier.musesBackdropBlur(backdrop, ...)` 消费。
 * - miuix-blur 为 KMP 工件（android/jvm 变体），commonMain 直接引用真类型，
 *   原 haze 时代的 `Any?` 类型擦除桥接随 haze 下线一并移除。
 * - 未提供（null）时磨砂表面回退半透明纯色背景（见 musesBackdropBlur）。
 */
val LocalMusesBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
