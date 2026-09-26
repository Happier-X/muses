package com.muses.player.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muses.player.core.data.repository.DailyPlayStat
import com.muses.player.core.data.repository.PlayStats
import com.muses.player.core.data.repository.PlayStatsRepository
import com.muses.player.core.data.repository.SongPlayStat
import com.muses.player.core.ui.components.MusesCover
import com.muses.player.core.ui.components.MusesCoverRadius
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.SettingsBlockTitle
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import java.time.LocalDate
import java.time.YearMonth
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 高频歌曲展示条数（需求：按次数排名取前十） */
private const val TOP_SONG_LIMIT = 10

/** 热力图格子圆角（squircle，与 miuix 卡片同族但更小，避免小格看起来像圆点） */
private val HeatmapCorner = 6.dp

/** 热力图分档阈值：0 / <15 分钟 / <30 分钟 / <60 分钟 / ≥1 小时 */
private val HeatLevelThresholds = listOf(15 * 60_000L, 30 * 60_000L, 60 * 60_000L)

/** 热力图周表头（周一为一周起点） */
private val WeekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 统计页：听歌统计。
 *
 * 结构（自上而下）：可切换月份 → 该月热力图 → 本月概览（听歌天数/时长/次数）
 * → 累计数据（时长/次数）→ 高频歌曲（按累计播放次数排前十）。
 *
 * 数据全部取自 [PlayStatsRepository]：播放次数在歌曲起播时登记，听歌时长由
 * `PlayStatsSessionTracker` 按节拍结算落盘（暂停不计），因此本页数据会随播放自然增长。
 */
@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    val repository = koinInject<PlayStatsRepository>()
    val stats by remember(repository) { repository.observe() }
        .collectAsState(initial = PlayStats.Empty)
    val today = remember { LocalDate.now() }
    val currentMonth = remember(today) { YearMonth.from(today) }
    var month by remember { mutableStateOf(currentMonth) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MiuixTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { MusesTopBar(title = "统计", onBack = onBack) },
    ) { padding ->
        if (stats.totalPlayCount <= 0) {
            MusesEmpty(
                title = "还没有听歌数据",
                description = "播放歌曲后，这里会统计你的听歌天数、时长与高频歌曲",
                icon = TablerIcons.MusicNote,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            // 月份左边界：最早有记录的那个月（明细最多保留约三年）
            val earliestMonth = remember(stats.earliestMonth) {
                stats.earliestMonth?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: currentMonth
            }
            val monthly = remember(stats, month) { stats.month(month.toString()) }
            val monthDays = remember(stats, month) { stats.monthDays(month.toString()) }
            val topSongs = remember(stats) { stats.topSongs(TOP_SONG_LIMIT) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(top = 8.dp),
            ) {
                MonthSwitcherCard(
                    month = month,
                    canGoPrevious = month > earliestMonth,
                    canGoNext = month < currentMonth,
                    onPrevious = { month = month.minusMonths(1) },
                    onNext = { month = month.plusMonths(1) },
                )
                // 月份切换与热力图是两张紧邻的卡片，手动留出与 SmallTitle 分组相当的空隙
                Spacer(Modifier.height(12.dp))
                HeatmapCard(month = month, days = monthDays)

                SettingsBlockTitle("本月概览")
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    StatisticRow("听歌天数", "${monthly.listeningDays} 天")
                    StatisticRow("听歌时长", formatListenDuration(monthly.listenMs))
                    StatisticRow("播放次数", "${monthly.playCount} 次", divider = false)
                }

                SettingsBlockTitle("累计数据")
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    StatisticRow("听歌时长", formatListenDuration(stats.totalListenMs))
                    StatisticRow("播放次数", "${stats.totalPlayCount} 次", divider = false)
                }

                SettingsBlockTitle("高频歌曲")
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    if (topSongs.isEmpty()) {
                        Text(
                            text = "还没有播放记录",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    } else {
                        topSongs.forEachIndexed { index, song ->
                            if (index > 0) HorizontalDivider()
                            TopSongRow(rank = index + 1, song = song)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp + LocalBottomChromePadding.current))
            }
        }
    }
}

