package com.muses.player.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.model.DEFAULT_VOLUME_BOOST_DB
import com.muses.player.core.model.VOLUME_BOOST_STEPS_DB
import com.muses.player.core.ui.components.MusesActionItem
import com.muses.player.core.ui.components.MusesActionsSheet
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.preference.ArrowPreference

/**
 * 播放音量增益：Muses 走 ExoPlayer 原样直出，主流音乐 App 普遍带响度增强，
 * 同一首歌对比会显得偏小，这里用 LoudnessEnhancer 补一层整体增益。
 */
@Composable
actual fun VolumeBoostPreference() {
    val settings = koinInject<SettingsRepository>()
    val db by settings.volumeBoostDb.collectAsState(DEFAULT_VOLUME_BOOST_DB)
    val scope = rememberCoroutineScope()
    var sheetOpen by remember { mutableStateOf(false) }

    ArrowPreference(
        title = "音量增益",
        summary = volumeBoostLabel(db),
        onClick = { sheetOpen = true },
    )
    MusesActionsSheet(
        opened = sheetOpen,
        onDismiss = { sheetOpen = false },
        label = "音量增益",
        items = VOLUME_BOOST_STEPS_DB.map { step ->
            MusesActionItem(volumeBoostLabel(step)) {
                scope.launch { settings.setVolumeBoostDb(step) }
                sheetOpen = false
            }
        },
    )
}

/** 档位文案：0 = 关闭，其余显示 +n dB。 */
private fun volumeBoostLabel(db: Int): String = if (db <= 0) "关闭" else "+$db dB"
