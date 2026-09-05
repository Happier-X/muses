package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius

/**
 * 刮削页共用组件（V3 刮削页共用化，U4 设置页模式：纯 UI + 回调）。
 *
 * 背景：安卓 `feature:scrape` 的 ScrapeScreen（四态机 queue/matching/preview/result）
 * 与 ScrapeReviewScreen（Searching/Review/Empty/Writing/Success）各自手写行/进度/预览卡；
 * 桌面端上刮削功能入口（composeApp ScrapeScreen）。本文件把三者 UI 结构中的平台无关部分上收。
 *
 * 范围：
 * - 在内：候选项行（封面+标题+来源+置信度+选择回调）、进度条（当前/总数+取消回调）、
 *   预览卡（变更前后对比+确认/跳过回调，逐字段勾选行复用）、写回结果行。
 * - 在外：ViewModel 编排（匹配链/写回/队列）、编辑表/审核三维候选切换（审核页特有，留平台侧）。
 *
 * 约束：commonMain 零安卓 import、零业务模块依赖（对齐 SourceComponents 契约：
 * 展示数据由调用方映射，不 import core:model——安卓从 `PreviewCandidate`/
 * `WritebackResult` 映射，桌面从 Room `SongEntity`/引擎结果映射）；
 * 纯 UI + 回调，不接触 DAO/Repository/引擎。
 */

// ---------------------------------------------------------------------------
// 展示数据（平台无关，只承载展示信息 + 回调键）
// ---------------------------------------------------------------------------

/**
 * 候选项展示数据。
 *
 * @param songId 回调键（选择/重试/去审核均回传此 id）
 * @param title 标题行（歌名）
 * @param subtitle 副标题行（歌手 · 专辑，调用方拼好）
 * @param coverUri 封面（远程候选 URL 或本地 coverUri；空 = 占位）
 * @param sourceLabel 来源角标文案（调用方取 `OnlineTextSource.wire`/`OnlineCoverSource.wire`）
 * @param confidenceLabel 置信度展示文案（安卓 `PreviewCandidate.confidence` /
 *   引擎 `Ok.confidence?.name`；null = 未命中不展示）
 */
data class SharedScrapeCandidate(
    val songId: String,
    val title: String,
    val subtitle: String? = null,
    val coverUri: String? = null,
    val sourceLabel: String? = null,
    val confidenceLabel: String? = null,
)

/**
 * 预览字段行展示数据（变更前后对比的一行）。
 *
 * @param key 字段键（title/artist/album/cover/lyrics，回调回传）
 * @param label 字段中文名（标题/歌手/专辑/封面/歌词）
 * @param original 本地原值
 * @param updated 候选新值；null = 无候选（行展示原值，可勾选性由调用方经 enabled 保证）
 * @param checked 是否勾选写回
 */
data class SharedReviewField(
    val key: String,
    val label: String,
    val original: String,
    val updated: String?,
    val checked: Boolean,
)

/** 写回结果状态语义（圆点/状态文字配色；调用方从 `WritebackStatus` 三值映射）。 */
enum class ScrapeStatusKind {
    /** success：绿 */
    SUCCESS,

    /** file-failed：橙（库已更新但文件写入失败，值得重刮） */
    WARNING,

    /** failed：红 */
    ERROR,
}

/**
 * 写回结果行展示数据（result 态行）。
 *
 * @param songId 回调键
 * @param title 歌名（调用方从 queueTitles/库反查，缺失回退 `take(8)` 由调用方做）
 * @param statusKind 状态语义（配色，从 `WritebackStatus` 映射）
 * @param statusWire 状态文案（`WritebackStatus.wire`）
 * @param detail 失败详情文案；成功时 null
 * @param detailHighlight true = 详情用 primary 色（限流提示「限流，稍后重试」）
 * @param retryText 重试按钮文案（null = 不渲染按钮；「重试」/「限流重试」由调用方定）
 */
data class SharedWritebackResult(
    val songId: String,
    val title: String,
    val statusKind: ScrapeStatusKind,
    val statusWire: String,
    val detail: String? = null,
    val detailHighlight: Boolean = false,
    val retryText: String? = null,
)

// ---------------------------------------------------------------------------
// 候选项行
// ---------------------------------------------------------------------------

/**
 * 刮削候选项行 —— 封面 + 标题 + 来源 + 置信度 + 选择回调。
 *
 * 视觉契约（对照 ScrapeScreen 预览卡头 / ScrapeReviewScreen 歌曲头）：
 * - 48dp SaltCover（远程/本地图，空占位）+ 12dp 间距 + 文字区；
 * - 标题 14sp/600 单行省略，副标题 12sp text2 单行省略；
 * - 来源角标（surface2 底 9sp text2）+ 置信度角标（primary 浅底 10sp primary）；
 * - [onSelect] = null 时为纯展示（无点击）；非空时整行可点（无涟漪，对齐 SaltListItem）。
 */
