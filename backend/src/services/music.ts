import fs from 'fs';
import path from 'path';
import * as mm from 'music-metadata';
import { PrismaClient } from '@prisma/client';
import { config } from '../config/index.js';

const prisma = new PrismaClient();

interface ScannedFile {
  filePath: string;
  fileName: string;
  format: string;
  metadata: {
    title?: string;
    artist?: string;
    album?: string;
    year?: number;
    track?: number;
    duration?: number;
    bitrate?: number;
  };
}

async function parseMetadata(filePath: string): Promise<ScannedFile> {
  const metadata = await mm.parseFile(filePath);
  const fileName = path.basename(filePath);
  const format = path.extname(filePath).slice(1).toLowerCase();

  return {
    filePath,
    fileName,
    format,
    metadata: {
      title: metadata.common.title || fileName.replace(/\.[^.]+$/, ''),
      artist: metadata.common.artist,
      album: metadata.common.album,
      year: metadata.common.year,
      track: metadata.common.track.no || undefined,
      duration: Math.round(metadata.format.duration || 0),
      bitrate: metadata.format.bitrate
    }
  };
}

async function getOrCreateArtist(name: string) {
  let artist = await prisma.artist.findFirst({ where: { name } });
  if (!artist) {
    artist = await prisma.artist.create({ data: { name } });
  }
  return artist;
}

async function getOrCreateAlbum(title: string, artistId: number, year?: number) {
  let album = await prisma.album.findFirst({
    where: { title, artistId }
  });
  if (!album) {
    album = await prisma.album.create({
      data: { title, artistId, year }
    });
  }
  return album;
}

export async function scanMusicLibrary() {
  const musicPath = config.musicPath;
  const audioExtensions = ['.mp3', '.flac', '.aac', '.m4a', '.ogg', '.wav', '.ape'];

  const files: string[] = [];

  function walkDir(dir: string) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        walkDir(fullPath);
      } else if (audioExtensions.includes(path.extname(entry.name).toLowerCase())) {
        files.push(fullPath);
      }
    }
  }

  if (!fs.existsSync(musicPath)) {
    throw new Error(`Music path does not exist: ${musicPath}`);
  }

  walkDir(musicPath);

  let added = 0;
  let updated = 0;

  for (const filePath of files) {
    const existing = await prisma.song.findFirst({ where: { filePath } });

    if (existing) {
      const stat = fs.statSync(filePath);
      if (stat.mtime <= existing.createdAt) {
        continue;
      }
    }

    const scanned = await parseMetadata(filePath);
    const artist = await getOrCreateArtist(scanned.metadata.artist || 'Unknown Artist');
    const album = await getOrCreateAlbum(
      scanned.metadata.album || 'Unknown Album',
      artist.id,
      scanned.metadata.year
    );

    if (existing) {
      await prisma.song.update({
        where: { id: existing.id },
        data: {
          title: scanned.metadata.title,
          albumId: album.id,
          artistId: artist.id,
          duration: scanned.metadata.duration || 0,
          fileFormat: scanned.format,
          bitrate: Math.round((scanned.metadata.bitrate || 0) / 1000),
          trackNumber: scanned.metadata.track
        }
      });
      updated++;
    } else {
      await prisma.song.create({
        data: {
          title: scanned.metadata.title || 'Unknown',
          albumId: album.id,
          artistId: artist.id,
          duration: scanned.metadata.duration || 0,
          filePath,
          fileFormat: scanned.format,
          bitrate: Math.round((scanned.metadata.bitrate || 0) / 1000),
          trackNumber: scanned.metadata.track
        }
      });
      added++;
    }
  }

  return { added, updated, total: files.length };
}

export async function getAllArtists() {
  return prisma.artist.findMany({
    include: {
      _count: { select: { songs: true, albums: true } }
    },
    orderBy: { name: 'asc' }
  });
}

export async function getArtistById(id: number) {
  return prisma.artist.findUnique({
    where: { id },
    include: {
      albums: { include: { _count: { select: { songs: true } } } },
      songs: true
    }
  });
}

export async function getAllAlbums() {
  return prisma.album.findMany({
    include: {
      artist: true,
      _count: { select: { songs: true } }
    },
    orderBy: { title: 'asc' }
  });
}

export async function getAlbumById(id: number) {
  return prisma.album.findUnique({
    where: { id },
    include: {
      artist: true,
      songs: { orderBy: { trackNumber: 'asc' } }
    }
  });
}

export async function getAllSongs() {
  return prisma.song.findMany({
    include: { artist: true, album: true },
    orderBy: { title: 'asc' }
  });
}

export async function getSongById(id: number) {
  return prisma.song.findUnique({
    where: { id },
    include: { artist: true, album: true }
  });
}
