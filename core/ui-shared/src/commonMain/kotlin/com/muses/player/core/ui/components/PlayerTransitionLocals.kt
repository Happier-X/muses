package com.muses.player.core.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 「迷你条 ↔ 沉浸页」转场用的作用域与共享元素 key。
 *
 * 为什么用 CompositionLocal 而不是逐层传参：`sharedBounds` / `sharedElement` 需要
 * `SharedTransitionScope` + `AnimatedVisibilityScope`，而两端的封面分别深埋在
 * [MiniPlayerBar] 与 [PlayerCoverHero] 内部（中间还隔着 PhoneImmersiveLayout /
 * TabletImmersiveLayout 等好几层），逐层传参会污染一串签名。由宿主 `MusesApp`
 * 统一 provide，需要的组件直接读。
 *
 * 只有 [LocalPlayerArtworkKey] 非 null 的封面（迷你条 + 沉浸页正封）才参与共享动画，
 * 列表 / 首页等处的封面保持 null，不会被误加转场。
 */
val LocalPlayerSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalPlayerAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }
val LocalPlayerArtworkKey = staticCompositionLocalOf<String?> { null }

/** 封面共享元素的 key（迷你条与沉浸页正封共用，转场时由 Compose 把封面从迷你条尺寸插值到全屏） */
const val PlayerArtworkSharedKey = "muses-player-artwork"
