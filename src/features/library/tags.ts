import { LocalLibraryNative } from './native'
import type { AudioFileEntry, AudioTags } from './types'
import { WebDavNative, buildWebDavUrl } from '@/features/sources/webdav'
import type { WebDavSourceItem } from '@/features/sources/types'

const normalizeTags = (tags: AudioTags): AudioTags => {
  return {
    title: tags.title?.trim() || undefined,
    artist: tags.artist?.trim() || undefined,
    album: tags.album?.trim() || undefined,
    duration: typeof tags.duration === 'number' && Number.isFinite(tags.duration) && tags.duration > 0 ? tags.duration : undefined,
  }
}

export const readLocalAudioTags = async (file: AudioFileEntry): Promise<AudioTags> => {
  const tags = await LocalLibraryNative.readMetadata({ uri: file.uri })
  return normalizeTags(tags)
}

export const readWebDavAudioTags = async (
  source: WebDavSourceItem,
  file: AudioFileEntry,
  password: string,
): Promise<AudioTags> => {
  const tags = await WebDavNative.readMetadata({
    url: buildWebDavUrl(source.serverUrl, file.path),
    username: source.username,
    password,
  })
  return normalizeTags(tags)
}
