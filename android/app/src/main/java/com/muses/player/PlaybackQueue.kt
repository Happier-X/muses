package com.muses.player

/**
 * 简化版滑动窗口播放队列，移植自 SPlayer，Kotlin 重写。
 * JS 端推送窗口，原生自治切歌，WINDOW_REFILL_THRESHOLD 触发补窗。
 */
class PlaybackQueue {

    private var windowTracks: List<Track> = emptyList()
    private var windowCurrentIndex: Int = -1
    private var repeatMode: RepeatMode = RepeatMode.ALL
    private var hasPreviousOutsideWindow: Boolean = false
    private var hasNextOutsideWindow: Boolean = false

    enum class RepeatMode {
        OFF, ALL, ONE;

        companion object {
            fun fromString(value: String?): RepeatMode {
                return when (value) {
                    "all" -> ALL
                    "one" -> ONE
                    "off" -> OFF
                    else -> ALL
                }
            }
        }
    }

    class Track {
        var songId: String = ""
        var durationMs: Long = 0L
        var title: String = ""
        var artist: String = ""
        var album: String = ""
        var coverUrl: String = ""
        var url: String? = null
        var authHeader: String? = null
        var playListIndex: Int = -1

        fun copy(): Track {
            return Track().apply {
                songId = this@Track.songId
                durationMs = this@Track.durationMs
                title = this@Track.title
                artist = this@Track.artist
                album = this@Track.album
                coverUrl = this@Track.coverUrl
                url = this@Track.url
                authHeader = this@Track.authHeader
                playListIndex = this@Track.playListIndex
            }
        }

        fun playable(): Boolean = !url.isNullOrEmpty()
    }

    @Synchronized
    fun replace(
        tracks: List<Track>?,
        currentIndex: Int,
        mode: RepeatMode?,
        prevOutside: Boolean,
        nextOutside: Boolean,
    ) {
        if (tracks.isNullOrEmpty()) {
            windowTracks = emptyList()
            windowCurrentIndex = -1
        } else {
            val copy = tracks.mapNotNull { it?.copy() }
            windowTracks = copy
            windowCurrentIndex = when {
                currentIndex < 0 -> -1
                currentIndex >= copy.size -> copy.size - 1
                else -> currentIndex
            }
        }
        repeatMode = mode ?: RepeatMode.ALL
        hasPreviousOutsideWindow = prevOutside
        hasNextOutsideWindow = nextOutside
    }

    @Synchronized
    fun current(): Track? {
        if (windowCurrentIndex < 0 || windowCurrentIndex >= windowTracks.size) return null
        return windowTracks[windowCurrentIndex]
    }

    @Synchronized
    fun playableTracksAhead(): Int {
        if (windowTracks.isEmpty() || windowCurrentIndex < 0) return 0
        return windowTracks.size - windowCurrentIndex - 1
    }

    @Synchronized
    fun hasPreviousOutsideWindow(): Boolean = hasPreviousOutsideWindow

    @Synchronized
    fun hasNextOutsideWindow(): Boolean = hasNextOutsideWindow

    @Synchronized
    fun advanceRaw(respectRepeatOne: Boolean): Track? {
        if (windowTracks.isEmpty()) return null
        if (respectRepeatOne && repeatMode == RepeatMode.ONE) {
            return current()
        }
        val probe = windowCurrentIndex + 1
        if (probe < windowTracks.size) {
            windowCurrentIndex = probe
            return windowTracks[probe]
        }
        // 窗口完整覆盖全局时 ALL 模式可在窗口内 wrap
        if (repeatMode == RepeatMode.ALL
            && !hasPreviousOutsideWindow
            && !hasNextOutsideWindow
            && windowTracks.isNotEmpty()
        ) {
            windowCurrentIndex = 0
            return windowTracks[0]
        }
        return null
    }

    @Synchronized
    fun backRaw(): Track? {
        if (windowTracks.isEmpty()) return null
        val probe = windowCurrentIndex - 1
        if (probe >= 0) {
            windowCurrentIndex = probe
            return windowTracks[probe]
        }
        return null
    }

    @Synchronized
    fun isEmpty(): Boolean = windowTracks.isEmpty()

    @Synchronized
    fun getRepeatMode(): RepeatMode = repeatMode

    @Synchronized
    fun findUrlBySongId(songId: String): String? {
        return windowTracks.firstOrNull { it.songId == songId }?.url
    }

    @Synchronized
    fun updateTrackUrl(songId: String, url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val track = windowTracks.firstOrNull { it.songId == songId } ?: return false
        track.url = url
        return true
    }
}
