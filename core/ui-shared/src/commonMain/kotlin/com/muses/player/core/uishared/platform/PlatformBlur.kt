package com.muses.player.core.uishared.platform

/**
 * 平台模糊供给（设计 §3）。
 *
 * commonMain 只声明语义：模糊开关 + 强度；安卓侧 Haze 真模糊，
 * 桌面侧纯色降级（enabled=false，调用方按此开关走降级分支）。
 */
expect object PlatformBlur {
    /** 当前平台是否提供真模糊（安卓 true，桌面 false 走降级） */
    val enabled: Boolean

    /** 建议模糊半径（dp）；桌面端忽略 */
    val radiusDp: Float
}
