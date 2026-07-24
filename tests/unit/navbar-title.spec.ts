import { readFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, test } from 'vitest'

const readSource = (file: string): string => readFileSync(resolve(process.cwd(), file), 'utf8')

const listVueSources = (directory: string): string[] => readdirSync(resolve(process.cwd(), directory), { withFileTypes: true })
  .flatMap((entry) => {
    const path = `${directory}/${entry.name}`
    if (entry.isDirectory()) {
      return listVueSources(path)
    }
    return entry.isFile() && entry.name.endsWith('.vue') ? [path] : []
  })

const pageNavbarCases = [
  ['src/views/SongsPage.vue', 'title="歌曲"'],
  ['src/views/PlaylistsPage.vue', 'title="歌单"'],
  ['src/views/SettingsPage.vue', 'title="设置"'],
  ['src/views/PlaylistDetailPage.vue', ':title="playlist?.name ?? \'歌单\'"'],
  ['src/views/QueuePage.vue', 'title="播放队列"'],
  ['src/views/SourcesPage.vue', 'title="音源"'],
] as const

const modalNavbarCases = [
  '编辑音源',
  '扫描设置',
  '扫描进度',
  '添加 WebDAV',
] as const

const directNavbarFiles = [...new Set(pageNavbarCases.map(([file]) => file))]
const allNavbarFiles = [
  'src/components/ui/MPage.vue',
  ...directNavbarFiles,
]
const allVueSources = listVueSources('src')

const ionicNavbarPattern = /<ion-(?:header|toolbar|title|buttons|back-button)\b/

describe('HNavBar 使用契约', () => {
  test.each(pageNavbarCases)('%s 使用 HNavBar 承载标题', (file, titleBinding) => {
    const source = readSource(file)

    expect(source).toContain('<h-nav-bar')
    expect(source).toContain(titleBinding)
    expect(source).toContain(':fixed="false"')
  })

  test('MPage 将标题和左右操作映射到 HNavBar 插槽', () => {
    const source = readSource('src/components/ui/MPage.vue')

    expect(source).toContain('<h-nav-bar :fixed="false">')
    expect(source).toContain('<template v-if="$slots.start" #left>')
    expect(source).toContain('<template #title>')
    expect(source).toContain('<template v-if="$slots.end" #right>')
    expect(source).toContain("import { HNavBar } from 'happier-ui'")
    expect(source).not.toContain("from '@/components/ui'")
    expect(source).not.toMatch(/condensedTitle|collapse="condense"/)
  })

  test.each([
    ['src/views/AlbumsPage.vue', '专辑'],
    ['src/views/ArtistsPage.vue', '艺术家'],
  ] as const)('%s 通过 MPage 的标题插槽使用 HNavBar：%s', (file, title) => {
    const source = readSource(file)

    expect(source).toMatch(/<m-page[\s>]/)
    expect(source).toContain(`<template #title>${title}</template>`)
    expect(source).not.toMatch(ionicNavbarPattern)
  })

  test.each(modalNavbarCases)('SourcesPage 的“%s”弹窗使用非固定、无安全区 HNavBar', (title) => {
    const source = readSource('src/views/SourcesPage.vue')

    expect(source).toContain(`<h-nav-bar title="${title}" :fixed="false" :safe-area="false">`)
  })

  test('返回页面使用 HNavBar 返回契约', () => {
    const playlistDetail = readSource('src/views/PlaylistDetailPage.vue')
    const queue = readSource('src/views/QueuePage.vue')

    for (const source of [playlistDetail, queue]) {
      expect(source).toContain('show-back')
      expect(source).toContain('@handle-left-click="goBack"')
    }
    expect(playlistDetail).toContain("ionRouter.navigate('/tabs/playlists', 'back', 'pop')")
  })

  test.each(allNavbarFiles)('%s 不再使用 Ionic navbar 标签', (file) => {
    expect(readSource(file)).not.toMatch(ionicNavbarPattern)
  })

  test.each(allVueSources)('%s 不再直接声明 Ionic navbar 标签', (file) => {
    expect(readSource(file)).not.toMatch(ionicNavbarPattern)
  })

  test.each(directNavbarFiles)('%s 的右侧业务按钮不依赖 HNavBar 容器点击', (file) => {
    expect(readSource(file)).not.toMatch(/@handle-right-click=/)
  })

  test('全局主题不再包含 Ionic navbar 专属样式', () => {
    const themeCss = readSource('src/theme/variables.css')

    expect(themeCss).not.toMatch(/ion-header|ion-title|ion-buttons/)
  })
})
