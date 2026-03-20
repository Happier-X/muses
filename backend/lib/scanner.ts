import * as fs from "fs";
import * as path from "path";
import { parseFile } from "music-metadata";
import { prisma } from "@/lib/prisma";

const SUPPORTED_FORMATS = [".mp3", ".flac", ".wav", ".m4a", ".ogg", ".aac", ".wma"];

interface AudioMetadata {
  title?: string;
  artist?: string;
  album?: string;
  duration?: number;
  track?: number;
  disc?: number;
  year?: number;
  genre?: string;
  coverUrl?: string;
}

async function parseAudioFile(filePath: string): Promise<AudioMetadata | null> {
  try {
    const metadata = await parseFile(filePath);
    const { common, format } = metadata;

    let coverUrl: string | undefined;

    if (common.picture && common.picture.length > 0) {
      const coverDir = path.join(process.cwd(), "public", "covers");
      if (!fs.existsSync(coverDir)) {
        fs.mkdirSync(coverDir, { recursive: true });
      }

      const picture = common.picture[0];
      const ext = picture.format.includes("png") ? ".png" : ".jpg";
      const fileName = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}${ext}`;
      const coverPath = path.join(coverDir, fileName);

      fs.writeFileSync(coverPath, picture.data);
      coverUrl = `/covers/${fileName}`;
    }

    return {
      title: common.title || path.basename(filePath, path.extname(filePath)),
      artist: common.artist || "Unknown Artist",
      album: common.album || "Unknown Album",
      duration: format.duration ? Math.round(format.duration) : 0,
      track: common.track?.no,
      disc: common.disc?.no,
      year: common.year,
      genre: common.genre?.[0],
      coverUrl,
    };
  } catch (error) {
    console.error(`Error parsing ${filePath}:`, error);
    return null;
  }
}

function findAudioFiles(dir: string): string[] {
  const files: string[] = [];

  if (!fs.existsSync(dir)) {
    return files;
  }

  const items = fs.readdirSync(dir, { withFileTypes: true });

  for (const item of items) {
    const fullPath = path.join(dir, item.name);

    if (item.isDirectory()) {
      files.push(...findAudioFiles(fullPath));
    } else if (item.isFile()) {
      const ext = path.extname(item.name).toLowerCase();
      if (SUPPORTED_FORMATS.includes(ext)) {
        files.push(fullPath);
      }
    }
  }

  return files;
}

export async function scanMusicFolder(folderPath: string): Promise<{
  success: boolean;
  total: number;
  added: number;
  errors: string[];
}> {
  const errors: string[] = [];
  let added = 0;

  try {
    // 更新扫描状态
    await prisma.scanConfig.upsert({
      where: { id: "default" },
      create: {
        id: "default",
        musicFolder: folderPath,
        scanStatus: "scanning",
      },
      update: {
        musicFolder: folderPath,
        scanStatus: "scanning",
      },
    });

    const audioFiles = findAudioFiles(folderPath);

    for (const filePath of audioFiles) {
      try {
        // 检查是否已存在
        const existing = await prisma.track.findFirst({
          where: { filePath },
        });

        if (existing) {
          continue;
        }

        const metadata = await parseAudioFile(filePath);
        if (!metadata) {
          errors.push(`Failed to parse: ${filePath}`);
          continue;
        }

        // 获取或创建艺术家
        let artist = await prisma.artist.findFirst({
          where: { name: metadata.artist || "Unknown Artist" },
        });

        if (!artist) {
          artist = await prisma.artist.create({
            data: {
              name: metadata.artist || "Unknown Artist",
            },
          });
        }

        // 获取或创建专辑
        let album = await prisma.album.findFirst({
          where: {
            artistId: artist.id,
            title: metadata.album || "Unknown Album",
          },
        });

        if (!album) {
          album = await prisma.album.create({
            data: {
              title: metadata.album || "Unknown Album",
              artistId: artist.id,
              artistName: metadata.artist || "Unknown Artist",
              year: metadata.year,
              genre: metadata.genre,
              cover: metadata.coverUrl || null,
            },
          });
        }

        // 创建曲目
        await prisma.track.create({
          data: {
            title: metadata.title || path.basename(filePath, path.extname(filePath)),
            artistId: artist.id,
            artistName: metadata.artist || "Unknown Artist",
            albumId: album.id,
            albumName: metadata.album || "Unknown Album",
            duration: metadata.duration || 0,
            trackNumber: metadata.track,
            discNumber: metadata.disc,
            year: metadata.year,
            genre: metadata.genre,
            audioUrl: filePath,
            filePath: filePath,
            coverUrl: metadata.coverUrl || null,
          },
        });

        added++;
      } catch (error) {
        errors.push(`Error processing ${filePath}: ${error}`);
      }
    }

    // 更新扫描状态
    await prisma.scanConfig.update({
      where: { id: "default" },
      data: {
        scanStatus: "completed",
        lastScanAt: new Date(),
      },
    });

    return {
      success: true,
      total: audioFiles.length,
      added,
      errors,
    };
  } catch (error) {
    // 更新扫描状态为失败
    await prisma.scanConfig.update({
      where: { id: "default" },
      data: { scanStatus: "failed" },
    }).catch(() => {});

    return {
      success: false,
      total: 0,
      added: 0,
      errors: [String(error)],
    };
  }
}
