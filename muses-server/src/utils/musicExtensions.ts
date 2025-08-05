export const MUSIC_EXTENSIONS = [
  '.mp3',
  '.flac',
] as const;

export const MIME_TYPE_MAP: Record<string, string> = {
  '.mp3': 'audio/mpeg',
  '.flac': 'audio/flac',
};