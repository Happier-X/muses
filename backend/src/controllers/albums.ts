import { Response } from 'express';
import { getAllAlbums, getAlbumById } from '../services/music.js';
import { AppError } from '../middleware/error.js';

export async function listAlbums(req: Response) {
  return getAllAlbums();
}

export async function getAlbum(req: Response, id: number) {
  const album = await getAlbumById(id);
  if (!album) {
    throw new AppError(404, 'Album not found');
  }
  return album;
}
