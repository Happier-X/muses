package com.muses.player.feature.player.lyric

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.muses.player.core.lyrics.model.LyricLine
import com.muses.player.core.lyrics.model.LyricsDocument

data class PlaybackUiState(
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val artworkUrl: String? = null,
    val seekTo: (Long) -> Unit = {},
)

enum class LyricFontWeight(val composeWeight: FontWeight) {
    Light(FontWeight.Light),
    Regular(FontWeight.Normal),
    Medium(FontWeight.Medium),
    SemiBold(FontWeight.SemiBold),
    Bold(FontWeight.Bold),
    Black(FontWeight.Black),
}

object SettingsRuntime {
    const val automaticLyricSelectionEnabled: Boolean = true
    const val hapticFeedbackEnabled: Boolean = true
    const val lyricAdvanceAppliesToWordByWord: Boolean = true
    const val lyricAdvanceMs: Int = 0
    const val lyricAutoFollowEnabled: Boolean = true
    const val lyricBlurStrength: Float = 1f
    const val lyricCascadeBounce: Float = 0.16f
    const val lyricCascadeBounceEnabled: Boolean = true
    const val lyricCascadeBounceGradient: Float = 0.40f
    const val lyricCascadeCatchUpRatio: Float = 0.84f
    const val lyricCascadeChaseSpeedGradient: Float = 0.60f
    const val lyricCascadeDelayIncreaseMs: Float = 2.2f
    const val lyricCascadeDelayMs: Float = 17f
    const val lyricCascadeDurationMs: Float = 740f
    const val lyricCascadeFollowingDelayMs: Float = 27f
    const val lyricDimAmount: Float = 1f
    const val lyricDistanceBlurScale: Float = 1.05f
    const val lyricFocusColorLeadMs: Long = 80L
    const val lyricFocusPosition: Float = 0.5f
    const val lyricFocusScale: Float = 1.05f
    const val lyricFollowDelayMs: Int = 3000
    val lyricFontWeight: LyricFontWeight = LyricFontWeight.SemiBold
    const val lyricGlowEnabled: Boolean = true
    const val lyricGlowLongTonesOnly: Boolean = false
    const val lyricGlowStrength: Float = 1f
    const val lyricHiddenInterfaceBlurScale: Float = 1f
    const val lyricHighlightGradientReduction: Float = 0.65f
    const val lyricHighlightGradientWidth: Float = 0.7f
    const val lyricInactiveOpacity: Float = 0.3f
    const val lyricInterludeCountdownEnabled: Boolean = true
    const val lyricPseudoTimingEnabled: Boolean = false
    const val lyricReduceMotion: Boolean = false
    const val lyricRefreshRate: Int = 60
    const val lyricRomanizationFontScale: Float = 0.65f
    const val lyricRomanizationOpacity: Float = 0.9f
    const val lyricScaleBounce: Float = 0.30f
    const val lyricScaleBounceDurationMs: Int = 580
    const val lyricScaleBounceEnabled: Boolean = true
    const val lyricScrollHideThresholdDp: Float = 24f
    const val lyricSnapThresholdMs: Float = 150f
    const val lyricSpacingScale: Float = 1f
    const val lyricTapSeekEnabled: Boolean = true
    const val lyricTranslationFontScale: Float = 0.65f
    const val lyricTranslationOpacity: Float = 0.9f
    const val lyricWordBounceEnabled: Boolean = true
    const val lyricWordByWordEnabled: Boolean = true
    const val showLyricRomanization: Boolean = true
    const val showLyricTranslation: Boolean = true
    const val skylineEnabled: Boolean = false
    const val skylineAmbientBlur: Float = 2f
    const val skylineAmbientDrift: Float = 1f
    const val skylineAmbientFontSize: Float = 22f
    const val skylineAmbientLines: Int = 5
    const val skylineAmbientMaximumCharacters: Int = 12
    const val skylineAmbientMaximumTilt: Float = 5f
    const val skylineAmbientMaximumVisibleTexts: Int = 10
    const val skylineAmbientOpacity: Float = 1f
    const val skylineCurrentFontSize: Float = 28f
    const val skylineCurrentMaximumScale: Float = 1.06f
    const val skylineCurrentSpacing: Float = 12f
    const val skylineCurrentWidth: Float = 1f
    const val skylineNextFontSize: Float = 18f
    const val skylineNextOpacity: Float = 0.5f
    const val skylineShowSongInfo: Boolean = true
    const val textPVAnimationSpeed: Float = 1f
    const val textPVMotionIntensity: Float = 1f
    const val lyricFontScale: Float = 1f
    val lyricsStyle: LyricsStyle = LyricsStyle.AppleMusic
    val lyricRenderingQuality: LyricsRenderingQuality = LyricsRenderingQuality.High
    val lyricRomanizationDisplayMode: LyricAnnotationDisplayMode = LyricAnnotationDisplayMode.AllLines
    val lyricLiftMode: LyricsGroupingMode = LyricsGroupingMode.Word
    val lyricLongToneDetectionMode: LyricsGroupingMode = LyricsGroupingMode.Word
    const val lyricLongToneStrength: Float = 1f
    const val lyricLongToneThresholdMs: Int = 950
    const val lyricLongPressShareEnabled: Boolean = true
    val textPVStyle: TextPVStyle = TextPVStyle.BlueBold
}

object AppVisibility {
    const val isForeground: Boolean = true
}

enum class LyricsStyle { AppleMusic, Eva, TextPV }
enum class LyricsRenderingQuality { Low, Balanced, High }
enum class LyricAnnotationDisplayMode { AllLines, CurrentLine }
enum class LyricsGroupingMode { Word, Line }

enum class TextPVStyle {
    BlueBold, KineticSplit, BluePlane, CyberGrunge, Geometric, RainCity, CyberpunkHUD,
    EmotionCinema, HystericNight, SpiderWeb, StaggeredText, CalmVillain, GirlyClouds,
    SweetPink, FlyMeToTheMoon, KawaiiPixel, CrimeScene, Haruhikage, Dynamic, Minimal, Cyber
}

val LocalFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }
val LanTingProFontFamily: FontFamily = FontFamily.Default
object R
const val NowPlayingControlsHeight: Int = 84

object ProviderLyricsLoader {
    suspend fun load(context: Context, state: PlaybackUiState): LyricsDocument? = null
}

fun normalizeLyricMatchText(text: String?): String = text?.trim()?.lowercase().orEmpty()
suspend fun shareLyricImage(context: Context, state: PlaybackUiState, lines: List<LyricLine>) {}
fun Modifier.meloXLiquidButton(shape: RoundedCornerShape, enabled: Boolean, surfaceColor: Color): Modifier = this
@Composable fun Artwork(url: String?, modifier: Modifier = Modifier) { AsyncImage(model = url, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop) }
