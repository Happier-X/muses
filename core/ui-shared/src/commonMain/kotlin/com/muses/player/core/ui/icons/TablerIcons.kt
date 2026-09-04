package com.muses.player.core.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.filled.*
import com.composables.icons.tabler.outline.*

/**
 * Tabler Icons 包装器 - 语义名稳定，底层图标库实现可整体切换
 * （当前为 Tabler：outline 用 Tabler.Outline.*，fill 用 Tabler.Filled.*；
 * 该库扩展属性保留 tabler 原始 kebab 命名，如 Arrows_shuffle / Menu_2）
 */
object TablerIcons {
    // 音乐相关
    val MusicNote: ImageVector = Tabler.Outline.Music
    val MusicNoteOutlined: ImageVector = Tabler.Outline.Music
    val QueueMusic: ImageVector = Tabler.Outline.Playlist
    val PlaylistPlay: ImageVector = Tabler.Outline.Playlist

    // 播放控制（stroke 风格）
    val Play: ImageVector = Tabler.Outline.PlayerPlay
    val Pause: ImageVector = Tabler.Outline.PlayerPause
    val SkipPrevious: ImageVector = Tabler.Outline.PlayerSkipBack
    val SkipNext: ImageVector = Tabler.Outline.PlayerSkipForward
    val Repeat: ImageVector = Tabler.Outline.Repeat
    val RepeatOne: ImageVector = Tabler.Outline.RepeatOnce
    val Shuffle: ImageVector = Tabler.Outline.ArrowsShuffle

    // 播放控制（fill 风格，Tabler 官方带圆角路径）
    val PlayFill: ImageVector = Tabler.Filled.PlayerPlay
    val PauseFill: ImageVector = Tabler.Filled.PlayerPause
    val SkipPreviousFill: ImageVector = Tabler.Filled.PlayerSkipBack
    val SkipNextFill: ImageVector = Tabler.Filled.PlayerSkipForward

    // 导航
    val ArrowBack: ImageVector = Tabler.Outline.ArrowLeft
    val ArrowForward: ImageVector = Tabler.Outline.ArrowRight
    val ChevronRight: ImageVector = Tabler.Outline.ChevronRight
    val ChevronLeft: ImageVector = Tabler.Outline.ChevronLeft

    // 操作
    val Close: ImageVector = Tabler.Outline.X
    val Delete: ImageVector = Tabler.Outline.Trash
    val Search: ImageVector = Tabler.Outline.Search
    val MoreVert: ImageVector = Tabler.Outline.DotsVertical
    val MoreHorizontal: ImageVector = Tabler.Outline.Dots
    val Check: ImageVector = Tabler.Outline.Check
    val CheckBox: ImageVector = Tabler.Outline.SquareCheck
    val CheckBoxOutlineBlank: ImageVector = Tabler.Outline.Square
    val Refresh: ImageVector = Tabler.Outline.Refresh

    // 文件和文件夹
    val Folder: ImageVector = Tabler.Outline.Folder
    val FolderOpen: ImageVector = Tabler.Outline.FolderOpen
    val File: ImageVector = Tabler.Outline.File

    // 人物
    val Person: ImageVector = Tabler.Outline.User
    val PersonOutline: ImageVector = Tabler.Outline.User

    // 媒体
    val Album: ImageVector = Tabler.Outline.Disc
    val Mic: ImageVector = Tabler.Outline.Microphone

    // 界面
    val Menu: ImageVector = Tabler.Outline.Menu2
    val Settings: ImageVector = Tabler.Outline.Settings
    val Info: ImageVector = Tabler.Outline.InfoCircle
    val BugReport: ImageVector = Tabler.Outline.Bug
    val Checklist: ImageVector = Tabler.Outline.ListCheck
    val FormatListBulleted: ImageVector = Tabler.Outline.List
    val Radio: ImageVector = Tabler.Outline.Broadcast

    // 位置
    val MyLocation: ImageVector = Tabler.Outline.CurrentLocation
    val LocationOn: ImageVector = Tabler.Outline.MapPin

    // 其他
    val Translate: ImageVector = Tabler.Outline.Language
    val Share: ImageVector = Tabler.Outline.Share
    val Download: ImageVector = Tabler.Outline.Download
    val Upload: ImageVector = Tabler.Outline.Upload
    val Edit: ImageVector = Tabler.Outline.Pencil
    val Add: ImageVector = Tabler.Outline.Plus
    val Remove: ImageVector = Tabler.Outline.Minus
    val CheckCircle: ImageVector = Tabler.Outline.CircleCheck
    val Warning: ImageVector = Tabler.Outline.AlertTriangle
    val Error: ImageVector = Tabler.Outline.CircleX
}
