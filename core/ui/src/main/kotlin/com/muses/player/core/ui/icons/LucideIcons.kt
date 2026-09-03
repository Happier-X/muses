package com.muses.player.core.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.*

/**
 * Lucide Icons 包装器 - 提供与 Material Icons 相同的 API 接口
 * 便于从 Material Icons 迁移到 Lucide Icons
 */
object LucideIcons {
    // 音乐相关
    val MusicNote: ImageVector = Lucide.Music
    val MusicNoteOutlined: ImageVector = Lucide.Music
    val QueueMusic: ImageVector = Lucide.ListMusic
    val PlaylistPlay: ImageVector = Lucide.ListMusic

    // 播放控制
    val Play: ImageVector = Lucide.Play
    val Pause: ImageVector = Lucide.Pause
    val SkipPrevious: ImageVector = Lucide.SkipBack
    val SkipNext: ImageVector = Lucide.SkipForward
    val Repeat: ImageVector = Lucide.Repeat
    val RepeatOne: ImageVector = Lucide.Repeat1
    val Shuffle: ImageVector = Lucide.Shuffle

    // 导航
    val ArrowBack: ImageVector = Lucide.ArrowLeft
    val ArrowForward: ImageVector = Lucide.ArrowRight
    val ChevronRight: ImageVector = Lucide.ChevronRight
    val ChevronLeft: ImageVector = Lucide.ChevronLeft

    // 操作
    val Close: ImageVector = Lucide.X
    val Delete: ImageVector = Lucide.Trash
    val Search: ImageVector = Lucide.Search
    val MoreVert: ImageVector = Lucide.EllipsisVertical
    val MoreHorizontal: ImageVector = Lucide.Ellipsis
    val Check: ImageVector = Lucide.Check
    val CheckBox: ImageVector = Lucide.SquareCheck
    val CheckBoxOutlineBlank: ImageVector = Lucide.Square
    val Refresh: ImageVector = Lucide.RefreshCw

    // 文件和文件夹
    val Folder: ImageVector = Lucide.Folder
    val FolderOpen: ImageVector = Lucide.FolderOpen
    val File: ImageVector = Lucide.File

    // 人物
    val Person: ImageVector = Lucide.User
    val PersonOutline: ImageVector = Lucide.User

    // 媒体
    val Album: ImageVector = Lucide.Disc3
    val Mic: ImageVector = Lucide.Mic

    // 界面
    val Menu: ImageVector = Lucide.Menu
    val Settings: ImageVector = Lucide.Settings
    val Info: ImageVector = Lucide.Info
    val BugReport: ImageVector = Lucide.Bug
    val Checklist: ImageVector = Lucide.ListChecks
    val FormatListBulleted: ImageVector = Lucide.List
    val Radio: ImageVector = Lucide.Radio

    // 位置
    val MyLocation: ImageVector = Lucide.MapPin
    val LocationOn: ImageVector = Lucide.MapPin

    // 其他
    val Translate: ImageVector = Lucide.Languages
    val Share: ImageVector = Lucide.Share
    val Download: ImageVector = Lucide.Download
    val Upload: ImageVector = Lucide.Upload
    val Edit: ImageVector = Lucide.Pencil
    val Add: ImageVector = Lucide.Plus
    val Remove: ImageVector = Lucide.Minus
    val CheckCircle: ImageVector = Lucide.CircleCheck
    val Warning: ImageVector = Lucide.TriangleAlert
    val Error: ImageVector = Lucide.CircleX
}
