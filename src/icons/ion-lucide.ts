/**
 * Lucide → ion-icon 适配层。
 *
 * 将 Lucide IconNode 转为与 `ionicons/icons` 兼容的 data-URI SVG 字符串，
 * 业务侧继续把结果传给 `<ion-icon :icon="..." />`，禁止再从 `ionicons/icons` 导入。
 *
 * ionicons → Lucide 对照（语义等价，状态图标必须可区分）：
 * | ionicons              | Lucide           | 用途           |
 * |-----------------------|------------------|----------------|
 * | play（fill）          | Play             | 播放主控       |
 * | playOutline           | Play             | 列表/次级播放  |
 * | pause（fill）         | Pause            | 暂停主控       |
 * | playSkipBack（fill）  | SkipBack         | 上一曲主控     |
 * | playSkipForward（fill）| SkipForward     | 下一曲主控     |
 * | shuffle               | Shuffle          | 随机播放       |
 * | listOutline（顺序）   | ListOrdered      | 仅顺序播放模式（shuffle off） |
 * | list（队列/歌单）     | ListMusic        | 打开队列；歌单 Tab/列表占位   |
 * | repeatOutline（列表） | Repeat           | 列表循环       |
 * | repeat（单曲）        | Repeat1          | 单曲循环       |
 * | musicalNotes*         | Music            | 音乐占位/歌曲  |
 * | albums                | Disc3            | 专辑           |
 * | person                | MicVocal         | 艺术家         |
 * | radio                 | Folder           | 音源           |
 * | settings              | Settings         | 设置           |
 * | searchOutline         | Search           | 搜索           |
 * | add / addOutline      | Plus             | 新增           |
 * | close                 | X                | 关闭           |
 * | trash                 | Trash2           | 删除           |
 * | chevronBack           | ChevronLeft      | 返回           |
 * | languageOutline       | Captions         | 歌词翻译开     |
 * | languageOffOutline    | CaptionsOff      | 歌词翻译关     |
 * | locateOutline         | Locate           | 定位当前播放   |
 * | ellipsisVertical      | EllipsisVertical | 更多菜单       |
 * | removeCircleOutline   | CircleMinus      | 从列表移除     |
 * | play / pause（fill）  | Play / Pause     | 播放主控与歌词页浮动播放键 |
 */
import type { IconNode } from 'lucide'
import {
  Captions,
  CaptionsOff,
  ChevronLeft,
  CircleMinus,
  Disc3,
  EllipsisVertical,
  Folder,
  ListMusic,
  ListOrdered,
  Locate,
  MicVocal,
  Music,
  Pause,
  Play,
  Plus,
  Repeat,
  Repeat1,
  Search,
  Settings,
  Shuffle,
  SkipBack,
  SkipForward,
  Trash2,
  X,
} from 'lucide'

/** 图标变体：outline 线框（默认），fill 实心（播放主控）。 */
export type LucideIonIconVariant = 'outline' | 'fill'

export type LucideToIonIconOptions = {
  variant?: LucideIonIconVariant
}

/** Lucide 默认 SVG 根属性（outline 24×24，stroke 继承 currentColor）。 */
const SVG_ROOT_ATTRS_OUTLINE: Record<string, string | number> = {
  xmlns: 'http://www.w3.org/2000/svg',
  width: 24,
  height: 24,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  'stroke-width': 2,
  'stroke-linecap': 'round',
  'stroke-linejoin': 'round',
}

/**
 * fill 变体：实心填充。
 * 保留同色 stroke，避免 SkipBack/SkipForward 的竖线 path（无面积）在 stroke:none 时消失。
 */
const SVG_ROOT_ATTRS_FILL: Record<string, string | number> = {
  xmlns: 'http://www.w3.org/2000/svg',
  width: 24,
  height: 24,
  viewBox: '0 0 24 24',
  fill: 'currentColor',
  stroke: 'currentColor',
  'stroke-width': 2,
  'stroke-linecap': 'round',
  'stroke-linejoin': 'round',
}

