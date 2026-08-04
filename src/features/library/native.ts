import { registerPlugin } from '@capacitor/core'
import type { AudioFileEntry, NativeAudioMetadata } from './types'

/** 写标签结果：ok=true 成功；失败带 code 便于 D4 文案，不抛异常 */
export interface WriteMetadataResult {
  ok: boolean
  code?: string
  message?: string
}

export interface WriteMetadataOptions {
  uri: string
  title?: string
  artist?: string
  album?: string
  lyrics?: string
  clearLyrics?: boolean
  /** 本地封面 file:// 或绝对路径 */
  coverPath?: string
  clearCover?: boolean
  replayGainTrackDb?: number
  clearReplayGain?: boolean
}

interface LocalLibraryNativePlugin {
  scanDirectory(options: { treeUri: string }): Promise<{ files: AudioFileEntry[] }>
  readMetadata(options: { uri: string; path?: string; songId?: string }): Promise<NativeAudioMetadata>
  writeMetadata?(options: WriteMetadataOptions): Promise<WriteMetadataResult>
  /** 选图 base64 → cache/covers 安全 file:// */
  cacheCoverBytes?(options: { cacheKey: string; base64Data: string }): Promise<{ uri: string | null }>
}

// 本地音源的平台能力边界：封装 Android SAF 树 URI 的递归枚举与元数据读写，未来可替换为成熟插件而不影响扫描器。
export const LocalLibraryNative = registerPlugin<LocalLibraryNativePlugin>('LocalLibrary')

export const scanLocalAudioFiles = async (treeUri: string): Promise<AudioFileEntry[]> => {
  const result = await LocalLibraryNative.scanDirectory({ treeUri })
  return Array.isArray(result.files) ? result.files : []
}

export const writeLocalAudioMetadata = async (
  options: WriteMetadataOptions,
): Promise<WriteMetadataResult> => {
  if (!LocalLibraryNative.writeMetadata) {
    return { ok: false, code: 'not_implemented', message: '当前环境不支持写入本地标签。' }
  }
  try {
    const result = await LocalLibraryNative.writeMetadata(options)
    if (result?.ok) {
      return { ok: true }
    }
    return {
      ok: false,
      code: result?.code || 'write_failed',
      message: result?.message || '写入本地标签失败。',
    }
  } catch (error) {
    return {
      ok: false,
      code: 'write_failed',
      message: error instanceof Error ? error.message : '写入本地标签失败。',
    }
  }
}

/** 将选图字节落到 covers 缓存；禁止 data/http 入曲库。 */
export const cacheCoverBytes = async (options: {
  cacheKey: string
  base64Data: string
}): Promise<string | null> => {
  if (!LocalLibraryNative.cacheCoverBytes) {
    return null
  }
  try {
    const result = await LocalLibraryNative.cacheCoverBytes(options)
    const uri = result?.uri?.trim()
    if (!uri) {
      return null
    }
    const normalized = uri.toLowerCase()
    if (
      normalized.startsWith('data:')
      || normalized.startsWith('blob:')
      || normalized.includes(';base64,')
      || normalized.startsWith('http://')
      || normalized.startsWith('https://')
    ) {
      return null
    }
    return uri
  } catch {
    return null
  }
}
