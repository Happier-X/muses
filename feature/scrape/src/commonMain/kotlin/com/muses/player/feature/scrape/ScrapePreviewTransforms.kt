package com.muses.player.feature.scrape

/**
 * 预览行纯变换（ScrapeViewModel 瘦身：勾选/字段/编辑的全量拷贝逻辑下沉为可单测纯函数，
 * ViewModel 只负责取 Preview 态与回写，行为与原内联实现一致）。
 */

/** 预览行勾选切换（整首） */
internal fun List<PreviewCandidate>.toggleChecked(songId: String): List<PreviewCandidate> =
    map { if (it.songId == songId) it.copy(checked = !it.checked) else it }

/** 全选 / 全不选（整首） */
internal fun List<PreviewCandidate>.setAllChecked(checked: Boolean): List<PreviewCandidate> =
    map { it.copy(checked = checked) }

/** 切换单首歌曲的单个字段勾选 */
internal fun List<PreviewCandidate>.toggleField(songId: String, field: String): List<PreviewCandidate> =
    map {
        if (it.songId == songId) {
            val newChecked = it.checkedFields.toMutableSet()
            if (field in newChecked) newChecked.remove(field) else newChecked.add(field)
            it.copy(checkedFields = newChecked)
        } else it
    }

/** 批量全选/全不选某字段（跨所有歌曲） */
internal fun List<PreviewCandidate>.setAllFields(field: String, checked: Boolean): List<PreviewCandidate> =
    map {
        val newChecked = it.checkedFields.toMutableSet()
        if (checked) newChecked.add(field) else newChecked.remove(field)
        it.copy(checkedFields = newChecked)
    }

/** 更新预览行编辑值（空串已在调用方转 null 表示回退匹配值） */
internal fun List<PreviewCandidate>.updateItem(
    songId: String,
    title: String?,
    artist: String?,
    album: String?,
    lyrics: String? = null,
): List<PreviewCandidate> =
    map {
        if (it.songId == songId) {
            it.copy(editTitle = title, editArtist = artist, editAlbum = album, editLyrics = lyrics)
        } else it
    }
