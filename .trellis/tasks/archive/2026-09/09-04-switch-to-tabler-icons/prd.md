# PRD：全量切换图标库到 Tabler Icons

## Goal

将项目中所有图标（Lucide 包装器 + 直引 Material Icons）统一替换为 Tabler Icons（Composables `icons-tabler` 库），重命名包装器为 `TablerIcons`，移除 Lucide 与 Material Icons 依赖。

## 背景

- `core/ui` 的 `LucideIcons` 包装器（91 行）把 34 个 Material 风格语义名映射到 Lucide（`icons-lucide-android:1.1.0`），业务代码全部经它引用（约 68 处、18 个文件）。
- 14 个文件绕过包装器直接 `import androidx.compose.material.icons.*`（约 30 种 Material filled 图标）。
- 工作区已有未提交改动：加入 Tabler Filled 依赖（`icons-tabler-filled-cmp-android:2.2.1`）、删除手写 `FillIcons.kt`、播放控制 fill 图标改用 `Tabler.Filled.Player*`——本任务将其一并收编提交。

## Requirements

1. `gradle/libs.versions.toml` + `core/ui/build.gradle.kts`：新增 `icons-tabler-cmp-android:2.2.1`（已确认存在于 Maven Central），移除 `lucide-icons`；material-icons 依赖迁移后若无代码引用一并移除。
2. `LucideIcons.kt` → `TablerIcons.kt`：对象改名 `TablerIcons`，内部映射全部改为 Tabler outline（`Tabler.*`），fill 播放控制保持 `Tabler.Filled.*`；补充直引 Material 图标所需的新语义名（Queue、PlayCircle、RemoveCircleOutline 等）。
3. 全项目 `LucideIcons.` → `TablerIcons.` 机械替换（sed 级 + import 调整）。
4. 14 个直引 Material 图标的文件改为引用 `TablerIcons`，删除 material icons import。

### 语义名 → Tabler 映射（outline，除标注 fill 外）

| 语义名 | Tabler | 备注 |
|---|---|---|
| MusicNote / MusicNoteOutlined | Music | |
| QueueMusic / PlaylistPlay / Queue | Playlist / PlaylistAdd | |
| Play / Pause | PlayerPlay / PlayerPause | 视调用点，避免圆套圆时可用 CaretRight 等 |
| PlayFill / PauseFill / SkipPreviousFill / SkipNextFill | Tabler.Filled.Player* | 沿用工作区已有改动 |
| SkipPrevious / SkipNext | PlayerSkipBack / PlayerSkipForward | |
| Repeat / RepeatOne | Repeat / RepeatOnce | |
| Shuffle | ArrowsShuffle | |
| ArrowBack / ArrowForward | ArrowLeft / ArrowRight | |
| ChevronRight / ChevronLeft | ChevronRight / ChevronLeft | |
| Close | X | |
| Delete | Trash | |
| Search | Search | |
| MoreVert / MoreHorizontal | DotsVertical / Dots | |
| Check / CheckCircle | Check / CircleCheck | |
| CheckBox / CheckBoxOutlineBlank | SquareCheck / Square | |
| Refresh | Refresh | |
| Folder / FolderOpen / File | Folder / FolderOpen / File | |
| Person / PersonOutline | User | |
| Album | Disc | |
| Mic | Mic | |
| Menu | Menu2 | 三横线 |
| Settings | Settings | |
| Info / BugReport | InfoCircle / Bug | |
| Checklist / FormatListBulleted | ListCheck / List | |
| Radio | Broadcast | 电台/广播语义，实现时按调用点定 |
| MyLocation / LocationOn | CurrentLocation / MapPin | |
| Translate | Language | |
| Share / Download / Upload | Share / Download / Upload | |
| Edit | Pencil | |
| Add / Remove | Plus / Minus | |
| Warning / Error | TriangleAlert / CircleX | |
| PlayCircle | Tabler.Filled.PlayerPlay | 圆形播放 |
| RemoveCircleOutline | CircleMinus | |

具体扩展属性名以 `icons-tabler-cmp-android:2.2.1` 实际生成结果为准，编译期兜底。

## 非目标

- 不改图标尺寸、颜色、点击行为等调用点样式。
- 不做 SVG 手写路径（Tabler 官方库已覆盖所需图标）。

## Acceptance Criteria

- [ ] 构建验证通过：`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug`（多 flavor 项目，裸 assembleDebug 无效）
- [ ] 全仓库（app/core/feature）无 `Lucide`、`androidx.compose.material.icons` 残留引用
- [ ] Lucide 依赖从 toml 与 core/ui 移除；material-icons 依赖无代码引用后移除
- [ ] 图标视觉抽查：底栏导航、播放控制（fill）、迷你播放条、设置页正常

## Notes

- 构建验证命令见 `.trellis/spec/android/index.md`（assembleMusesDebug + lintMusesDebug + testDebugUnitTest）。
