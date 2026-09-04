package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import com.muses.player.core.ui.icons.TablerIcons
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.theme.HazeBlurStyleData
import com.muses.player.core.ui.theme.LocalHazeBlurState
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltDarkColors
import com.muses.player.core.ui.theme.SaltSpacing
import com.muses.player.core.ui.theme.musesNavbarHazeStyle
import com.muses.player.core.uishared.platform.PlatformInsets
import com.muses.player.core.uishared.platform.platformBlurModifier

/**
 * `.m-navbar` —— 吸顶导航栏（MNavbar.vue 一比一翻译）。
 *
 * 视觉契约：
 * - 顶部避让 `--m-navbar-pt = max(16px, safe-area-top)`；
 * - 内容行高 44px，水平 padding `--m-spacing`(16px)（+safe-left/right）；
 * - 灰底磨砂玻璃：`--m-navbar-glass-bg` + blur(20px)。**blur 说明**：
 *   Compose 无原生 backdrop-filter；且 Web 版在 MuMu WebView 上 backdrop-filter
 *   本就失效、由 alpha + 内高光承担玻璃观感 —— 原生同策略只画半透明底，
 *   真机 backdrop blur 待后续统一方案（haze/RenderEffect）再补；
 * - 内高光：`inset 0 1px 0 rgba(255,255,255,.65)`（暗色 .1）——顶部物理 1px 高光线；
 * - 无底部横线（08-16 全页面统一，navbar 底直接衔接内容）；
 * - 标题 17px / 600 / line-height 1.3 / 单行省略 / start 对齐；
 * - subnavbar 插槽：与 navbar 同一块玻璃、无分界线，行高 `--m-list-row-h`(56dp)；
 * - transparent 变体：无背景、无内高光（PlayerPage 沉浸式用）。
 */
@Composable
fun SaltNavbar(
    title: String,
    modifier: Modifier = Modifier,
    transparent: Boolean = false,
    left: (@Composable RowScope.() -> Unit)? = null,
    right: (@Composable RowScope.() -> Unit)? = null,
    /** 工具条/搜索栏插槽：与 navbar 同一块玻璃（`.m-navbar__subnavbar`） */
    subnavbar: (@Composable RowScope.() -> Unit)? = null,
) {
    val salt = LocalSaltColors.current
    val isDark = salt === SaltDarkColors

    // --m-navbar-pt: max(16px, safe-area-top)
    // 使用 PlatformInsets 替代 WindowInsets.statusBars（跨平台兼容）
    val statusBarTop = PlatformInsets.statusBarHeightDp().toDouble().dp
    val navbarPt = maxOf(SaltSpacing.navbarTopPaddingMin, statusBarTop)

    // 平台模糊风格数据：Android 真 Haze / 桌面纯色降级
    // LocalHazeBlurState 由 app 层（TabsLayout）provide，值与 LocalMusesHazeState 相同
    val hazeState = LocalHazeBlurState.current
    val hazeStyle: HazeBlurStyleData? = if (!transparent) musesNavbarHazeStyle(isDark) else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (transparent) {
                    Modifier
                } else {
                    platformBlurModifier(
                        isDark = isDark,
                        backgroundColor = salt.navbarGlassBg,
                        hazeState = hazeState,
                        hazeStyleData = hazeStyle,
                    )
                },
            )
            .drawBehind {
                if (!transparent) {
                    // inset 0 1px 0 rgba(255,255,255,.65)（暗色 .1）：顶部 1px 物理像素高光（真磨砂上叠加）
                    drawRect(
                        color = if (isDark) {
                            Color.White.copy(alpha = 0.1f)
                        } else {
                            Color.White.copy(alpha = 0.65f)
                        },
                        topLeft = Offset.Zero,
                        size = Size(size.width, 1f),
                    )
                }
            }
            .padding(top = navbarPt),
    ) {
        Column {
            // __inner：44px 内容行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = SaltSpacing.spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 内建汉堡按钮：对照 MNavbar——navigationDrawer 注入存在且
                // 未提供自定义 left 插槽时自动渲染（打开导航抽屉）
                val openDrawer = LocalSaltOpenDrawer.current
                if (left != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        // __left { margin-right: var(--m-spacing-sub) }
                        modifier = Modifier.padding(end = SaltSpacing.spacingSub),
                    ) {
                        left()
                    }
                } else if (openDrawer != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = SaltSpacing.spacingSub),
                    ) {
                        SaltIconButton(onClick = openDrawer) {
                            Icon(TablerIcons.Menu, contentDescription = "打开导航菜单")
                        }
                    }
                }
                // __title
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = (17f * 1.3f).sp,
                    color = salt.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (right != null) 8.dp else 0.dp),
                )
                if (right != null) {
                    // __right：无容器间距，子元素自控 margin（与 Web 一致）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        right()
                    }
                }
            }
            // __subnavbar：同一块玻璃，无分界线
            if (subnavbar != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SaltSpacing.listRowHeight)
                        .padding(horizontal = SaltSpacing.spacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    subnavbar()
                }
            }
        }
    }
}
