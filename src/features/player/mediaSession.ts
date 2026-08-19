/**
 * MediaSession 状态同步
 *
 * 现在由 PlaybackService 中的 ExoPlayer MediaSession 自动管理。
 * 这些函数保留为空实现，以保持 controller.ts 的接口兼容。
 */

type ActionHandler = () => Promise<void> | void

type SeekHandler = (seconds: number) => Promise<boolean | void> | boolean | void

/**
 * 设置 MediaSession 操作处理器
 * 现在由 ExoPlayer MediaSession 自动处理
 */
export const setupMediaSessionActions = async (_handlers: {
  play?: ActionHandler
  pause?: ActionHandler
  stop?: ActionHandler
  previoustrack?: ActionHandler
  nexttrack?: ActionHandler
  seekto?: SeekHandler
}): Promise<void> => {
  // ExoPlayer MediaSession 自动处理媒体按钮事件
}

/**
 * 更新 MediaSession 元数据
 * 现在由 ExoPlayer MediaSession 自动同步
 */
export const updateMediaSessionMetadata = async (_metadata: {
  title?: string
  artist?: string
  album?: string
  coverUri?: string | null
}): Promise<void> => {
  // ExoPlayer MediaSession 自动同步元数据
}

/**
 * 更新 MediaSession 播放状态
 * 现在由 ExoPlayer MediaSession 自动同步
 */
export const updateMediaSessionPlayback = async (_status: string): Promise<void> => {
  // ExoPlayer MediaSession 自动同步播放状态
}

/**
 * 更新 MediaSession 位置状态
 * 现在由 ExoPlayer MediaSession 自动同步
 */
export const updateMediaSessionPosition = async (
  _position: number,
  _duration: number,
): Promise<void> => {
  // ExoPlayer MediaSession 自动同步位置
}

/**
 * 清除 MediaSession
 * 现在由 ExoPlayer MediaSession 自动管理
 */
export const clearMediaSession = async (): Promise<void> => {
  // ExoPlayer MediaSession 自动管理生命周期
}
