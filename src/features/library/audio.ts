const SUPPORTED_AUDIO_EXTENSIONS = new Set([
  'aac',
  'aiff',
  'alac',
  'ape',
  'flac',
  'm4a',
  'm4b',
  'mp3',
  'ogg',
  'opus',
  'wav',
  'wma',
])

export const getFileNameFromPath = (path: string): string => {
  return path.split(/[\\/]/).filter(Boolean).at(-1) ?? path
}

export const getTitleFromPath = (path: string): string => {
  const fileName = getFileNameFromPath(path)
  const extensionIndex = fileName.lastIndexOf('.')
  return extensionIndex > 0 ? fileName.slice(0, extensionIndex) : fileName
}

export const isSupportedAudioFile = (path: string): boolean => {
  const fileName = getFileNameFromPath(path).trim()
  const extensionIndex = fileName.lastIndexOf('.')
  if (extensionIndex < 0 || extensionIndex === fileName.length - 1) {
    return false
  }

  return SUPPORTED_AUDIO_EXTENSIONS.has(fileName.slice(extensionIndex + 1).toLowerCase())
}
