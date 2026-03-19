import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { AuthRequest } from '../middleware/auth.js';
import { AppError } from '../middleware/error.js';

const prisma = new PrismaClient();

export async function listFavorites(req: AuthRequest, res: Response) {
  return prisma.favorite.findMany({
    where: { userId: req.userId },
    include: {
      song: { include: { artist: true, album: true } }
    },
    orderBy: { createdAt: 'desc' }
  });
}

export async function addFavorite(req: AuthRequest, res: Response) {
  const songId = parseInt(req.params.songId);

  const song = await prisma.song.findUnique({ where: { id: songId } });
  if (!song) {
    throw new AppError(404, 'Song not found');
  }

  return prisma.favorite.create({
    data: { userId: req.userId!, songId }
  }).catch(() => ({ success: true }));
}

export async function removeFavorite(req: AuthRequest, res: Response) {
  const songId = parseInt(req.params.songId);

  await prisma.favorite.delete({
    where: { userId_songId: { userId: req.userId!, songId } }
  }).catch(() => {});

  return { success: true };
}
