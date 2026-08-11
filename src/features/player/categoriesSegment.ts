/** 分类页（/tabs/categories）分段状态：组件卸载后仍保持（详情页返回时也可指定）。 */

export type CategoriesSegment = 'albums' | 'artists' | 'playlists'

let currentSegment: CategoriesSegment = 'albums'

export const getCategoriesSegment = (): CategoriesSegment => currentSegment

export const setCategoriesSegment = (segment: CategoriesSegment): void => {
  currentSegment = segment
}
