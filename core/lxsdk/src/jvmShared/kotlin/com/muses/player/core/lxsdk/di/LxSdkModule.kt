package com.muses.player.core.lxsdk.di

import com.muses.player.core.lxsdk.LxOnlineTrackResolver
import com.muses.player.core.lxsdk.LxScriptRepository
import com.muses.player.core.lxsdk.crypto.LxCrypto
import com.muses.player.core.lxsdk.crypto.LxCryptoJvm
import com.muses.player.core.lxsdk.http.LxHttpClient
import com.muses.player.core.lxsdk.store.FileLxScriptStore
import com.muses.player.core.lxsdk.store.LxScriptStore
import com.muses.player.core.model.online.OnlineTrackResolver
import org.koin.dsl.module

/**
 * 洛雪自定义音源（在线音源）Koin 装配。
 *
 * 装配链：
 * ```
 * LxScriptStore（文件持久化）
 *   → LxScriptRepository（多脚本管理 + 按 platform 路由）
 *     → LxOnlineTrackResolver（适配 :core:common 的 OnlineTrackResolver 端口）
 *       → 双端播放器（Android PlayerConnection / 桌面 JvmPlayerPort）
 * ```
 *
 * ## 为什么脚本同步是「懒」的
 * 仓库经 `storedScriptsProvider` 在**首次真正使用时**才去存储拉取脚本，
 * 而不是在构造期或 `scope.launch` 里 eager 读取。原因：
 * - Android 的 `PlatformDirs.appDataDir()` 依赖 `initPlatformDirs(context)` 先执行；
 * - 播放端口链路（PlaybackPort → PlayerConnection → OnlineTrackResolver →
 *   LxScriptRepository → LxScriptStore）在 `MainViewModel` 创建时就会被 Koin 解析，
 *   此时目录初始化可能尚未完成，eager 访问会直接崩（实测：
 *   `IllegalArgumentException: initPlatformDirs 未初始化就调用 appDataDir`）。
 * 懒同步后，构造只创建轻量对象，任何目录访问都发生在用户真正播放/管理脚本时。
 */
fun lxSdkModule() = module {
    single<LxCrypto> { LxCryptoJvm() }
    single { LxHttpClient() }
    single<LxScriptStore> { FileLxScriptStore() }
    single {
        val store: LxScriptStore = get()
        LxScriptRepository(
            crypto = get(),
            httpClient = get(),
            storedScriptsProvider = {
                // 只登记已启用脚本；引擎仍按需加载（首次请求某平台时才建 QuickJS runtime）
                store.list().filter { it.enabled }.map { it.id to it.source }
            },
        )
    }
    single<OnlineTrackResolver> { LxOnlineTrackResolver(get()) }
}
