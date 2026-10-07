package com.muses.player.feature.scrape

/**
 * 预览行纯变换（ScrapeViewModel 瘦身：勾选/字段/编辑的全量拷贝逻辑下沉为可单测纯函数，
 * ViewModel 只负责取预览态与回写；选择与可用候选字段同步）。
 */

/** 预览行勾选切换（整首） */
internal fun List<PreviewCandidate>.toggleChecked(songId: String): List<PreviewCandidate> =
    map {
        if (it.songId != songId) it else {
            val fields = if (it.checkedFields.isEmpty()) it.availableFields() else emptySet()
            it.copy(checked = fields.isNotEmpty(), checkedFields = fields)
        }
    }

/** 全选 / 全不选（整首） */
internal fun List<PreviewCandidate>.setAllChecked(checked: Boolean): List<PreviewCandidate> =
    map {
        val fields = if (checked) it.availableFields() else emptySet()
        it.copy(checked = fields.isNotEmpty(), checkedFields = fields)
    }

/** 切换单首歌曲的单个字段勾选 */
internal fun List<PreviewCandidate>.toggleField(songId: String, field: String): List<PreviewCandidate> =
    map {
        if (it.songId == songId) {
            if (field !in it.availableFields()) return@map it
            val newChecked = it.checkedFields.toMutableSet()
            if (field in newChecked) newChecked.remove(field) else newChecked.add(field)
            it.copy(checked = newChecked.isNotEmpty(), checkedFields = newChecked)
        } else it
    }

/** 批量全选/全不选某字段（跨所有歌曲） */
internal fun List<PreviewCandidate>.setAllFields(field: String, checked: Boolean): List<PreviewCandidate> =
    map {
        val newChecked = it.checkedFields.toMutableSet()
        if (checked && field in it.availableFields()) newChecked.add(field) else newChecked.remove(field)
        it.copy(checked = newChecked.isNotEmpty(), checkedFields = newChecked)
    }

/** 一次选择所有可用变更中的指定字段；调用方明确点击后才选择。 */
internal fun List<PreviewCandidate>.selectFields(fields: Set<String>): List<PreviewCandidate> =
    map {
        val selected = it.availableFields().intersect(fields)
        it.copy(checked = selected.isNotEmpty(), checkedFields = selected)
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
            val updated = it.copy(editTitle = title, editArtist = artist, editAlbum = album, editLyrics = lyrics)
            val selected = it.checkedFields.intersect(updated.availableFields())
            updated.copy(checked = selected.isNotEmpty(), checkedFields = selected)
        } else it
    }
