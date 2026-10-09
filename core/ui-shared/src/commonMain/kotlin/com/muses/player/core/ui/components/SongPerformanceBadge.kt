package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.components.MarqueeText as Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 目录平台确认的原唱标记。 */
@Composable
fun SongPerformanceBadge(label: String?) {
    if (label == null) return
    val scheme = MiuixTheme.colorScheme
    Text(label, fontSize = 10.sp, color = scheme.primary,
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .background(scheme.primary.copy(alpha = 0.1f)).padding(horizontal = 5.dp, vertical = 2.dp))
}

/** 版本小标紧跟歌名，长标题先省略，为标签保留空间。 */
@Composable
fun SongTitleWithBadge(
    title: String,
    label: String?,
    style: TextStyle = MiuixTheme.textStyles.body2,
    color: Color = MiuixTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f, fill = false), style = style,
            color = color, fontWeight = fontWeight, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (label != null) {
            Spacer(Modifier.width(6.dp))
            SongPerformanceBadge(label)
        }
    }
}

/** 音质小标放在艺术家与专辑信息前，副标题变长时只省略文字。 */
@Composable
fun SongSubtitleWithQuality(
    subtitle: String,
    label: String?,
    style: TextStyle = MiuixTheme.textStyles.footnote1,
    color: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
) {
    if (subtitle.isBlank() && label == null) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (label != null) {
            SongQualityBadge(label)
            if (subtitle.isNotBlank()) Spacer(Modifier.width(5.dp))
        }
        if (subtitle.isNotBlank()) {
            Text(subtitle, modifier = Modifier.weight(1f), style = style.copy(
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ), color = color,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 音质与格式标签共用紧凑字标；格式标签只表示格式，不据此推断音质。 */
@Composable
private fun SongQualityBadge(label: String) {
    val dark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    // 颜色只按确认的音质分档，格式相同的文件可能具有不同音质。
    val (lightForeground, lightBackground) = when (label) {
        "HQ" -> Color(0xFF9C6538) to Color(0xFFF3E9E0)
        "SQ" -> Color(0xFF2D7AA8) to Color(0xFFE0F2F3)
        "Hi-Res" -> Color.Black to Color(0xFFFFE793)
        else -> Color(0xFF62666C) to Color(0xFFE7E9EC)
    }
    val foreground = if (dark) {
        if (label == "Hi-Res") Color(0xFFF9F9F9) else lightBackground
    } else lightForeground
    val background = if (dark) {
        if (label == "HQ" || label == "SQ") lightForeground else Color(0xFF3C4047)
    } else lightBackground
    val fill = if (label == "Hi-Res") {
        Brush.linearGradient(if (dark) listOf(Color(0xFFE7BA1C), Color(0xFFE4AC0F))
            else listOf(Color(0xFFFFE793), Color(0xFFF6CE5B)))
    } else Brush.linearGradient(listOf(background, background))
    Box(
        // 副标题的可见字形略低于行框中心，字标随之下移以保持视觉居中。
        modifier = Modifier.offset(y = 1.dp).clip(RoundedCornerShape(2.dp))
            .background(fill)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (label == "HQ" || label == "SQ") {
            Canvas(Modifier.size(11.dp, 7.dp).semantics { contentDescription = label }) {
                // 以 16 × 10 的字标坐标绘制，避免不同系统字体改变字形。
                val sx = size.width / 16f
                val sy = size.height / 10f
                val letters = Path().apply {
                    fun move(x: Float, y: Float) = moveTo(x * sx, y * sy)
                    fun line(x: Float, y: Float) = lineTo(x * sx, y * sy)
                    if (label == "HQ") {
                        move(1f, 0.8f); line(1f, 9.2f)
                        move(6f, 0.8f); line(6f, 9.2f)
                        move(1f, 5f); line(6f, 5f)
                    } else {
                        move(6f, 1f); line(1.8f, 1f); line(1f, 1.8f)
                        line(1f, 4.2f); line(1.8f, 5f); line(5.2f, 5f)
                        line(6f, 5.8f); line(6f, 8.2f); line(5.2f, 9f); line(1f, 9f)
                    }
                    move(10f, 1f); line(13.8f, 1f); line(14.5f, 1.7f)
                    line(14.5f, 8.3f); line(13.8f, 9f); line(10f, 9f)
                    line(9.3f, 8.3f); line(9.3f, 1.7f); close()
                    move(12.5f, 7f); line(15.5f, 9.5f)
                }
                drawPath(letters, foreground, style = Stroke(width = 1.4f * sx))
            }
        } else if (label == "FLAC" || label == "MP3") {
            FormatWordmark(label, foreground)
        } else {
            Text(if (label == "Hi-Res") "HR" else label,
                modifier = Modifier.semantics { contentDescription = label },
                style = TextStyle(fontSize = 9.sp, lineHeight = 10.sp,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                    fontWeight = if (label == "Hi-Res") FontWeight.Medium else FontWeight.Bold),
                color = foreground, maxLines = 1)
        }
    }
}

/** 与 HQ / SQ 一致的几何字形，避免普通字体的上下留白影响标签对齐。 */
@Composable
private fun FormatWordmark(label: String, color: Color) {
    val width = label.length * 7f - 2f
    Canvas(Modifier.size((width * 0.7f).dp, 7.dp).semantics { contentDescription = label }) {
        val sx = size.width / width
        val sy = size.height / 10f
        val letters = Path().apply {
            label.forEachIndexed { index, character ->
                val origin = index * 7f
                fun move(x: Float, y: Float) = moveTo((origin + x) * sx, y * sy)
                fun line(x: Float, y: Float) = lineTo((origin + x) * sx, y * sy)
                when (character) {
                    'F' -> {
                        move(5f, 1f); line(0.7f, 1f); line(0.7f, 9f)
                        move(0.7f, 5f); line(4.5f, 5f)
                    }
                    'L' -> { move(0.7f, 1f); line(0.7f, 9f); line(5f, 9f) }
                    'A' -> {
                        move(0.7f, 9f); line(0.7f, 2f); line(1.7f, 1f)
                        line(4f, 1f); line(5f, 2f); line(5f, 9f)
                        move(0.7f, 5.5f); line(5f, 5.5f)
                    }
                    'C' -> {
                        move(5f, 1f); line(1.7f, 1f); line(0.7f, 2f)
                        line(0.7f, 8f); line(1.7f, 9f); line(5f, 9f)
                    }
                    'M' -> {
                        move(0.7f, 9f); line(0.7f, 1f); line(2.85f, 4.5f)
                        line(5f, 1f); line(5f, 9f)
                    }
                    'P' -> {
                        move(0.7f, 9f); line(0.7f, 1f); line(4f, 1f)
                        line(5f, 2f); line(5f, 4f); line(4f, 5f); line(0.7f, 5f)
                    }
                    '3' -> {
                        move(0.7f, 1f); line(4f, 1f); line(5f, 2f)
                        line(5f, 8f); line(4f, 9f); line(0.7f, 9f)
                        move(2f, 5f); line(5f, 5f)
                    }
                }
            }
        }
        drawPath(letters, color, style = Stroke(width = 1.4f * sx))
    }
}
