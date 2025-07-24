import { Injectable, StreamableFile } from '@nestjs/common';
import { readdirSync, statSync, createReadStream } from 'fs';
import { join, extname } from 'path';
import { createHash } from 'crypto';
import { parseFile } from 'music-metadata';
import { PrismaService } from 'src/prisma/prisma.service';


@Injectable()
export class SongsService {
    constructor(private prisma: PrismaService) { }

    private readonly musicDir = join(process.cwd(), 'music');
    private readonly exts = ['.mp3', '.wav', '.flac'];

    // 计算文件的哈希值
    private async calcFileHash(filePath: string) {
        const { format } = await parseFile(filePath);
        const duration = typeof format.duration === 'number' ? format.duration : 0;
        const audioStream = createReadStream(filePath, {
            start: duration > 60 ? 500000 : 0,
        })
        const hash = createHash('sha256');
        hash.update(`${format.duration}-${format.bitrate}-${format.sampleRate}`);
        return new Promise((resolve, reject) => {
            let count = 0;
            audioStream.on('data', chunk => {
                if (count < 3) {
                    hash.update(chunk);
                    count++;
                }
            })
            audioStream.on('end', () => resolve(hash.digest('hex')))
        })
    }

    // 扫描文件夹
    private async scanDir(dir: string): Promise<any[]> {
        const result: any[] = [];
        const files = readdirSync(dir, { withFileTypes: true });
        for (const file of files) {
            const fullPath = join(dir, file.name);
            if (file.isDirectory()) {
                result.push(...await this.scanDir(fullPath));
            } else if (this.exts.includes(extname(file.name).toLowerCase())) {
                const hash = await this.calcFileHash(fullPath);
                result.push({
                    name: file.name,
                    path: fullPath,
                    hash,
                });
            }
        }
        await this.prisma.song.createMany({
            data: result.map(song => ({
                title: song.name,
                path: song.path,
                hash: song.hash,
            }))
        });
        return result;
    }

    // 扫描所有歌曲
    async scanAllSongs() {
        return this.scanDir(this.musicDir);
    }

    // 获取歌曲流
    async getStreamById(id: number): Promise<StreamableFile> {
        const song = await this.prisma.song.findUnique({ where: { id } });
        if (!song) {
            throw new Error('Song not found');
        }
        const file = createReadStream(song.path);
        const ext = extname(song.path).toLowerCase();
        let type = '';
        switch (ext) {
            case '.mp3':
                type = 'audio/mpeg';
                break;
            case '.wav':
                type = 'audio/wav';
                break;
            case '.flac':
                type = 'audio/flac';
                break;
            default:
                type = 'application/octet-stream';
        }
        return new StreamableFile(file, {
            type,
            disposition: `inline; filename="${encodeURIComponent(song.title)}"`,
        });
    }
}
