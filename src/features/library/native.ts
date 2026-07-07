import { registerPlugin } from '@capacitor/core'
import type { AudioFileEntry, NativeAudioMetadata } from './types'

interface LocalLibraryNativePlugin {
  scanDirectory(options: { treeUri: string }): Promise<{ files: AudioFileEntry[] }>
  readMetadata(options: { uri: string }): Promise<NativeAudioMetadata>
}

// 本地音源的平台能力边界：封装 Android SAF 树 URI 的递归枚举与元数据读取，未来可替换为成熟插件而不影响扫描器。
export const LocalLibraryNative = registerPlugin<LocalLibraryNativePlugin>('LocalLibrary')

export const scanLocalAudioFiles = async (treeUri: string): Promise<AudioFileEntry[]> => {
  const result = await LocalLibraryNative.scanDirectory({ treeUri })
  return Array.isArray(result.files) ? result.files : []
}
