package com.muses.player.core.scrape.writeback

import java.nio.file.Files
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CoverSnapshotStoreTest {
    @Test fun `字节快照重建后可读且校验损坏缺失路径与容量`() = runTest {
        val directory = Files.createTempDirectory("muses-cover-history-").toFile()
        try {
            val store = CoverSnapshotStore(directory, 4)
            val bytes = byteArrayOf(1, 2, 3, 4)
            val id = store.save(bytes)
            assertEquals(id, store.save(bytes))
            assertContentEquals(bytes, CoverSnapshotStore(directory).read(id))
            assertFailsWith<IllegalStateException> { store.save(byteArrayOf(5)) }
            assertFailsWith<IllegalArgumentException> { store.read("../invalid") }
            File(directory, id).writeBytes(byteArrayOf(4, 3, 2, 1))
            assertFailsWith<IllegalStateException> { store.read(id) }
            File(directory, id).delete()
            assertFailsWith<IllegalStateException> { store.read(id) }
        } finally { directory.deleteRecursively() }
    }
}
