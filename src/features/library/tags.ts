import { LocalLibraryNative } from './native'
import type { AudioFileEntry, AudioTags } from './types'
import { WebDavNative, buildWebDavUrl } from '@/features/sources/webdav'
import type { WebDavSourceItem } from '@/features/sources/types'

const normalizeTags = (tags: AudioTags): Omit<AudioTags, 'metadataDiagnostic'> => {
  const coverUri = tags.coverUri?.trim()
  return {
    title: tags.title?.trim() || undefined,
    artist: tags.artist?.trim() || undefined,
    album: tags.album?.trim() || undefined,
    duration: typeof tags.duration === 'number' && Number.isFinite(tags.duration) && tags.duration > 0 ? tags.duration : undefined,
    lyrics: tags.lyrics?.trim() || undefined,
    lyricsSource:
      tags.lyricsSource === 'embedded'
      || tags.lyricsSource === 'sidecar'
      || tags.lyricsSource === 'online'
        ? tags.lyricsSource
        : undefined,
    lyricsFormat:
      tags.lyricsFormat === 'lrc'
      || tags.lyricsFormat === 'ttml'
      || tags.lyricsFormat === 'yrc'
      || tags.lyricsFormat === 'qrc'
        ? tags.lyricsFormat
        : undefined,
    coverUri: coverUri
      && !coverUri.toLowerCase().startsWith('data:')
      && !coverUri.toLowerCase().startsWith('blob:')
      && !coverUri.includes(';base64,')
      && !coverUri.toLowerCase().startsWith('http://')
      && !coverUri.toLowerCase().startsWith('https://')
      ? coverUri
      : undefined,
    tagsScanned: tags.tagsScanned,
    tagsScannedAt: tags.tagsScannedAt,
    metadataVersion: tags.metadataVersion,
  }
}

export const readLocalAudioTags = async (file: AudioFileEntry, songId?: string): Promise<Omit<AudioTags, 'metadataDiagnostic'>> => {
  const tags = await LocalLibraryNative.readMetadata({ uri: file.uri, path: file.path, songId })
  return normalizeTags(tags)
}

export const readWebDavAudioTags = async (
  source: WebDavSourceItem,
  file: AudioFileEntry,
  password: string,
): Promise<Omit<AudioTags, 'metadataDiagnostic'>> => {
  const tags = await WebDavNative.readMetadata({
    url: buildWebDavUrl(source.serverUrl, file.path),
    username: source.username,
    password,
    songId: file.path,
  })
  return normalizeTags(tags)
}