@Composable
fun ScrapeCandidateRow(
    candidate: SharedScrapeCandidate,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onSelect: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val salt = LocalSaltColors.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onSelect != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onSelect,
                    )
                } else {
                    Modifier
                },
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SaltCover(uri = candidate.coverUri, size = 48.dp, radius = SaltCoverRadius.SM)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = candidate.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) salt.primary else salt.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!candidate.subtitle.isNullOrBlank()) {
                Text(
                    text = candidate.subtitle,
                    fontSize = 12.sp,
                    color = salt.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                if (candidate.sourceLabel != null) {
                    ScrapeBadgeBox(text = candidate.sourceLabel)
                }
                if (candidate.confidenceLabel != null) {
                    Box(
                        Modifier
                            .background(salt.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(candidate.confidenceLabel, fontSize = 10.sp, color = salt.primary)
                    }
                }
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

// ---------------------------------------------------------------------------
// 进度条
// ---------------------------------------------------------------------------

/**
 * 刮削进度条 —— 当前/总数 + 取消回调。
 *
 * 视觉契约（对照 ScrapeScreen MatchingStateContent）：
 * - 居中菊花 + 「正在匹配 current / total」17sp/600 + LinearProgress + 当前歌名 13sp text2；
 * - [message] 非空时 primary 色 13sp 提示行（如限流「等待限流恢复…」）；
 * - [onCancel] = null 时不渲染取消按钮（安卓 matching 态沿用无取消行为）；
 *   非空时渲染 SaltTextButton「取消」。
 */
@Composable
fun ScrapeProgressBar(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
    currentItem: String? = null,
    message: String? = null,
    title: String = "正在匹配 $current / $total",
    onCancel: (() -> Unit)? = null,
) {
    val salt = LocalSaltColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(20.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = salt.text)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) current.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        if (!currentItem.isNullOrBlank()) {
            Text(
                currentItem,
                fontSize = 13.sp,
                color = salt.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                fontSize = 13.sp,
                color = salt.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onCancel != null) {
            Spacer(Modifier.height(12.dp))
            SaltTextButton(onClick = onCancel, text = "取消")
        }
    }
}

// ---------------------------------------------------------------------------
// 预览卡（变更前后对比 + 确认/跳过回调）
// ---------------------------------------------------------------------------

/**
 * 刮削预览卡 —— 变更前后对比 + 确认/跳过回调。
 *
 * 视觉契约（对照 ScrapeScreen 预览卡）：
 * - surface1 底 + radius-card + hairline 边框（有勾选字段时 primary 半透明边框），内缩 12dp；
 * - 头部复用 [ScrapeCandidateRow]（封面+标题+置信度）；
 * - 字段行复用 [ScrapeReviewFieldRow]（Checkbox +「本地值 → 候选值」）；
 * - [onConfirm]/[onSkip] 均 null 时不渲染底部按钮行（纯展示，如桌面选中预览）；
 *   非空时渲染「确认」/「跳过」SaltTextButton 行。
 */
@Composable
fun ScrapeReviewCard(
    candidate: SharedScrapeCandidate,
    fields: List<SharedReviewField>,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onToggleField: ((String) -> Unit)? = null,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    skipText: String? = null,
    onSkip: (() -> Unit)? = null,
) {
    val salt = LocalSaltColors.current
    val hasChecked = fields.any { it.checked }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(salt.surface1, RoundedCornerShape(SaltRadius.card))
            .border(
                0.5.dp,
                if (hasChecked) salt.primary.copy(alpha = 0.5f) else salt.hairline,
                RoundedCornerShape(SaltRadius.card),
            )
            .padding(12.dp),
    ) {
        ScrapeCandidateRow(candidate = candidate, selected = selected)
        Spacer(Modifier.height(6.dp))
        fields.forEach { field ->
            ScrapeReviewFieldRow(
                field = field,
                onCheckedChange = onToggleField?.let { cb -> { cb(field.key) } },
            )
        }
        if (onConfirm != null || onSkip != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onSkip != null) {
                    SaltTextButton(onClick = onSkip, text = skipText ?: "跳过")
                }
                if (onConfirm != null) {
                    SaltTextButton(onClick = onConfirm, text = confirmText ?: "确认")
                }
            }
        }
    }
}

