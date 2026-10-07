package com.muses.player.core.model.online

/** 按歌曲信息寻找其他平台的同曲版本；不在浏览阶段解析播放地址。 */
fun interface OnlineTrackCandidateProvider {
    suspend fun candidates(ref: OnlineTrackRef, platforms: List<String>): List<OnlineTrackRef>
}
