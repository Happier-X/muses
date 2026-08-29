/*
 * Vendored third-party source - muses
 *
 * Origin : Accompanist lyrics-ui 1.0.19 (AMLL official Compose implementation)
 * Repo   : https://github.com/6xingyv/accompanist-lyrics-ui
 * License: Apache License 2.0  (https://www.apache.org/licenses/LICENSE-2.0)
 * Author : The Accompanist / MochaRealm Authors
 *
 * The package name is intentionally kept as `com.mocharealm.accompanist.*` so the
 * code can be diffed against upstream. Local modifications:
 *  - expect/actual (Kotlin Multiplatform) collapsed into plain Kotlin for this
 *    single-target Android module: Char.isCjk / isArabic / isDevanagari now live
 *    directly in utils/String.kt
 *  - com.mocharealm.gaze.capsule.ContinuousRoundedRectangle replaced with
 *    androidx.compose.foundation.shape.RoundedCornerShape, dropping the
 *    gaze-capsule dependency (only used to clip a line item)
 */

package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

@Composable
fun SyncedLineText(
    line: SyncedLine,
    isLineRtl: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier,
    showTranslation: Boolean = true
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = if (isLineRtl) Alignment.End else Alignment.Start
    ) {
        Text(
            text = line.content,
            style = textStyle,
            color = textColor,
            textAlign = if (isLineRtl) TextAlign.End else TextAlign.Start
        )
        if (showTranslation) {
            line.translation?.let {
                Text(
                    text = it,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = if (isLineRtl) TextAlign.End else TextAlign.Start
                )
            }
        }
    }
}

