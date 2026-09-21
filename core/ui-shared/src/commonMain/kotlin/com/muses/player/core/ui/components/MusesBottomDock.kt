package com.muses.player.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 悬浮底栏的一项（纯展示数据 + 点击回调；与壳层 `MusesNavItem` 同构，便于直接映射）。
 */
data class MusesBottomDockItem(
    val icon: ImageVector,
    val label: String,
    val active: Boolean,
    val onClick: () -> Unit,
)

/**
 * 容器高。
 *
 * Halcyon 是 64dp，但它的 dock 项更少、屏幕也更宽；我们窄屏要同时塞 6 项导航 + 右侧搜索钮，
 * 64dp 在 360dp 宽上显得「又高又满」，搜索钮也会像块正方形大按钮。
 * 收到 **56dp**（与迷你条同高，两行 chrome 等高更协调），搜索钮同步 56dp。
 */
private val DockHeight = 56.dp

/** 容器内边距（Halcyon 同值 4dp：64 - 4×2 = item 得 56dp） */
private val DockInnerPadding = 4.dp

/** item 之间的水平间隙 */
private val DockItemSpacing = 2.dp

/** item 高（= 容器高 - 内边距×2）——选中气泡高度与之相等，即「撑满 item」 */
private val DockItemHeight = DockHeight - DockInnerPadding * 2

/** 选中气泡底色透明度（Halcyon 为 0.15f） */
private const val DockIndicatorAlpha = 0.15f

/** item 图标尺寸 */
private val DockIconSize = 24.dp

/** 旁边独立动作钮的边长（与底栏等高，两件并排时上下齐平；Halcyon 为 64dp） */
private val DockActionSize = 56.dp

/** item 文字字号（Halcyon 为 11sp，窄屏 6 项并排时收到 10sp 避免拥挤） */
private val DockLabelFontSize = 10.sp

/**
 * 自研悬浮底栏（替代 miuix 官方 `FloatingNavigationBar`）。
 *
 * 为什么自研：官方组件的高度/图标尺寸由内部常量固定（IconSize 28 + IconPadding 10×2），
 * **没有任何尺寸参数**，无法对齐参考项目 Halcyon 的「容器 64dp / item 56dp / 图标 + 11sp 文字」规格；
 * 而底栏尺寸又与「滚动融合」的紧凑形态必须成套（融合后两侧圆 pill 需与迷你条严格齐平）。
 *
 * 材质与 [MiniPlayerBar] 同款（同一套悬浮件视觉）：`surfaceContainer` 底 + 官方 `dropShadow`
 * 阴影 + squircle 平滑圆角。
 *
 * 选中态对齐 Halcyon 的 `FloatingBottomBar`：一个**撑满 item（宽 × 56dp 高）的胶囊气泡**，
 * 随选中项做弹簧位移；气泡之上的内容（图标 + 文字）用 `primary` 高亮，未选中用 `onSurface`。
 *
 * 演进教训：一开始只给「整块 item」铺 12% 底色（大圆角矩形，笨重）；后来改成 44dp 正圆
 * （图标 24dp 配 44dp 圆，空）；再改「撑满 item 宽的扁平胶囊（40dp 高）」仍然空。
 * 根因是**底栏只有图标**——Halcyon 的气泡之所以成立，是因为 item 里是图标 + 文字的**两行内容**，
 * 56dp 气泡恰好包住它们。所以正确做法是补上文字，而不是继续调气泡尺寸。
 */
@Composable
fun MusesBottomDock(
    items: List<MusesBottomDockItem>,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val activeIndex = items.indexOfFirst { it.active }
    Box(
        modifier = modifier
            .height(DockHeight)
            .dropShadow(
                shape = shape,
                shadow = Shadow(radius = 10.dp, color = Color.Black, alpha = 0.2f),
            )
            .squircleBackground(scheme.surfaceContainer, 50.dp)
            .padding(DockInnerPadding),
    ) {
        // 滑动胶囊气泡：铺在内容层之下，位置随选中项弹簧位移（左右对齐到 item 左缘）
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (activeIndex >= 0 && items.isNotEmpty()) {
                val count = items.size
                val itemWidth = (maxWidth - DockItemSpacing * (count - 1)) / count
                val target = (itemWidth + DockItemSpacing) * activeIndex
                val indicatorX by animateDpAsState(
                    targetValue = target,
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 520f),
                    label = "dock-indicator",
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = indicatorX)
                        .width(itemWidth)
                        .height(DockItemHeight)
                        .background(
                            color = scheme.primary.copy(alpha = DockIndicatorAlpha),
                            shape = shape,
                        ),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DockItemSpacing),
        ) {
            items.forEach { item ->
                MusesBottomDockTab(
                    item = item,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 单个底栏项：**图标（24dp）+ 文字（11sp）** 两行垂直排列，间距 1dp。
 *
 * 文字不是装饰——它是选中气泡「撑满 item」的前提条件（见 [MusesBottomDock] 的演进教训）；
 * 同时底栏本就该给出可读的导航标签（原先只把 label 放进 contentDescription）。
 */
@Composable
private fun MusesBottomDockTab(
    item: MusesBottomDockItem,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    val contentColor = if (item.active) scheme.primary else scheme.onSurface
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = item.onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(DockIconSize),
        )
        Text(
            text = item.label,
            fontSize = DockLabelFontSize,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/**
 * 底栏旁的**独立动作钮**（对齐 Halcyon 的 `BottomDockActionPill`）：64×64 正方形，
 * 与 [MusesBottomDock] 同材质（`surfaceContainer` + `dropShadow` + squircle 圆角），
 * 只有图标没有文字，用来承载「搜索」这类非导航动作。
 *
 * 交互对齐 Halcyon：按压时整体放大 1.06 倍；选中（停在搜索页）时叠一层 8% 的 `onSurface` 底
 * 并把图标换成 `primary`。
 */
@Composable
fun MusesDockActionPill(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 620f),
        label = "dock-action-scale",
    )
    val overlayAlpha by animateFloatAsState(
        targetValue = when {
            pressed -> 1f
            selected -> 0.72f
            else -> 0f
        },
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 700f),
        label = "dock-action-overlay",
    )
    Box(
        modifier = modifier
            .size(DockActionSize)
            .dropShadow(
                shape = shape,
                shadow = Shadow(radius = 10.dp, color = Color.Black, alpha = 0.2f),
            )
            .squircleBackground(scheme.surfaceContainer, 50.dp)
            .padding(DockInnerPadding)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = scheme.onSurface.copy(alpha = 0.08f * overlayAlpha),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) scheme.primary else scheme.onSurface,
            modifier = Modifier.size(DockIconSize),
        )
    }
}
