import { registerPlugin } from '@capacitor/core'
import type { PluginListenerHandle } from '@capacitor/core'
import type { AudioPlayerNativeState, PlayOptions } from './types'

export interface AudioPlayerNativePlugin {
  play(options: PlayOptions): Promise<void>
  pause(): Promise<void>
  resume(): Promise<void>
  stop(): Promise<void>
  getState(): Promise<AudioPlayerNativeState>
  addListener(
    eventName: 'stateChange',
    listenerFunc: (state: AudioPlayerNativeState) => void,
  ): Promise<PluginListenerHandle>
}

export const AudioPlayerNative = registerPlugin<AudioPlayerNativePlugin>('AudioPlayer')
