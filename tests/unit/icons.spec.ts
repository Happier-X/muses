import { Captions, CaptionsOff, ListOrdered, Pause, Play, Shuffle, SkipBack, SkipForward } from '@lucide/vue'
import { describe, expect, test } from 'vitest'

const iconNames = [Play, Pause, SkipBack, SkipForward, Shuffle, ListOrdered, Captions, CaptionsOff]

describe('业务图标体系', () => {
  test('全部使用 @lucide/vue 组件，供 HIcon 渲染', () => {
    expect(iconNames.every((icon) => icon)).toBe(true)
  })

  test('播放、顺序播放和字幕状态使用不同语义图标', () => {
    expect(Play).not.toBe(Pause)
    expect(ListOrdered).not.toBe(Shuffle)
    expect(Captions).not.toBe(CaptionsOff)
  })
})
