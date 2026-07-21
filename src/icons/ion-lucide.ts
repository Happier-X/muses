/**
 * Lucide → ion-icon 适配层。
 *
 * 将 Lucide IconNode 转为与 `ionicons/icons` 兼容的 data-URI SVG 字符串，
 * 业务侧继续把结果传给 `<ion-icon :icon="..." />`，禁止再从 `ionicons/icons` 导入。
 *
 * ionicons → Lucide 对照（语义等价，状态图标必须可区分）：
 * | ionicons              | Lucide           | 用途           |
 * |-----------------------|------------------|----------------|
 * | play / playOutline    | Play             | 播放           |
 * | pause                 | Pause            | 暂停           |
 * | playSkipBack          | SkipBack         | 上一曲         |
 * | playSkipForward       | SkipForward      | 下一曲         |
 * | shuffle               | Shuffle          | 随机播放       |
 * | listOutline（顺序）   | List             | 顺序播放模式   |
 * | list（队列/歌单）     | ListMusic        | 队列/歌单导航  |
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

/** Lucide 默认 SVG 根属性（outline 24×24，stroke 继承 currentColor）。 */
const SVG_ROOT_ATTRS: Record<string, string | number> = {
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
 */
export const lucideToIonIcon = (iconNode: IconNode): string => {
  const children = iconNode
    .map(([tag, childAttrs]) => `<${tag} ${attrsToString(childAttrs)}/>`)
    .join('')
  const svg = `<svg ${attrsToString(SVG_ROOT_ATTRS)}>${children}</svg>`
  return `data:image/svg+xml;utf8,${svg}`
}

// —— 播放控制 ——
export const play = lucideToIonIcon(Play)
export const playOutline = play
export const pause = lucideToIonIcon(Pause)
export const playSkipBack = lucideToIonIcon(SkipBack)
export const playSkipForward = lucideToIonIcon(SkipForward)
export const playCircleOutline = lucideToIonIcon(PlayCircle)
export const pauseCircleOutline = lucideToIonIcon(PauseCircle)

// —— 播放模式（状态切换必须用不同图标）——
/** 随机播放 */
export const shuffle = lucideToIonIcon(Shuffle)
/** 顺序播放 / 通用列表线框 */
export const listOutline = lucideToIonIcon(List)
/** 队列、歌单导航 */
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
