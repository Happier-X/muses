import { PrismaClient } from '@prisma/client'
import * as mm from 'music-metadata'
import fs from 'fs'
import path from 'path'

const prisma = new PrismaClient()

// 支持的音乐格式
const SUPPORTED_FORMATS = ['.mp3', '.flac', '.wav', '.ogg', '.m4a', '.aac', '.wma']

export interface ScanResult {
  added: number
  updated: number
  skipped: number
  errors: string[]
}

export class MusicService {
  // 扫描音乐目录
  async scanDirectory(musicPath: string): Promise<ScanResult> {
    const result: ScanResult = {
      added: 0,
      updated: 0,
      skipped: 0,
      errors: []
    }

    if (!musicPath) {
      result.errors.push('音乐目录路径未配置')
      return result
    }

    if (!fs.existsSync(musicPath)) {
      result.errors.push(`音乐目录不存在: ${musicPath}`)
      return result
    }

    const files = this.getMusicFiles(musicPath)

    for (const filePath of files) {
      try {
        const song = await this.processFile(filePath)
        if (song) {
          result.added++
        } else {
          result.skipped++
        }
      } catch (error) {
        result.errors.push(`处理文件失败 ${filePath}: ${error}`)
      }
    }

    return result
  }

  // 获取所有音乐文件
  private getMusicFiles(dirPath: string): string[] {
    const files: string[] = []

    const items = fs.readdirSync(dirPath, { withFileTypes: true })

    for (const item of items) {
      const fullPath = path.join(dirPath, item.name)

      if (item.isDirectory()) {
        // 递归扫描子目录
        files.push(...this.getMusicFiles(fullPath))
      } else if (item.isFile()) {
        const ext = path.extname(item.name).toLowerCase()
        if (SUPPORTED_FORMATS.includes(ext)) {
          files.push(fullPath)
        }
      }
    }

    return files
  }

  // 处理单个音乐文件
  private async processFile(filePath: string): Promise<boolean> {
    // 检查文件是否已存在
    const existing = await prisma.song.findUnique({
      where: { filePath }
    })

    // 读取元数据
    const metadata = await mm.parseFile(filePath)
    const stats = fs.statSync(filePath)

    const songData = {
      title: metadata.common.title || path.basename(filePath, path.extname(filePath)),
      artist: metadata.common.artist || null,
      album: metadata.common.album || null,
      albumArtist: metadata.common.albumartist || null,
      year: metadata.common.year ? parseInt(metadata.common.year.toString()) : null,
      track: metadata.common.track.no || null,
      genre: metadata.common.genre?.[0] || null,
      duration: metadata.format.duration || null,
      bitrate: metadata.format.bitrate ? Math.round(metadata.format.bitrate / 1000) : null,
      sampleRate: metadata.format.sampleRate || null,
      format: path.extname(filePath).slice(1).toUpperCase(),
      filePath,
      fileSize: stats.size
    }

    if (existing) {
      // 检查是否有变化
      const hasChanges =
        existing.title !== songData.title ||
        existing.artist !== songData.artist ||
        existing.album !== songData.album ||
        existing.year !== songData.year ||
        existing.genre !== songData.genre ||
        existing.duration !== songData.duration

      if (hasChanges) {
        await prisma.song.update({
          where: { id: existing.id },
          data: songData
        })
        return false // 表示更新
      }
      return false // 无变化
    }

    // 创建新记录
    await prisma.song.create({
      data: songData
    })

    return true // 表示新增
  }

  // 获取所有歌曲
  async getAllSongs() {
    return prisma.song.findMany({
      orderBy: { createdAt: 'desc' }
    })
  }

  // 根据 ID 获取歌曲
  async getSongById(id: string) {
    return prisma.song.findUnique({
      where: { id }
    })
  }

  // 搜索歌曲
  async searchSongs(query: string) {
    return prisma.song.findMany({
      where: {
        OR: [
          { title: { contains: query } },
          { artist: { contains: query } },
          { album: { contains: query } }
        ]
      },
      orderBy: { title: 'asc' }
    })
  }

  // 删除歌曲
  async deleteSong(id: string) {
    return prisma.song.delete({
      where: { id }
    })
  }

  // 增加播放次数
  async incrementPlayCount(id: string) {
    return prisma.song.update({
      where: { id },
      data: { playCount: { increment: 1 } }
    })
  }
}

export const musicService = new MusicService()
