package com.muses.player.feature.player.lyric

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlin.math.abs

/**
 * 自研单行卡拉OK渲染 — 手搓版，不依赖 vendored lyrics-ui
 * 逐词连续：对当前行按 syllable 粒度 lerp inactive→active，远行统一 inactive
 * 长词字符级：暂按整词 lerp，后续可拆字符（整词>6字符时字符级在 AnnotatedString 内逐字符 lerp 可迭代）
 */
@Composable
fun NativeKaraokeLine(
    line: ISyncedLine,
    isCurrent: Boolean,
    distance: Int,
    positionProvider: (() -> Int)?,
    translationEnabled: Boolean,
    fontSize: TextUnit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 距离衰减：当前 1.0/1.05，其余按距离递减 — 改为 spring 动画（替代瞬切）
    val targetAlpha = when (distance) {
        0 -> 1f
        1 -> 0.45f
        2 -> 0.28f
        else -> 0.18f
    }
    val targetScale = if (distance == 0) 1.05f else 0.92f
    val targetBlur = if (abs(distance) >= 2) 6.dp else 0.dp
    // 弹簧参数可调：stiffness 越大越硬，dampingRatio 越小越弹
    val lineAlpha by animateFloatAsState(targetValue = targetAlpha, animationSpec = spring(stiffness = 350f, dampingRatio = 0.82f), label = "lyric-alpha")
    val lineScale by animateFloatAsState(targetValue = targetScale, animationSpec = spring(stiffness = 380f, dampingRatio = 0.72f), label = "lyric-scale")
    val blurRadius by animateDpAsState(targetValue = targetBlur, animationSpec = spring(stiffness = 300f, dampingRatio = 0.85f), label = "lyric-blur")

    // 当前行逐帧位置（仅当前行订阅，减少重组）
    var currentPos by remember(line, isCurrent) { mutableIntStateOf(positionProvider?.invoke() ?: line.start) }
    if (isCurrent && positionProvider != null) {
        LaunchedEffect(line) {
            while (true) {
                withFrameMillis {
                    currentPos = positionProvider()
                }
            }
        }
    }

    val isBG = line is KaraokeLine.AccompanimentKaraokeLine
    val lineStart = line.start.toLong()
    // 提取 words 与翻译
    val words: List<WordInfo> = remember(line) {
        when (line) {
            is KaraokeLine -> line.syllables.map { s ->
                WordInfo(text = s.content, start = s.start.toLong(), end = s.end.toLong())
            }
            is SyncedLine -> listOf(WordInfo(text = line.content, start = line.start.toLong(), end = line.end.toLong()))
            else -> emptyList()
        }
    }
    val translation: String? = remember(line, translationEnabled) {
        if (!translationEnabled) null else when (line) {
            is KaraokeLine -> line.translation?.takeIf { it.isNotBlank() }
            is SyncedLine -> line.translation?.takeIf { it.isNotBlank() }
            else -> null
        }
    }
    val roman: String? = remember(line, translationEnabled) {
        if (!translationEnabled) null else (line as? KaraokeLine)?.phonetic?.takeIf { it.isNotBlank() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = lineScale
                scaleY = lineScale
                alpha = lineAlpha
            }
            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
            .clickable { onSeek(lineStart) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        // 主行文本
        val mainText = buildAnnotatedString {
            if (!isCurrent) {
                // 非当前行：统一 inactive
                val baseColor = if (isBG) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.35f)
                withStyle(
                    SpanStyle(
                        color = baseColor,
                        fontSize = fontSize,
                        fontWeight = if (isBG) FontWeight.Normal else FontWeight.Medium,
                        fontStyle = if (isBG) FontStyle.Italic else FontStyle.Normal,
                    )
                ) {
                    // 拼接 words，保留原始空格逻辑：word 之间加空格（若原文不含空格）
                    words.forEachIndexed { idx, w ->
                        append(w.text)
                        if (idx < words.lastIndex) append(" ")
                    }
                }
            } else {
                // 当前行：逐词 lerp
                words.forEachIndexed { idx, w ->
                    val fraction = when {
                        currentPos < w.start -> 0f
                        currentPos >= w.end -> 1f
                        else -> (currentPos - w.start).toFloat() / (w.end - w.start).coerceAtLeast(1).toFloat()
                    }.coerceIn(0f, 1f)

                    // 长词字符级：若 word 长度>6，则按字符拆分进一步细化（首版简易线性分配）
                    if (w.text.length > 6 && fraction in 0f..1f && w.text.length <= 20) {
                        val chars = w.text.toList()
                        chars.forEachIndexed { cIdx, ch ->
                            val charFraction = run {
                                val total = chars.size
                                val charProgress = fraction * total
                                (charProgress - cIdx).coerceIn(0f, 1f)
                            }
                            val color = lerpColor(
                                Color.White.copy(alpha = 0.35f),
                                Color.White,
                                charFraction
                            )
                            val weight = if (charFraction > 0.5f) FontWeight.ExtraBold else FontWeight.Medium
                            withStyle(SpanStyle(color = color, fontSize = fontSize, fontWeight = weight, fontStyle = if (isBG) FontStyle.Italic else FontStyle.Normal)) {
                                append(ch.toString())
                            }
                        }
                    } else {
                        val color = lerpColor(Color.White.copy(alpha = 0.35f), Color.White, fraction)
                        val weight = if (fraction > 0.5f) FontWeight.ExtraBold else FontWeight.Medium
                        // 对和声行保持 italic
                        withStyle(SpanStyle(color = color, fontSize = fontSize, fontWeight = weight, fontStyle = if (isBG) FontStyle.Italic else FontStyle.Normal)) {
                            append(w.text)
                        }
                    }
                    if (idx < words.lastIndex) {
                        // 空格不参与染色，保持 inactive 35%
                        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.35f), fontSize = fontSize)) {
                            append(" ")
                        }
                    }
                }
            }
        }

        val textAlign = if (isBG) TextAlign.End else TextAlign.Center
        Text(
            text = mainText,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
            style = LocalTextStyle.current.copy(lineHeight = (fontSize.value * 1.25f).sp),
        )

        // 翻译 / 音译（第二行，小字）
        if (translation != null) {
            Text(
                text = translation,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                maxLines = 1,
            )
        }
        if (roman != null) {
            Text(
                text = roman,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                maxLines = 1,
            )
        }
    }
}

private data class WordInfo(val text: String, val start: Long, val end: Long)

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f,
    )
}

internal fun computeCurrentIndexNative(lines: List<ISyncedLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var idx = -1
    for (i in lines.indices) {
        if (lines[i].start <= positionMs) idx = i else break
    }
    if (idx == -1) return 0
    val last = lines.last()
    if (positionMs > last.end) return lines.lastIndex
    return idx.coerceIn(0, lines.lastIndex)
}
