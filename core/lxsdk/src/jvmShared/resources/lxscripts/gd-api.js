/**
 * @name GD 音乐台直连接口
 * @description 由 GD 音乐台（music.gdstudio.xyz）提供接口，按所选档位请求，不在脚本内降档。
 * @version 1.0.0
 * @author Muses；接口提供者：GD 音乐台
 * @homepage https://music-api.gdstudio.xyz/api.php
 */

// 官方参数说明：https://music-api.gdstudio.xyz/api.php
// 接口许可：CC BY-NC 4.0；使用范围以接口提供方声明为准。
const { EVENT_NAMES, on, send, request } = globalThis.lx
const endpoint = 'https://music-api.gdstudio.xyz/api.php'
const platforms = { wy: 'netease', kw: 'kuwo', tx: 'tencent' }
const qualities = { '128k': 128, '192k': 192, '320k': 320, flac: 740, flac24bit: 999 }
const requestTimes = []

function query(source, type, id, extra) {
  const now = Date.now()
  while (requestTimes.length && requestTimes[0] <= now - 300000) requestTimes.shift()
  if (requestTimes.length >= 50) return Promise.reject(new Error('GD 音乐台请求频率已达上限，请稍后重试'))
  requestTimes.push(now)
  let url = endpoint + '?types=' + type + '&source=' + encodeURIComponent(source) + '&id=' + encodeURIComponent(id)
  if (extra) url += '&' + extra
  return new Promise((resolve, reject) => {
    request(url, { method: 'GET', timeout: 12000, headers: { Accept: 'application/json' } }, (error, response) => {
      if (error) return reject(new Error('GD 音乐台请求失败'))
      if (!response || response.statusCode !== 200) return reject(new Error('GD 音乐台接口暂不可用'))
      try {
        const body = typeof response.body === 'string' ? JSON.parse(response.body) : response.body
        if (!body || typeof body !== 'object' || Array.isArray(body)) throw new Error('返回结构无效')
        resolve(body)
      } catch (_) { reject(new Error('GD 音乐台返回结构无效')) }
    })
  })
}

function validUrl(url) {
  if (typeof url !== 'string' || !/^https?:\/\/[^\s]+$/.test(url)) throw new Error('GD 音乐台未返回可用地址')
  return url
}

on(EVENT_NAMES.request, async ({ source, action, info }) => {
  const platform = platforms[source]
  if (!platform) throw new Error('GD 音乐台暂不支持此平台')
  const music = info.musicInfo || {}
  // 同一平台的歌曲 ID 原样透传，不能把其他平台的搜索结果冒充原曲。
  const trackId = music.songmid || music.id || music.songId || music.mid
  if (action === 'musicUrl') {
    const br = qualities[info.type]
    if (!br) throw new Error('GD 音乐台暂不支持此音质')
    if (!trackId) throw new Error('缺少歌曲 ID')
    const result = await query(platform, 'url', trackId, 'br=' + br)
    // 请求中的 740/999 是档位编码，返回的 br 是实际码率（如普通无损返回 918）。
    // 无损及位深最终由下载文件头核验；明确返回有损码率时先让其他同档音源参与择优。
    const actual = Number(result.br)
    if (!Number.isFinite(actual) || actual <= 0 ||
        (br >= 740 ? actual <= 320 : actual < br * 0.9 || actual > br * 1.1)) {
      throw new Error('GD 音乐台未提供所选音质')
    }
    return validUrl(result.url)
  }
  if (action === 'lyric') {
    const id = music.lyric_id || trackId
    if (!id) throw new Error('缺少歌词 ID')
    const result = await query(platform, 'lyric', id)
    if (typeof result.lyric !== 'string' || !result.lyric.trim()) throw new Error('GD 音乐台未返回歌词')
    return { lyric: result.lyric, tlyric: typeof result.tlyric === 'string' ? result.tlyric : null }
  }
  if (action === 'pic') {
    if (!music.pic_id) throw new Error('缺少专辑图 ID')
    return validUrl((await query(platform, 'pic', music.pic_id, 'size=500')).url)
  }
  throw new Error('GD 音乐台暂不支持此操作')
})

const sources = {}
Object.keys(platforms).forEach(source => {
  sources[source] = { name: 'GD 音乐台', type: 'music', actions: ['musicUrl', 'lyric', 'pic'], qualitys: Object.keys(qualities) }
})
send(EVENT_NAMES.inited, { sources })
