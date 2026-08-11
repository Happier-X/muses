/** 音乐页（/tabs/music）分段状态：组件卸载后仍保持（详情页返回时也可指定）。 */

export type MusicSegment = 'songs' | 'albums' | 'artists' | 'playlists'

let currentSegment: MusicSegment = 'songs'

export const getMusicSegment = (): MusicSegment => currentSegment

export const setMusicSegment = (segment: MusicSegment): void => {
  currentSegment = segment
}
