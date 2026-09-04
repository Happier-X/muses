package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius

/**
 * 跨平台 WebDAV 浏览条目（平台无关，只承载展示信息）。
 *
 * 安卓端由调用方从 `WebDavClient.list()` 返回的 `WebDavItem`（目录项）映射而来；
 * 桌面端由调用方从桌面 PROPFIND 加载器返回的同名字段映射而来。
 * 共用组件只认本数据类，不接触 `core:webdav` 的 `WebDavItem`，保持 commonMain 零业务依赖。
 */
data class WebDavBrowseItem(
    val name: String,
    val url: String,
    val isDirectory: Boolean = true,
    val contentLength: Long = 0L,
    val lastModified: String? = null,
)

/**
 * WebDAV 目录浏览共用组件（浏览页共用化）。
 *
 * 视觉契约（对照安卓 `WebDavBrowseScreen` + `DirectoryRow`）：
 * - 路径导航行：返回上级 SaltTextButton(SMALL) + 当前路径 13sp text2 单行省略；
 * - 加载态：居中 CircularProgressIndicator +「正在读取目录…」14sp text2；
 * - 空目录：居中 13sp text2（多选「当前目录没有可添加的子文件夹。」/ 单选「当前目录没有子文件夹。」）；
 * - 目录行：surface1 背景 + radius-sm + 目录图标 primary 28dp + 名称 16sp/600 单行省略 +
 *   路径 13sp text2 单行省略 + 尾部 SaltTextButton(SMALL)（单选「选择」/ 多选「进入」）；
 * - 多选模式：行首复选框（选中 SquareCheck primary / 未选 Square text2，24dp）；
 * - 多选底部确认按钮：全宽 SaltTextButton「添加选中的 N 个文件夹」；
 * - 错误态由调用方承载（安卓 AlertDialog / 桌面行内文案），本组件只收 `errorText` 做行内展示，
 *   `onDismissError` 为空时不渲染关闭按钮。
 *
 * 纯 UI 组件，零平台依赖，所有业务逻辑经回调注入。
 *
 * @param mode "single" 单选确认（编辑回填）/ "multiple" 多选确认（添加流程）
 * @param currentPath 当前路径（导航行展示）
 * @param directories 当前目录下的目录项（调用方已过滤排序）
 * @param selectedPaths 已选路径集合（多选模式）
 * @param isLoading 加载中
 * @param canGoParent 是否可返回上级（调用方按 parentPath != null && !isLoading 计算）
 * @param errorText 错误文案；null = 无错误
 */
@Composable
fun WebDavBrowseList(
    mode: String,
    currentPath: String,
    directories: List<WebDavBrowseItem>,
    selectedPaths: Set<String>,
    isLoading: Boolean,
    canGoParent: Boolean,
    onGoParent: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onOpenDirectory: (String) -> Unit,
    onConfirmSingle: (String) -> Unit,
    onConfirmMultiple: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    onDismissError: (() -> Unit)? = null,
) {
    val salt = LocalSaltColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        // .webdav-browser__nav（路径导航）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaltTextButton(
                onClick = onGoParent,
                text = "返回上级",
                enabled = canGoParent,
                size = SaltTextButtonSize.SMALL,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = currentPath,
                fontSize = 13.sp,
                color = salt.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    "正在读取目录…",
                    fontSize = 14.sp,
                    color = salt.text2,
                )
            }
        } else {
            if (directories.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = if (mode == "multiple") {
                            "当前目录没有可添加的子文件夹。"
                        } else {
                            "当前目录没有子文件夹。"
                        },
                        fontSize = 13.sp,
                        color = salt.text2,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(directories, key = { it.url }) { directory ->
                        WebDavBrowseRow(
                            directory = directory,
                            isSelected = selectedPaths.contains(directory.url),
                            mode = mode,
                            onSelect = { onToggleSelection(directory.url) },
                            onConfirm = {
                                if (mode == "single") {
                                    onConfirmSingle(directory.url)
                                } else {
                                    onOpenDirectory(directory.url)
                                }
                            },
                        )
                    }
                }

                if (mode == "multiple" && selectedPaths.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    SaltTextButton(
                        onClick = { onConfirmMultiple(selectedPaths.toList()) },
                        text = "添加选中的 ${selectedPaths.size} 个文件夹",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 96.dp),
                    )
                }
            }
        }

        if (errorText != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = errorText,
                    fontSize = 13.sp,
                    color = salt.danger,
                    modifier = Modifier.weight(1f),
                )
                if (onDismissError != null) {
                    SaltTextButton(
                        onClick = onDismissError,
                        text = "关闭",
                        size = SaltTextButtonSize.SMALL,
                    )
                }
            }
        }
    }
}

/**
 * 目录行 —— 对照安卓 `DirectoryRow`（`.webdav-browser__row`）。
 */
@Composable
private fun WebDavBrowseRow(
    directory: WebDavBrowseItem,
    isSelected: Boolean,
    mode: String,
    onSelect: () -> Unit,
    onConfirm: () -> Unit,
) {
    val salt = LocalSaltColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(salt.surface1, RoundedCornerShape(SaltRadius.sm))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mode == "multiple") {
            Icon(
                imageVector = if (isSelected) TablerIcons.CheckBox else TablerIcons.CheckBoxOutlineBlank,
                contentDescription = if (isSelected) "取消选择" else "选择",
                tint = if (isSelected) salt.primary else salt.text2,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onSelect() },
            )
            Spacer(Modifier.width(8.dp))
        }

        Icon(
            imageVector = TablerIcons.Folder,
            contentDescription = null,
            tint = salt.primary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = directory.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = salt.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = directory.url,
                fontSize = 13.sp,
                color = salt.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        SaltTextButton(
            onClick = onConfirm,
            text = if (mode == "single") "选择" else "进入",
            size = SaltTextButtonSize.SMALL,
        )
    }
}
