package com.muses.player.core.model.online

import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 会话级「在线曲目」登记表。
 *
 * ## 为什么需要它
 * 在线曲目**不入库**（避免污染 `songs.path`/`tagsVersion`/扫描器语义），
 * 但桌面播放链路是「队列只存 songId → 播放时查库拿 Song」：
 * ```
 * DesktopPlayerHook.play(songs) → JvmPlayerPort.enqueue(ids) → playSongId(id) → songLookup(id) → DB
 * ```
 * 在线曲目的 songId 在库里查不到，会导致「文件不存在」。
 *
 * 本表充当**播放期的临时缓存**：入队时登记，播放时作为查库的回退，
 * 使在线曲目无需落库即可被双端播放链路正常解析。
 *
 * ## 生命周期
 * - 登记：开始播放在线曲目时（[remember]）；
 * - 读取：播放器的 songLookup 回退链（[find]）；
 * - 清理：切换到非在线内容或退出时可 [clear]；不做自动过期（条目极小，仅元数据）。
 *
 * 线程安全：经 [Mutex] 串行化（commonMain 无 synchronized）。
 * 用 suspend 接口换取 KMP 可移植性——调用方（songLookup/play）本就是 suspend 上下文。
 */
object OnlineTrackSession {

    private val mutex = Mutex()
    private val songs = mutableMapOf<String, Song>()

    /** 登记一批在线曲目（仅登记 [SourceType.ONLINE] 的条目，其余忽略） */
    suspend fun remember(candidates: List<Song>) {
        val online = candidates.filter { it.sourceType == SourceType.ONLINE }
        if (online.isEmpty()) return
        mutex.withLock {
            online.forEach { songs[it.id] = it }
        }
    }

    /** 按 id 取会话内的在线曲目；未登记返回 null */
    suspend fun find(songId: String): Song? = mutex.withLock { songs[songId] }

    /** 批量取（供批量解析场景） */
    suspend fun findAll(ids: List<String>): Map<String, Song> = mutex.withLock {
        ids.mapNotNull { id -> songs[id]?.let { id to it } }.toMap()
    }

    /** 当前登记数量（调试/测试用） */
    suspend fun size(): Int = mutex.withLock { songs.size }

    /** 清空（切换音源/退出在线搜索时调用，避免陈旧条目长期占用） */
    suspend fun clear() = mutex.withLock { songs.clear() }
}
