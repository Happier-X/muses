import fs from 'fs';
import { Response } from 'express';
import { getSongById } from './music.js';
import { transcode, getBestFormat, getQualityFromBitrate } from './transcode.js';
import { AppError } from '../middleware/error.js';

export async function streamSong(
  id: number,
  res: Response,
  options: {
    acceptEncoding?: string;
    maxBitrate?: number;
    deviceType?: string;
  }
) {
  const song = await getSongById(id);

  if (!song) {
    throw new AppError(404, 'Song not found');
  }

  const { acceptEncoding, maxBitrate } = options;

  // Determine if transcoding is needed
  const format = getBestFormat(acceptEncoding);
  const quality = getQualityFromBitrate(maxBitrate);

  const needsTranscode = song.fileFormat !== 'mp3' && song.fileFormat !== 'aac';

  let streamPath: string;
  let contentType: string;
  let stat: fs.Stats;

  if (needsTranscode) {
    try {
      streamPath = await transcode(song.filePath, format, quality);
      contentType = format === 'mp3' ? 'audio/mpeg' : 'audio/aac';
    } catch {
      // Fallback to original file
      streamPath = song.filePath;
      contentType = getContentType(song.fileFormat);
    }
  } else {
    streamPath = song.filePath;
    contentType = getContentType(song.fileFormat);
  }

  stat = fs.statSync(streamPath);

  // Handle Range requests for seeking
  const range = res.req?.headers?.range;
  if (range) {
    const parts = range.replace(/bytes=/, '').split('-');
    const start = parseInt(parts[0], 10);
    const end = parts[1] ? parseInt(parts[1], 10) : stat.size - 1;
    const chunksize = end - start + 1;

    res.writeHead(206, {
      'Content-Range': `bytes ${start}-${end}/${stat.size}`,
      'Accept-Ranges': 'bytes',
      'Content-Length': chunksize,
      'Content-Type': contentType
    });

    const stream = fs.createReadStream(streamPath, { start, end });
    stream.pipe(res);
  } else {
    res.writeHead(200, {
      'Content-Length': stat.size,
      'Content-Type': contentType,
      'Accept-Ranges': 'bytes'
    });

    const stream = fs.createReadStream(streamPath);
    stream.pipe(res);
  }
}

function getContentType(format: string): string {
  const types: Record<string, string> = {
    mp3: 'audio/mpeg',
    aac: 'audio/aac',
    flac: 'audio/flac',
    ogg: 'audio/ogg',
    wav: 'audio/wav',
    m4a: 'audio/mp4',
    alac: 'audio/alac'
  };
  return types[format.toLowerCase()] || 'audio/mpeg';
}
