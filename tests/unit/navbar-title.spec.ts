import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, test } from 'vitest'

const themeCss = readFileSync(resolve(process.cwd(), 'src/theme/variables.css'), 'utf8')

const readSource = (file: string): string => readFileSync(resolve(process.cwd(), file), 'utf8')

const navbarCases = [
  ['src/views/SongsPage.vue', '<ion-title>歌曲</ion-title>'],
  ['src/views/AlbumsPage.vue', '<ion-title>专辑</ion-title>'],
  ['src/views/ArtistsPage.vue', '<ion-title>艺术家</ion-title>'],
  ['src/views/PlaylistsPage.vue', '<ion-title>歌单</ion-title>'],
  ['src/views/SettingsPage.vue', '<ion-title>设置</ion-title>'],
  ['src/views/PlaylistDetailPage.vue', "<ion-title>{{ playlist?.name ?? '歌单' }}</ion-title>"],
  ['src/views/QueuePage.vue', '<ion-title>播放队列</ion-title>'],
  ['src/views/SourcesPage.vue', '<ion-title>音源</ion-title>'],
  ['src/views/SourcesPage.vue', '<ion-title>扫描设置</ion-title>'],
  ['src/views/SourcesPage.vue', '<ion-title>扫描进度</ion-title>'],
  ['src/views/SourcesPage.vue', '<ion-title>添加 WebDAV</ion-title>'],
] as const

const collapsibleTitleFiles = [
  'src/views/SongsPage.vue',
  'src/views/AlbumsPage.vue',
  'src/views/ArtistsPage.vue',
  'src/views/PlaylistsPage.vue',
  'src/views/SettingsPage.vue',
  'src/views/PlaylistDetailPage.vue',
]

const ruleBody = (selector: string): string => {
  const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = themeCss.match(new RegExp(`${escapedSelector}\\s*\\{([^}]*)\\}`))
  expect(match, `缺少全局样式规则：${selector}`).not.toBeNull()
  return match?.[1] ?? ''
}

describe('navbar 标题全局布局', () => {
  test('普通标题相对完整 toolbar 绝对居中，并明确排除折叠大标题', () => {
    const selector = 'ion-header ion-toolbar > ion-title:not([size="large"])'
    const declarations = ruleBody(selector)

    expect(declarations).toMatch(/position:\s*absolute/)
    expect(declarations).toMatch(/inset:\s*0/)
    expect(declarations).toMatch(/text-align:\s*center/)
    expect(selector).toContain(':not([size="large"])')
  })

  test('为长标题保留对称安全空间，且标题层不拦截按钮事件', () => {
    const declarations = ruleBody('ion-header ion-toolbar > ion-title:not([size="large"])')

    expect(declarations).toMatch(/padding-inline:\s*80px/)
    expect(declarations).toMatch(/pointer-events:\s*none/)
  })

  test('左右操作按钮位于标题层上方并保持可交互', () => {
    const declarations = ruleBody('ion-header ion-toolbar > ion-buttons')

    expect(declarations).toMatch(/position:\s*relative/)
    expect(declarations).toMatch(/z-index:\s*1/)
    expect(declarations).not.toMatch(/pointer-events:\s*none/)
  })

  test.each(navbarCases)('%s 的普通标题由全局规则接管：%s', (file, titleMarkup) => {
    const source = readSource(file)

    expect(source).toContain(titleMarkup)
    expect(titleMarkup).not.toContain('size="large"')
  })

  test.each(collapsibleTitleFiles)('%s 的折叠大标题保留 size="large"', (file) => {
    expect(readSource(file)).toMatch(/<ion-title\s+size="large">/)
  })

  test('页面不再使用仅用于 navbar 居中的局部 class', () => {
    const files = [...new Set(navbarCases.map(([file]) => file))]
    const sources = files.map(readSource).join('\n')

    expect(sources).not.toMatch(/(?:page-title|source-title)/)
  })
})
