@file:Suppress("UnsafeOptInUsageError")

package com.muses.player.core.media.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline

/** 按实际播放顺序返回媒体索引；关闭循环遍历，保证每个队列项只出现一次。 */
internal fun Timeline.playbackQueueIndices(shuffleEnabled: Boolean): List<Int> = buildList {
    var index = getFirstWindowIndex(shuffleEnabled)
    while (index != C.INDEX_UNSET && size < windowCount) {
        add(index)
        index = getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffleEnabled)
    }
}
