import { Request, Response } from 'express';
import { getAllSongs, getSongById, scanMusicLibrary } from '../services/music.js';
import { AppError } from '../middleware/error.js';

export async function listSongs(req: Response) {
  return getAllSongs();
}

export async function getSong(req: Response, id: number) {
  const song = await getSongById(id);
  if (!song) {
    throw new AppError(404, 'Song not found');
  }
  return song;
}

export async function scanLibrary(req: Request, res: Response) {
  const result = await scanMusicLibrary();
  return result;
}
