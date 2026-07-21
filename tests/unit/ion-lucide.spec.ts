import { describe, expect, test } from 'vitest'
import {
  pause,
  play,
  playOutline,
  playSkipBack,
  playSkipForward,
  shuffle,
} from '@/icons/ion-lucide'

const decodeIonIconSvg = (dataUri: string): string => {
  expect(dataUri.startsWith('data:image/svg+xml;utf8,')).toBe(true)
  return dataUri.slice('data:image/svg+xml;utf8,'.length)
}

describe('ion-lucide 适配层', () => {
  test('播放主控导出为 fill（实心）', () => {
    for (const icon of [play, pause, playSkipBack, playSkipForward]) {
      const svg = decodeIonIconSvg(icon)
      expect(svg).toContain("fill='currentColor'")
      expect(svg).not.toContain("fill='none'")
      // 保留同色 stroke，确保 Skip 竖线等无面积 path 仍可见
      expect(svg).toContain("stroke='currentColor'")
    }
  })

  test('playOutline 与次级图标保持 outline', () => {
    for (const icon of [playOutline, shuffle]) {
      const svg = decodeIonIconSvg(icon)
      expect(svg).toContain("fill='none'")
      expect(svg).toContain("stroke='currentColor'")
    }
  })

  test('play 与 playOutline 导出内容不同（fill / outline 解耦）', () => {
    expect(play).not.toBe(playOutline)
  })
})
