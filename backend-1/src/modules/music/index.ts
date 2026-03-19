import { Elysia, t } from 'elysia'
import { musicService } from './service'

// 歌曲响应模型
const SongResponse = t.Object({
  id: t.String(),
  title: t.String(),
  artist: t.Optional(t.String()),
  album: t.Optional(t.String()),
  albumArtist: t.Optional(t.String()),
  year: t.Optional(t.Number()),
  track: t.Optional(t.Number()),
  genre: t.Optional(t.String()),
  duration: t.Optional(t.Number()),
  bitrate: t.Optional(t.Number()),
  sampleRate: t.Optional(t.Number()),
  format: t.String(),
  filePath: t.String(),
  fileSize: t.Optional(t.Number()),
  playCount: t.Number(),
  createdAt: t.String(),
  updatedAt: t.String()
})

const ScanResultResponse = t.Object({
  added: t.Number(),
  updated: t.Number(),
  skipped: t.Number(),
  errors: t.Array(t.String())
})

const ErrorResponse = t.Object({
  error: t.String()
})

export const music = new Elysia({ prefix: '/music' })
  // 扫描音乐目录
  .post('/scan', async ({ set }) => {
    const musicPath = process.env.MUSIC_DIR

    if (!musicPath) {
      set.status = 400
      return { error: '请在 .env 文件中配置 MUSIC_DIR 环境变量' }
    }

    try {
      const result = await musicService.scanDirectory(musicPath)
      return result
    } catch (error) {
      set.status = 500
      return { error: `扫描失败: ${error}` }
    }
  }, {
    response: {
      200: ScanResultResponse,
      400: ErrorResponse,
      500: ErrorResponse
    }
  })
  // 获取所有歌曲
  .get('/', async () => {
    const songs = await musicService.getAllSongs()
    return songs.map(song => ({
      ...song,
      createdAt: song.createdAt.toISOString(),
      updatedAt: song.updatedAt.toISOString()
    }))
  }, {
    response: {
      200: t.Array(SongResponse)
    }
  })
  // 获取歌曲详情
  .get('/:id', async ({ params: { id }, set }) => {
    const song = await musicService.getSongById(id)

    if (!song) {
      set.status = 404
      return { error: '歌曲不存在' }
    }

    return {
      ...song,
      createdAt: song.createdAt.toISOString(),
      updatedAt: song.updatedAt.toISOString()
    }
  }, {
    params: t.Object({
      id: t.String()
    }),
    response: {
      200: SongResponse,
      404: ErrorResponse
    }
  })
  // 搜索歌曲
  .get('/search/:query', async ({ params: { query } }) => {
    const songs = await musicService.searchSongs(query)
    return songs.map(song => ({
      ...song,
      createdAt: song.createdAt.toISOString(),
      updatedAt: song.updatedAt.toISOString()
    }))
  }, {
    params: t.Object({
      query: t.String()
    }),
    response: {
      200: t.Array(SongResponse)
    }
  })
  // 删除歌曲
  .delete('/:id', async ({ params: { id }, set }) => {
    try {
      await musicService.deleteSong(id)
      return { success: true }
    } catch (error) {
      set.status = 404
      return { error: '歌曲不存在' }
    }
  }, {
    params: t.Object({
      id: t.String()
    }),
    response: {
      200: t.Object({ success: t.Boolean() }),
      404: ErrorResponse
    }
  })
  // 增加播放次数
  .post('/:id/play', async ({ params: { id }, set }) => {
    try {
      const song = await musicService.incrementPlayCount(id)
      return { playCount: song.playCount }
    } catch (error) {
      set.status = 404
      return { error: '歌曲不存在' }
    }
  }, {
    params: t.Object({
      id: t.String()
    }),
    response: {
      200: t.Object({ playCount: t.Number() }),
      404: ErrorResponse
    }
  })

export type Music = typeof music
