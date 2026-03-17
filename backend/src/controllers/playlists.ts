import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { AuthRequest } from '../middleware/auth.js';
import { AppError } from '../middleware/error.js';

const prisma = new PrismaClient();

export async function listPlaylists(req: AuthRequest, res: Response) {
  return prisma.playlist.findMany({
    where: { userId: req.userId },
    include: {
      songs: {
        include: { song: { include: { artist: true, album: true } } },
        orderBy: { position: 'asc' }
      },
      _count: { select: { songs: true } }
    },
    orderBy: { createdAt: 'desc' }
  });
}

export async function createPlaylist(req: AuthRequest, res: Response) {
  const { name } = req.body;

  if (!name) {
    throw new AppError(400, 'Playlist name required');
  }

  return prisma.playlist.create({
    data: { userId: req.userId!, name }
  });
}

export async function getPlaylist(req: AuthRequest, res: Response) {
  const id = parseInt(req.params.id);
  const playlist = await prisma.playlist.findFirst({
    where: { id, userId: req.userId },
    include: {
      songs: {
        include: { song: { include: { artist: true, album: true } } },
        orderBy: { position: 'asc' }
      }
    }
  });

  if (!playlist) {
    throw new AppError(404, 'Playlist not found');
  }

  return playlist;
}

export async function updatePlaylist(req: AuthRequest, res: Response) {
  const id = parseInt(req.params.id);
  const { name } = req.body;

  const playlist = await prisma.playlist.findFirst({
    where: { id, userId: req.userId }
  });

  if (!playlist) {
    throw new AppError(404, 'Playlist not found');
  }

  return prisma.playlist.update({
    where: { id },
    data: { name }
  });
}

export async function deletePlaylist(req: AuthRequest, res: Response) {
  const id = parseInt(req.params.id);

  const playlist = await prisma.playlist.findFirst({
    where: { id, userId: req.userId }
  });

  if (!playlist) {
    throw new AppError(404, 'Playlist not found');
  }

  await prisma.playlist.delete({ where: { id } });
  return { success: true };
}

export async function addSongToPlaylist(req: AuthRequest, res: Response) {
  const playlistId = parseInt(req.params.id);
  const { songId } = req.body;

  const playlist = await prisma.playlist.findFirst({
    where: { id: playlistId, userId: req.userId }
  });

  if (!playlist) {
    throw new AppError(404, 'Playlist not found');
  }

  const maxPosition = await prisma.playlistSong.aggregate({
    where: { playlistId },
    _max: { position: true }
  });

  const position = (maxPosition._max.position || 0) + 1;

  // Check if song already exists in playlist
  const existingSong = await prisma.playlistSong.findUnique({
    where: { playlistId_songId: { playlistId, songId } }
  });

  if (existingSong) {
    return { success: true };
  }

  await prisma.playlistSong.create({
    data: { playlistId, songId, position }
  });

  return { success: true };
}

export async function removeSongFromPlaylist(req: AuthRequest, res: Response) {
  const playlistId = parseInt(req.params.id);
  const songId = parseInt(req.params.songId);

  const playlist = await prisma.playlist.findFirst({
    where: { id: playlistId, userId: req.userId }
  });

  if (!playlist) {
    throw new AppError(404, 'Playlist not found');
  }

  await prisma.playlistSong.delete({
    where: { playlistId_songId: { playlistId, songId } }
  });

  return { success: true };
}
