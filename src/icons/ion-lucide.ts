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
 * | listOutline（顺序）   | List             | 仅顺序播放模式（shuffle off） |
 * | list（队列/歌单）     | ListMusic        | 打开队列；歌单 Tab/列表占位   |
 * | repeatOutline（列表） | Repeat           | 列表循环       |
 * | repeat（单曲）        | Repeat1          | 单曲循环       |
 * | musicalNotes*         | Music            | 音乐占位/歌曲  |
 * | albums                | DiscAlbum        | 专辑           |
 * | person                | User             | 艺术家         |
 * | radio                 | Radio            | 音源           |
 * | settings              | Settings         | 设置           |
 * | searchOutline         | Search           | 搜索           |
 * | add / addOutline      | Plus             | 新增           |
 * | close                 | X                | 关闭           |
 * | trash                 | Trash2           | 删除           |
 * | chevronBack           | ChevronLeft      | 返回           |
 * | languageOutline       | Languages        | 歌词翻译       |
 * | locateOutline         | Locate           | 定位当前播放   |
 * | ellipsisVertical      | EllipsisVertical | 更多菜单       |
 * | removeCircleOutline   | CircleMinus      | 从列表移除     |
 * | playCircleOutline     | PlayCircle       | 圆形播放       |
 * | pauseCircleOutline    | PauseCircle      | 圆形暂停       |
 */
import type { IconNode } from 'lucide'
import {
  ChevronLeft,
  CircleMinus,
  DiscAlbum,
  EllipsisVertical,
  Languages,
  List,
  ListMusic,
  Locate,
  Music,
  Pause,
  PauseCircle,
  Play,
  PlayCircle,
  Plus,
  Radio,
  Repeat,
  Repeat1,
  Search,
  Settings,
  Shuffle,
  SkipBack,
  SkipForward,
  Trash2,
  User,
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
export const playCircleOutline = lucideToIonIcon(PlayCircle)
export const pauseCircleOutline = lucideToIonIcon(PauseCircle)

// —— 播放模式（状态切换必须用不同图标）——
/** 随机播放 */
export const shuffle = lucideToIonIcon(Shuffle)
/** 仅顺序播放模式（shuffle off）；勿用于队列/歌单 */
export const listOutline = lucideToIonIcon(List)
/** 打开队列；歌单 Tab / 列表占位 */
export const list = lucideToIonIcon(ListMusic)
/** 列表循环 */
export const repeatOutline = lucideToIonIcon(Repeat)
/** 单曲循环 */
export const repeat = lucideToIonIcon(Repeat1)

// —— 导航 / 占位 ——
export const musicalNotes = lucideToIonIcon(Music)
export const musicalNotesOutline = musicalNotes
export const albums = lucideToIonIcon(DiscAlbum)
export const person = lucideToIonIcon(User)
export const radio = lucideToIonIcon(Radio)
export const settings = lucideToIonIcon(Settings)

// —— 通用操作 ——
export const searchOutline = lucideToIonIcon(Search)
export const add = lucideToIonIcon(Plus)
export const addOutline = add
export const close = lucideToIonIcon(X)
export const trash = lucideToIonIcon(Trash2)
export const chevronBack = lucideToIonIcon(ChevronLeft)
export const languageOutline = lucideToIonIcon(Languages)
export const locateOutline = lucideToIonIcon(Locate)
export const ellipsisVertical = lucideToIonIcon(EllipsisVertical)
export const removeCircleOutline = lucideToIonIcon(CircleMinus)
