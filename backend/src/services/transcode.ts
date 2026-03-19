import ffmpeg from 'fluent-ffmpeg';
import fs from 'fs';
import path from 'path';
import { config } from '../config/index.js';

// Ensure cache directory exists
const cacheDir = path.join(config.cachePath, 'transcoded');
if (!fs.existsSync(cacheDir)) {
  fs.mkdirSync(cacheDir, { recursive: true });
}

export type TranscodeFormat = 'aac' | 'mp3' | 'opus';
export type Quality = 'low' | 'medium' | 'high';

const qualitySettings: Record<Quality, { bitrate: string; sampleRate: string }> = {
  low: { bitrate: '128k', sampleRate: '44100' },
  medium: { bitrate: '256k', sampleRate: '44100' },
  high: { bitrate: '320k', sampleRate: '48000' }
};

function getCacheKey(filePath: string, format: TranscodeFormat, quality: Quality): string {
  const stat = fs.statSync(filePath);
  const hash = `${path.basename(filePath)}-${stat.mtime.getTime()}-${format}-${quality}`;
  return hash.replace(/[^a-zA-Z0-9]/g, '_');
}

function getOutputPath(cacheKey: string, format: TranscodeFormat): string {
  return path.join(cacheDir, `${cacheKey}.${format}`);
}

export async function transcode(
  filePath: string,
  format: TranscodeFormat,
  quality: Quality
): Promise<string> {
  const cacheKey = getCacheKey(filePath, format, quality);
  const outputPath = getOutputPath(cacheKey, format);

  // Check cache
  if (fs.existsSync(outputPath)) {
    return outputPath;
  }

  const settings = qualitySettings[quality];

  return new Promise((resolve, reject) => {
    let command = ffmpeg(filePath)
      .audioCodec(format === 'mp3' ? 'libmp3lame' : 'aac')
      .audioBitrate(settings.bitrate)
      .audioFrequency(parseInt(settings.sampleRate));

    if (format === 'aac') {
      command = command.format('adts');
    } else if (format === 'mp3') {
      command = command.format('mp3');
    }

    command
      .on('end', () => resolve(outputPath))
      .on('error', (err) => reject(err))
      .save(outputPath);
  });
}

export function getBestFormat(acceptEncoding: string | undefined): TranscodeFormat {
  if (!acceptEncoding) return 'aac';

  const encodings = acceptEncoding.split(',').map(e => e.trim().toLowerCase());

  if (encodings.includes('aac')) return 'aac';
  if (encodings.includes('mp3')) return 'mp3';
  if (encodings.includes('opus')) return 'opus';

  return 'aac';
}

export function getQualityFromBitrate(maxBitrate: number | undefined): Quality {
  if (!maxBitrate || maxBitrate <= 128000) return 'low';
  if (maxBitrate <= 256000) return 'medium';
  return 'high';
}

export async function cleanupCache(maxSizeMB: number = 10240) {
  const files = fs.readdirSync(cacheDir)
    .map(f => {
      const filePath = path.join(cacheDir, f);
      const stat = fs.statSync(filePath);
      return { filePath, size: stat.size, mtime: stat.mtime };
    })
    .sort((a, b) => a.mtime.getTime() - b.mtime.getTime());

  let totalSize = files.reduce((sum, f) => sum + f.size, 0);
  const maxSizeBytes = maxSizeMB * 1024 * 1024;

  for (const file of files) {
    if (totalSize <= maxSizeBytes) break;
    fs.unlinkSync(file.filePath);
    totalSize -= file.size;
  }
}
