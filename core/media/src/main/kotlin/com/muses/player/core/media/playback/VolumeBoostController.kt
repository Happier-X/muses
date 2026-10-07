package com.muses.player.core.media.playback

import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.muses.player.core.model.PLAYBACK_VOLUME_COMPENSATION_DB
import kotlinx.coroutines.CoroutineScope

/**
 * 将音量增益挂到 ExoPlayer 实际输出的音频会话，并随会话变化重新绑定。
 *
 * 默认额外增益为零，不创建 [LoudnessEnhancer]，保持音源的原始幅度。
 * 非零增益的会话管理仍保留失败恢复能力，不提供用户可调设置。
 */
@UnstableApi
class VolumeBoostController(scope: CoroutineScope) {
    private val session = VolumeBoostSession(scope) { AndroidVolumeBoostEffect(it) }
    val status = session.status
    private var player: ExoPlayer? = null
    private val listener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            session.bind(audioSessionId)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) session.refresh()
        }
    }

    /** 未准备好音频输出时等待真实会话，避免效果器绑到无效或无关会话。 */
    fun attach(player: ExoPlayer) {
        release()
        this.player = player
        player.addListener(listener)
        session.bind(player.audioSessionId)
        session.apply(PLAYBACK_VOLUME_COMPENSATION_DB)
    }

    fun release() {
        player?.removeListener(listener)
        player = null
        session.release()
    }
}

private class AndroidVolumeBoostEffect(sessionId: Int) : VolumeBoostEffect {
    private val effect = LoudnessEnhancer(sessionId)
    override var targetGainMillibels: Int
        get() = effect.targetGain.toInt()
        set(value) { effect.setTargetGain(value) }
    override var enabled: Boolean
        get() = effect.enabled
        set(value) { effect.enabled = value }
    override fun release() = effect.release()
}