/** 月份切换：左右箭头 + 居中年月（到边界置灰且不可点，避免切进空月份） */
@Composable
private fun MonthSwitcherCard(
    month: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val disabledTint = scheme.onBackgroundVariant.copy(alpha = 0.3f)
    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MusesIconButton(
                onClick = onPrevious,
                imageVector = TablerIcons.ChevronLeft,
                contentDescription = "上个月",
                enabled = canGoPrevious,
                tint = if (canGoPrevious) scheme.onBackground else disabledTint,
            )
            Text(
                text = month.year.toString() + " 年 " + month.monthValue.toString() + " 月",
                style = MiuixTheme.textStyles.main,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            MusesIconButton(
                onClick = onNext,
                imageVector = TablerIcons.ChevronRight,
                contentDescription = "下个月",
                enabled = canGoNext,
                tint = if (canGoNext) scheme.onBackground else disabledTint,
            )
        }
    }
}

/**
 * 月热力图：当月日历网格（周一开头），格子深浅 = 当日听歌时长档位。
 *
 * miuix 无热力图组件，故按组件库规范自绘：squircle 圆角方块 + colorScheme 令牌着色，
 * 非本月的格子留空占位（无底色）。
 */
@Composable
private fun HeatmapCard(month: YearMonth, days: Map<Int, DailyPlayStat>) {
    val scheme = MiuixTheme.colorScheme
    // ISO 星期：周一 = 1，故减 1 得到周一起始的列偏移
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val dayCount = month.lengthOfMonth()
    val rows = (leadingBlanks + dayCount + 6) / 7

    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                for (label in WeekdayLabels) {
                    Text(
                        text = label,
                        style = MiuixTheme.textStyles.footnote2,
                        color = scheme.onBackgroundVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    for (column in 0 until 7) {
                        val dayNumber = row * 7 + column - leadingBlanks + 1
                        if (dayNumber < 1 || dayNumber > dayCount) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val listenMs = days[dayNumber]?.listenMs ?: 0L
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .squircleBackground(heatColor(heatLevel(listenMs)), HeatmapCorner),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 概览/累计的统计行（label 左、值右，行间分隔线；miuix 无现成的「标签-值」行组件，按 Card 规范自绘） */
@Composable
private fun StatisticRow(label: String, value: String, divider: Boolean = true) {
    val scheme = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.main,
                color = scheme.onBackground,
            )
            Text(
                text = value,
                style = MiuixTheme.textStyles.main,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onBackgroundVariant,
            )
        }
        if (divider) HorizontalDivider()
    }
}

/** 高频歌曲行：名次 + 封面 + 标题/艺术家 + 累计次数 */
@Composable
private fun TopSongRow(rank: Int, song: SongPlayStat) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            style = MiuixTheme.textStyles.body1,
            fontWeight = if (rank <= 3) FontWeight.SemiBold else FontWeight.Normal,
            color = if (rank <= 3) scheme.primary else scheme.onBackgroundVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(20.dp),
        )
        MusesCover(
            uri = song.coverUri,
            size = 40.dp,
            radius = MusesCoverRadius.SM,
            modifier = Modifier.padding(start = 6.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = song.title,
                style = MiuixTheme.textStyles.body1,
                color = scheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (song.subtitle.isNotBlank()) {
                Text(
                    text = song.subtitle,
                    style = MiuixTheme.textStyles.footnote1,
                    color = scheme.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            text = song.playCount.toString() + " 次",
            style = MiuixTheme.textStyles.footnote1,
            color = scheme.onBackgroundVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** 当日听歌时长分档（0..4），用于热力图取色 */
private fun heatLevel(listenMs: Long): Int {
    if (listenMs <= 0L) return 0
    return 1 + HeatLevelThresholds.count { listenMs >= it }
}

/** 热力图档位取色：primary 由浅到满，0 档为极浅的背景色块（无记录） */
@Composable
private fun heatColor(level: Int): Color {
    val scheme = MiuixTheme.colorScheme
    return when (level) {
       0 -> scheme.onBackground.copy(alpha = 0.06f)
        1 -> scheme.primary.copy(alpha = 0.3f)
        2 -> scheme.primary.copy(alpha = 0.55f)
        3 -> scheme.primary.copy(alpha = 0.78f)
        else -> scheme.primary
    }
}

/** 听歌时长展示：不到 1 分钟 / 42 分钟 / 8 小时 20 分 */
private fun formatListenDuration(ms: Long): String {
    val minutes = ms / 60_000L
    if (minutes <= 0L) return if (ms > 0L) "不到 1 分钟" else "0 分钟"
    if (minutes < 60L) return minutes.toString() + " 分钟"
    val hours = minutes / 60L
    val rest = minutes % 60L
    return if (rest == 0L) hours.toString() + " 小时" else hours.toString() + " 小时 " + rest.toString() + " 分"
}
