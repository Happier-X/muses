package com.muses.player.core.media.playback

import android.os.Bundle
import android.util.Log
import androidx.media3.session.MediaSession
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * 平台 MediaSession 歌词桥：向系统会话 metadata 注入 `android.media.metadata.LYRICS` 全文字段。
 *
 * 背景：CarWith / 蓝牙车机读取"当前活跃媒体会话" metadata 中的 LYRICS key 显示歌词
 * （洛雪音乐、椒盐音乐的"蓝牙歌词/车载歌词"即走此字段）。media3 的 [androidx.media3.common.MediaMetadata]
 * 是白名单字段对象，携带不了自定义 key；本桥绕过它，直取 media3 内置的 legacy
 * MediaSessionCompat（对外平台 token 即出自该会话），裸 Bundle 注入后回写。
 *
 * 反射链（media3-session 自带 legacy 包，逐层缓存，全部是三方库内部字段 + 其 public API，
 * 不触碰任何平台 hidden API）：
 * ```
 * MediaSession.impl → MediaSessionImpl.sessionLegacyStub → MediaSessionLegacyStub.sessionCompat
 *   └ getController()/getMetadata()/getBundle()  读现状（public）
 *   └ setMetadata(MediaMetadataCompat)           回写（public；(Bundle) 构造为包私有，反射一次）
 * ```
 *
 * 行为约定：
 * - **幂等短路**：回写前经 controller 读系统会话现有 metadata，LYRICS 与目标相同直接返回。
 *   media3 每次整体重建 metadata（白名单重建）都会丢掉 LYRICS，下一次 [push]
 *   （随 100ms 歌词轮询）自动补回——最终一致，丢失窗口 ≤ 轮询周期。
 * - **fail-soft**：任一步骤失败仅打一条 w 日志并永久禁用本桥，绝不影响播放与通知。
 * - 除注入的 LYRICS 外，bundle 其余内容（title/artist/封面等）以系统会话当前值为基底原样保留。
 *
 * 注意：读现状走 MediaControllerCompat.getMetadata()（binder 到系统会话服务），主线程同步调用
 * 是 androidx 官方常规用法；幂等短路保证无变化时只读不写。
 */
internal object SessionLyricsBridge {

    private const val TAG = "SessionLyricsBridge"

    /** 与洛雪/椒盐对齐的全文歌词 key（平台 MediaMetadata.METADATA_KEY_LYRICS 的字面量） */
    private const val KEY_LYRICS = "android.media.metadata.LYRICS"

    @Volatile
    private var dead = false

    // 反射链字段缓存：MediaSession.impl → sessionLegacyStub → sessionCompat
    private var fieldSessionImpl: Field? = null
    private var fieldLegacyStub: Field? = null
    private var fieldSessionCompat: Field? = null

    // legacy 包 public 方法（反射调用，编译期零依赖）+ MediaMetadataCompat 包私有 (Bundle) 构造
    private var getControllerMethod: Method? = null
    private var getMetadataMethod: Method? = null
    private var getBundleMethod: Method? = null
    private var setMetadataMethod: Method? = null
    private var compatMetaCtor: Constructor<*>? = null

    private var initialized = false

    /**
     * 把 [lyricsRaw]（LRC 全文原文）写入平台会话 metadata 的 LYRICS key；null 表示移除该 key。
     * 可高频调用：幂等短路保证重复调用开销 ≈ 一次 binder 读。
     */
    fun push(media3Session: MediaSession?, lyricsRaw: String?) {
        if (dead || media3Session == null) return
        if (!ensureInitialized()) return
        try {
            val compat = sessionCompat(media3Session) ?: return

            // 读系统会话现状（public API 链：getController → getMetadata → getBundle）
            val controller = getControllerMethod!!.invoke(compat) ?: return
            val currentMeta = getMetadataMethod!!.invoke(controller)
            val existing = currentMeta?.let { getBundleMethod!!.invoke(it) as Bundle? }

            if (existing?.getString(KEY_LYRICS) == lyricsRaw) return

            val bundle = if (existing == null) Bundle() else Bundle(existing)
            if (lyricsRaw == null) {
                bundle.remove(KEY_LYRICS)
            } else {
                bundle.putString(KEY_LYRICS, lyricsRaw)
            }
            setMetadataMethod!!.invoke(compat, compatMetaCtor!!.newInstance(bundle))
        } catch (t: Throwable) {
            dead = true
            Log.w(TAG, "注入 LYRICS 失败，已禁用该桥（不影响播放/通知）", t)
        }
    }

    private fun ensureInitialized(): Boolean {
        if (initialized) return true
        return try {
            val compatClass = Class.forName("androidx.media3.session.legacy.MediaSessionCompat")
            val controllerClass = Class.forName("androidx.media3.session.legacy.MediaControllerCompat")
            val metaClass = Class.forName("androidx.media3.session.legacy.MediaMetadataCompat")
            getControllerMethod = compatClass.getMethod("getController").also { it.isAccessible = true }
            getMetadataMethod = controllerClass.getMethod("getMetadata").also { it.isAccessible = true }
            getBundleMethod = metaClass.getMethod("getBundle").also { it.isAccessible = true }
            setMetadataMethod = compatClass.getMethod("setMetadata", metaClass).also { it.isAccessible = true }
            compatMetaCtor = metaClass.getDeclaredConstructor(Bundle::class.java).also { it.isAccessible = true }
            initialized = true
            true
        } catch (t: Throwable) {
            dead = true
            Log.w(TAG, "初始化 legacy MediaSessionCompat 反射失败，歌词桥禁用", t)
            false
        }
    }

    // ── 反射链：MediaSession.impl → sessionLegacyStub → sessionCompat ──

    private fun sessionCompat(media3Session: MediaSession): Any? {
        val impl = read(media3Session, fieldSessionImpl, "impl") { fieldSessionImpl = it } ?: return null
        val stub = read(impl, fieldLegacyStub, "sessionLegacyStub") { fieldLegacyStub = it } ?: return null
        return read(stub, fieldSessionCompat, "sessionCompat") { fieldSessionCompat = it }
    }

    /** 读 [target] 的 [cached] 字段；未缓存则按 [name] 沿类与父类链解析并缓存 */
    private fun read(target: Any, cached: Field?, name: String, store: (Field) -> Unit): Any? {
        (cached ?: resolveField(target.javaClass, name)?.also(store))?.let { return it.get(target) }
        return null
    }

    private fun resolveField(cls: Class<*>, name: String): Field? {
        var c: Class<*>? = cls
        while (c != null) {
            try {
                return c.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                c = c.superclass
            }
        }
        return null
    }
}
