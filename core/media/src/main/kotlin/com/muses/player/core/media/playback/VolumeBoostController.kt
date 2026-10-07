package com.muses.player.core.media.playback

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.muses.player.core.model.MAX_VOLUME_BOOST_DB
import com.muses.player.core.model.volumeBoostMillibels

/**
 * 播放音量增益：给 ExoPlayer 绑定自建 audio session，再挂 [LoudnessEnhancer]。
 *
 * 为什么要增益：Muses 原样直出（player volume 恒 1.0），主流音乐 App 普遍带响度增强，
 * 同一首歌在系统音量相同时会显得偏小。
 *
 * [LoudnessEnhancer] 是系统音效，自带限幅（不会硬削波）；设备不提供该效果时静默降级
 * （[available] 为 false，按原样播放，不崩溃）。
 */
@UnstableApi
class VolumeBoostController(private val context: Context) {

    private var enhancer: LoudnessEnhancer? = null

    /** 已下发的档位（dB），-1 = 尚未下发。 */
    private var appliedDb = -1

    /** 本机是否成功挂上效果器，且最近一次设置已成功下发。 */
    val available: Boolean
        get() = enhancer != null && appliedDb >= 0

    /** 最近一次初始化或下发失败的原因，供播放日志定位设备兼容性问题。 */
    var lastError: Throwable? = null
        private set

    /** 绑定 player 与效果器；重复调用先释放旧效果器。 */
    fun attach(player: ExoPlayer) {
        release()
        runCatching {
            val audioManager = checkNotNull(context.getSystemService(AudioManager::class.java))
            val sessionId = audioManager.generateAudioSessionId()
            // 无效会话不能挂效果器，否则可能与播放器实际输出的会话不一致。
            check(sessionId > 0) { "无法创建播放音频会话：$sessionId" }
            player.setAudioSessionId(sessionId)
            enhancer = LoudnessEnhancer(sessionId)
        }.onFailure { lastError = it }
    }

    /** 下发增益档位（dB，0 = 关闭）；同档位重复调用不重复设置。 */
    fun apply(db: Int) {
        val targetDb = db.coerceIn(0, MAX_VOLUME_BOOST_DB)
        if (targetDb == appliedDb) return
        val effect = enhancer ?: return
        runCatching {
            effect.setTargetGain(volumeBoostMillibels(targetDb))
            effect.enabled = targetDb > 0
            check(effect.enabled == (targetDb > 0)) { "音量增益启用状态与设置不一致" }
        }.onSuccess {
            appliedDb = targetDb
            lastError = null
        }.onFailure {
            // 失败不能记为已应用，允许相同档位再次下发。
            appliedDb = -1
            lastError = it
        }
    }

    fun release() {
        runCatching { enhancer?.release() }
        enhancer = null
        appliedDb = -1
        lastError = null
    }
}
