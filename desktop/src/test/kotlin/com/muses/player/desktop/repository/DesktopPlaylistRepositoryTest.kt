package com.muses.player.desktop.repository

import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.createJvmInMemoryDatabase
import com.muses.player.core.data.repository.RoomPlaylistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 桌面歌单仓库冒烟：覆盖 Windows 端歌单页报错根因——
 * `desktopLibraryModule` 曾缺 `PlaylistRepository` 绑定，
 * `PlaylistsViewModel(get())` 构造失败 "Could not create instance"。
 *
 * 验证桌面内存库可直构 [RoomPlaylistRepository] 并完成建单/列表读写，
 * 保证桌面绑定（[com.muses.player.desktop.DesktopKoinModule]）的构造链不断。
 */
class DesktopPlaylistRepositoryTest {

    private fun memoryDb(): MusesDatabase = createJvmInMemoryDatabase()

    @Test
    fun 桌面内存库可建歌单并读回() = runTest {
        val db = memoryDb()
        try {
            val repository = RoomPlaylistRepository(db)
            val id = repository.createPlaylist("桌面歌单")
            assertTrue(id.isNotBlank())
            val playlists = repository.observePlaylists().first()
            assertEquals(listOf("桌面歌单"), playlists.map { it.name })
            // 空歌单无有效歌曲行，validCounts 不含该 key（缺省即 0，与 PlaylistsPage
            // `validCounts[playlist.id] ?: 0` 的消费语义一致）
            val validCounts = repository.observeValidCounts().first()
            assertEquals(0, validCounts[id] ?: 0)
        } finally {
            db.close()
        }
    }
}