/**
 * 预览字段行 —— Checkbox +「本地值 → 候选值」（对照 ScrapeScreen PreviewFieldRow /
 * ScrapeReviewScreen FieldCheckRow；两处语义一致：updated 为空或与原值相同则只展示原值）。
 *
 * 审核页扩展（对照 FieldCheckRow 来源/推荐角标）：[sourceBadge] 非空时渲染来源角标，
 * [recommended] 为 true 时渲染「推荐」角标（EditCloudMetaSearch 无 per-hit 置信度，
 * 推荐候选 defaultIndex 以「推荐」角标置于原置信度角标位置）。
 *
 * 写回安全红线：[enabled] = false 时 Checkbox 禁用（审核页无解析值字段不可勾，
 * 对齐安卓 FieldCheckRow 的 enabled 参数）。
 *
 * @param onCheckedChange null = 纯展示（无 Checkbox，点击无反应；桌面预览用）
 */
@Composable
fun ScrapeReviewFieldRow(
    field: SharedReviewField,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: (() -> Unit)? = null,
    sourceBadge: String? = null,
    recommended: Boolean = false,
) {
    val salt = LocalSaltColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onCheckedChange != null) {
            Checkbox(
                checked = field.checked,
                onCheckedChange = { onCheckedChange() },
                enabled = enabled,
                colors = CheckboxDefaults.colors(checkedColor = salt.primary),
            )
        }
        Column(Modifier.weight(1f).padding(start = if (onCheckedChange != null) 4.dp else 0.dp)) {
            val display = if (field.updated != null && field.updated != field.original) {
                "${field.original} → ${field.updated}"
            } else {
                field.original
            }
            Text(
                "${field.label}：$display",
                fontSize = 12.sp,
                color = if (field.checked) salt.text else salt.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (sourceBadge != null) {
            ScrapeBadgeBox(text = sourceBadge)
            Spacer(Modifier.size(4.dp))
        }
        if (recommended) {
            Box(
                Modifier
                    .background(salt.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text("推荐", fontSize = 9.sp, color = salt.primary)
            }
        }
    }
}

/**
 * 写回结果行 —— 状态圆点 + 歌名 + 状态文案 + 重试回调。
 *
 * 视觉契约（对照 ScrapeScreen ResultStateContent 行）：
 * - 8dp 状态圆点（success 绿 / file-failed 橙 / failed 红）+ 歌名 13sp text2 + 状态 13sp；
 * - [SharedWritebackResult.retryText] 非空时渲染 SaltTextButton 重试；
 * - 详情文案 11sp 最多两行（限流提示经 detailHighlight 用 primary 色，其余 text2）。
 */
@Composable
fun ScrapeResultRow(
    result: SharedWritebackResult,
    modifier: Modifier = Modifier,
    onRetry: ((String) -> Unit)? = null,
) {
    val salt = LocalSaltColors.current
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(8.dp).background(statusColor(result.statusKind), RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.size(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                fontSize = 13.sp,
                color = salt.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (result.detail != null) {
                Text(
                    result.detail,
                    fontSize = 11.sp,
                    color = if (result.detailHighlight) salt.primary else salt.text2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(result.statusWire, fontSize = 13.sp, color = statusColor(result.statusKind))
        if (result.retryText != null && onRetry != null) {
            Spacer(Modifier.size(8.dp))
            SaltTextButton(
                text = result.retryText,
                onClick = { onRetry(result.songId) },
            )
        }
    }
}

/** 来源角标（surface2 底 9sp text2；对照审核页 BadgeBox）。 */
@Composable
fun ScrapeBadgeBox(text: String, modifier: Modifier = Modifier) {
    val salt = LocalSaltColors.current
    Box(
        modifier
            .background(salt.surface2, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(text, fontSize = 9.sp, color = salt.text2)
    }
}

/** 写回状态配色（对齐 ScrapeScreen statusColor 令牌）。 */
private fun statusColor(kind: ScrapeStatusKind): Color = when (kind) {
    ScrapeStatusKind.SUCCESS -> Color(0xFF34C759)
    ScrapeStatusKind.WARNING -> Color(0xFFFF9500)
    ScrapeStatusKind.ERROR -> Color(0xFFFF3B30)
}

/**
 * 远程封面候选缩略图（审核页封面区；72dp，选中态 primary 2dp 边框）。
 * 点击回调由调用方注入（安卓「再点已选项→大图预览，否则切换选中」逻辑留在调用方）。
 */
@Composable
fun ScrapeCoverThumb(
    url: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val salt = LocalSaltColors.current
    Box(
        modifier
            .size(72.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(salt.surface2)
            .border(
                width = if (selected) 2.dp else 0.5.dp,
                color = if (selected) salt.primary else salt.surface2,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
