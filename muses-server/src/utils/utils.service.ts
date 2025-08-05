import { Injectable } from '@nestjs/common';
import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import { PrismaService } from 'src/prisma/prisma.service';
import { createHash } from 'crypto';
import { createReadStream } from 'fs';
import { MUSIC_EXTENSIONS } from 'src/utils/musicExtensions';
import { ILyricsTag } from 'music-metadata';

// 音乐信息接口
interface MusicInfo {
  title: string;
  artist: string;
  album: string;
  lyrics: ILyricsTag[];
  duration: number;
}

@Injectable()
export class UtilsService {
  constructor(private prisma: PrismaService) {}

  private lastScanTime: Date | null = null;

  /**
   * 扫描文件夹并入库
   */
  async scanDir(dir: string, scanAll: boolean) {
    const files = await fs.readdir(dir, {
      withFileTypes: true,
      recursive: true,
    });

    if (!scanAll) {
      await this.getLastScanTime();
    }

    for (const file of files) {
      if (!file.isFile()) continue;

      const ext = path.extname(file.name).toLowerCase();
      if (!(MUSIC_EXTENSIONS as readonly string[]).includes(ext)) continue;

      const fullPath = path.join(file.parentPath, file.name);

      if (scanAll) {
        try {
          const [musicInfo, fileHash] = await Promise.all([
            this.getFileMusicInfo(fullPath),
            this.calcFileHash(fullPath),
          ]);
          await this.addSong(musicInfo, fileHash as string, fullPath);
        } catch (err) {
          console.error(`处理文件失败: ${fullPath}`, err);
        }
      } else {
        const stats = await fs.stat(fullPath);
        if (!this.lastScanTime || stats.mtime > this.lastScanTime) {
          try {
            const [musicInfo, fileHash] = await Promise.all([
              this.getFileMusicInfo(fullPath),
              this.calcFileHash(fullPath),
            ]);
            await this.addSong(musicInfo, fileHash as string, fullPath);
          } catch (err) {
            console.error(`处理文件失败: ${fullPath}`, err);
          }
        }
      }
    }
    await this.cleanDeletedFiles();
    await this.saveLastScanTime();
  }

  /**
   * 清理数据库中已删除的文件记录
   */
  private async cleanDeletedFiles() {
    const songs = await this.prisma.song.findMany();
    let deletedCount = 0;

    for (const song of songs) {
      try {
        await fs.access(song.filePath);
      } catch (err) {
        try {
          await this.prisma.song.delete({
            where: { id: song.id }
          });
          deletedCount++;
        } catch (deleteErr) {
          console.error(`删除数据库记录失败: ${song.filePath}`, deleteErr);
        }
      }
    }
    return deletedCount;
  }

  /**
   * 获取文件音乐信息
   */
  private async getFileMusicInfo(filePath: string): Promise<MusicInfo> {
    let musicInfo: MusicInfo = {
      title: '',
      artist: '',
      album: '',
      lyrics: [],
      duration: 0,
    };
    
    try {
      const res = await (await import('music-metadata')).parseFile(filePath);
      musicInfo.title = res.common.title || '';
      musicInfo.artist = res.common.artist || '';
      musicInfo.album = res.common.album || '';
      musicInfo.lyrics = res.common.lyrics || [];
      musicInfo.duration = res.format.duration
        ? Math.round(res.format.duration)
        : 0;
      return musicInfo;
    } catch (error) {
      console.error(`解析音乐文件信息失败: ${filePath}`, error);
      return musicInfo;
    }
  }

  /**
   * 计算文件哈希值
   */
  private async calcFileHash(filePath: string): Promise<string> {
    const hash = createHash('sha256');
    const stream = createReadStream(filePath);

    return new Promise((resolve, reject) => {
      stream.on('data', (chunk) => hash.update(chunk));
      stream.on('end', () => resolve(hash.digest('hex')));
      stream.on('error', (err) => {
        console.error(`计算文件哈希值失败: ${filePath}`, err);
        reject(err);
      });
    });
  }

  /**
   * 歌曲入库
   */
  private async addSong(musicInfo: MusicInfo, fileHash: string, filePath: string) {
    const song = {
      title: musicInfo.title,
      artist: musicInfo.artist,
      album: musicInfo.album,
      lyrics: JSON.stringify(musicInfo.lyrics),
      duration: musicInfo.duration,
      filePath,
      fileHash,
    };
    
    try {
      await this.prisma.song.upsert({
        where: {
          filePath_fileHash: {
            filePath: song.filePath,
            fileHash: song.fileHash,
          },
        },
        update: {
          ...song,
        },
        create: {
          ...song,
        },
      });
    } catch (error) {
      console.error(`歌曲入库失败: ${filePath}`, error);
      
      throw error;
    }
  }

  /**
   * 获取上次扫描时间
   */
  private async getLastScanTime() {
    try {
      const config = await this.prisma.config.findUnique({
        where: { key: 'lastScanTime' },
      });

      if (config) {
        this.lastScanTime = new Date(config.value);
      }
    } catch (error) {
      console.error('加载上次扫描时间失败:', error);
    }
  }

  /**
   * 上次扫描时间入库
   */
  private async saveLastScanTime() {
    try {
      const timeString =
        this.lastScanTime?.toISOString() || new Date().toISOString();

      await this.prisma.config.upsert({
        where: { key: 'lastScanTime' },
        update: { value: timeString },
        create: { key: 'lastScanTime', value: timeString },
      });
    } catch (error) {
      console.error('保存上次扫描时间失败:', error);
    }
  }
}