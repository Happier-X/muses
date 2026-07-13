/**
 * 酷我封面：移植自 any-listen-extension-online-metadata（Apache-2.0）的
 * musicSearch + getPic 最小链路，宿主 request 替换为 httpGetText。
 * 来源：https://github.com/any-listen/any-listen-extension-online-metadata
 */
import { httpGetText } from '../http'
import type { CoverProvider, OnlineCoverQuery } from '../types'

type KwAbsItem = {
  MUSICRID?: string
  SONGNAME?: string
  ARTIST?: string
  ALBUM?: string
  N_MINFO?: string
}

type KwSearchResult = {
  abslist?: KwAbsItem[]
}

const buildSearchUrl = (keyword: string, page = 1, limit = 10): string => {
  const pn = page - 1
  return (
    `https://search.kuwo.cn/r.s?client=kt&all=${encodeURIComponent(keyword)}` +
    `&pn=${pn}&rn=${limit}&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1` +
    '&show_copyright_off=1&newver=1&ft=music&cluster=0&strategy=2012' +
    '&encoding=utf8&rformat=json&vermerge=1&mobi=1&issubtitle=1'
  )
}

const parseKwSearch = (raw: string): KwAbsItem[] => {
  try {
    const body = JSON.parse(raw) as KwSearchResult
    return Array.isArray(body.abslist) ? body.abslist : []
  } catch {
    return []
  }
}

const extractMusicId = (item: KwAbsItem): string | null => {
  const rid = item.MUSICRID?.trim()
  if (!rid) {
    return null
  }
  return rid.replace(/^MUSIC_/i, '') || null
}

const normalizePicUrl = (body: string): string | null => {
  const trimmed = body.trim()
  if (!/^https?:\/\//i.test(trimmed)) {
    return null
  }
  let url = trimmed
  // 扩展侧：kwcdn → kuwo.cn 并升 https
  if (url.startsWith('http://') && url.includes('.kwcdn.kuwo.cn')) {
    url = url.replace('.kwcdn.kuwo.cn', '.kuwo.cn').replace('http://', 'https://')
  } else if (url.startsWith('http://')) {
    url = url.replace('http://', 'https://')
  }
  return url
}

export const searchKwCoverUrl = async (query: OnlineCoverQuery): Promise<string | null> => {
  const keyword = [query.title, query.artist, query.album].filter((part) => part?.trim()).join(' ').trim()
  if (!keyword) {
    return null
  }

  const searchRaw = await httpGetText(buildSearchUrl(keyword), {
    'User-Agent': 'Mozilla/5.0',
    Accept: 'application/json,text/plain,*/*',
  })
  const list = parseKwSearch(searchRaw)
  const firstWithId = list.find((item) => extractMusicId(item))
  const musicId = firstWithId ? extractMusicId(firstWithId) : null
  if (!musicId) {
    return null
  }

  const picRaw = await httpGetText(
    `https://artistpicserver.kuwo.cn/pic.web?corp=kuwo&type=rid_pic&pictype=500&size=500&rid=${encodeURIComponent(musicId)}`,
    {
      'User-Agent': 'Mozilla/5.0',
      Accept: 'text/plain,*/*',
    },
  )
  return normalizePicUrl(picRaw)
}

export const kwCoverProvider: CoverProvider = {
  id: 'kw',
  searchCoverUrl: searchKwCoverUrl,
}
