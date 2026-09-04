package com.muses.player.core.data.db

import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S1 桌面 Room 接线测试：指定路径文件库可读写 + 内存库可用。
 * 用隔离 tmp 目录建库，不污染真实 %APPDATA%。
 */
class JvmDatabaseTest {

    @Test
    fun 指定路径文件库读写可用() = runTest {
        val tmp = Files.createTempDirectory("muses-jvm-db").toFile()
        try {
            val dbFile = File(tmp, "muses.db").absolutePath
            val db = createJvmDatabaseAt(dbFile)
            try {
                db.songDao().upsert(
                    SongEntity(
                        id = "s1",
                        sourceId = "src",
                        sourceType = "LOCAL",
                        path = "/a.mp3",
                        title = "t",
                        tagsVersion = 1,
                    ),
                )
                assertEquals(1, db.songDao().count())
                assertTrue(File(dbFile).exists(), "DB 文件应落盘")
            } finally {
                db.close()
            }
            // 重开验证持久化
            val reopened = createJvmDatabaseAt(dbFile)
            try {
                assertEquals(1, reopened.songDao().count())
                assertEquals("t", reopened.songDao().getById("s1")?.title)
            } finally {
                reopened.close()
            }
        } finally {
            tmp.deleteRecursively()
        }
    }

    @Test
    fun 内存库读写可用() = runTest {
        val db = createJvmInMemoryDatabase()
        try {
            db.songDao().upsert(
                SongEntity(
                    id = "m1",
                    sourceId = "src",
                    sourceType = "LOCAL",
                    path = "/b.mp3",
                    title = "mem",
                    tagsVersion = 0,
                ),
            )
            assertEquals(listOf("m1"), db.songDao().getUntaggedSongIds())
        } finally {
            db.close()
        }
    }
}