const escapeAttr = (value: string | number): string =>
  String(value).replace(/&/g, '&amp;').replace(/'/g, '&apos;').replace(/"/g, '&quot;')

const attrsToString = (attrs: Record<string, string | number | undefined>): string =>
  Object.entries(attrs)
    .filter((entry): entry is [string, string | number] => entry[1] !== undefined)
    .map(([key, value]) => `${key}='${escapeAttr(value)}'`)
    .join(' ')

/**
 * 将 Lucide IconNode 转为 ion-icon 可直接使用的 data-URI SVG。
 * 格式与 ionicons/icons 一致：`data:image/svg+xml;utf8,<svg...>`
 *
 * @param options.variant outline（默认线框）| fill（实心，用于播放主控）
 */
export const lucideToIonIcon = (
  iconNode: IconNode,
  options: LucideToIonIconOptions = {},
): string => {
  const variant = options.variant ?? 'outline'
  const rootAttrs = variant === 'fill' ? SVG_ROOT_ATTRS_FILL : SVG_ROOT_ATTRS_OUTLINE
  const children = iconNode
    .map(([tag, childAttrs]) => `<${tag} ${attrsToString(childAttrs)}/>`)
    .join('')
  const svg = `<svg ${attrsToString(rootAttrs)}>${children}</svg>`
  return `data:image/svg+xml;utf8,${svg}`
}

// —— 播放控制（主控 fill；次级入口 outline）——
/** 播放主控（MiniPlayer / PlayerPage）——实心 */
export const play = lucideToIonIcon(Play, { variant: 'fill' })
/** 列表/次级「播放」入口——线框，与主控 play 解耦 */
export const playOutline = lucideToIonIcon(Play, { variant: 'outline' })
/** 暂停主控——实心 */
export const pause = lucideToIonIcon(Pause, { variant: 'fill' })
/** 上一曲主控——实心 */
export const playSkipBack = lucideToIonIcon(SkipBack, { variant: 'fill' })
/** 下一曲主控——实心 */
export const playSkipForward = lucideToIonIcon(SkipForward, { variant: 'fill' })

// —— 播放模式（状态切换必须用不同图标）——
/** 随机播放 */
export const shuffle = lucideToIonIcon(Shuffle)
/** 仅顺序播放模式（shuffle off，Lucide ListOrdered）；勿用于队列/歌单 */
export const listOutline = lucideToIonIcon(ListOrdered)
/** 打开队列；歌单 Tab / 列表占位 */
export const list = lucideToIonIcon(ListMusic)
/** 列表循环 */
export const repeatOutline = lucideToIonIcon(Repeat)
/** 单曲循环 */
export const repeat = lucideToIonIcon(Repeat1)

// —— 导航 / 占位 ——
export const musicalNotes = lucideToIonIcon(Music)
export const musicalNotesOutline = musicalNotes
/** 专辑 Tab / 专辑列表 */
export const albums = lucideToIonIcon(Disc3)
/** 艺术家 Tab / 艺术家列表 */
export const person = lucideToIonIcon(MicVocal)
/** 音源 Tab / 音源入口（文件夹语义，非电台） */
export const radio = lucideToIonIcon(Folder)
export const settings = lucideToIonIcon(Settings)

// —— 通用操作 ——
export const searchOutline = lucideToIonIcon(Search)
export const add = lucideToIonIcon(Plus)
export const addOutline = add
export const close = lucideToIonIcon(X)
export const trash = lucideToIonIcon(Trash2)
export const chevronBack = lucideToIonIcon(ChevronLeft)
/** 歌词翻译开（显示译文）；与 languageOffOutline 同族，仅差开/关标记 */
export const languageOutline = lucideToIonIcon(Captions)
/** 歌词翻译关（隐藏译文）；须与 languageOutline 可区分，禁止改用无关几何 */
export const languageOffOutline = lucideToIonIcon(CaptionsOff)
export const locateOutline = lucideToIonIcon(Locate)
export const ellipsisVertical = lucideToIonIcon(EllipsisVertical)
export const removeCircleOutline = lucideToIonIcon(CircleMinus)
