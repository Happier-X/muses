package com.muses.player.core.scrape.writeback

import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.scrape.ports.TagPort
import java.io.File

/** 写入后回读核验，不能仅凭标签库返回成功就宣称文件已保存。 */
fun TagPort.writeAndVerify(file: File, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult {
    if ((!changes.coverRemoteUrl.isNullOrBlank() || !changes.coverUri.isNullOrBlank()) && coverBytes?.isNotEmpty() != true) {
        return FileWriteResult(false, "cover_failed", "封面数据不可用，文件未更新。")
    }
    val result = writeTags(file, changes, coverBytes)
    if (!result.ok) return result
    return verifyTags(file, changes, coverBytes)
}

fun TagPort.verifyTags(file: File, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult {
    val actual = readTags(file) ?: return FileWriteResult(false, "verify_failed", "无法回读音频标签，请重试。")
    fun matches(expected: String?, value: String?) = expected == null || expected.trim() == value.orEmpty().trim()
    val valid = matches(changes.title, actual.title) && matches(changes.artist, actual.artist) &&
        matches(changes.album, actual.album) && matches(changes.lyrics, actual.lyrics) &&
        (changes.coverUri != "" || actual.cover?.isNotEmpty() != true) &&
        (coverBytes?.isNotEmpty() != true || actual.cover.contentEquals(coverBytes))
    return if (valid) FileWriteResult(true)
    else FileWriteResult(false, "verify_failed", "音频标签回读与所选结果不一致，未标记为已保存。")
}
