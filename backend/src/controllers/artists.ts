import { Response } from 'express';
import { getAllArtists, getArtistById } from '../services/music.js';
import { AppError } from '../middleware/error.js';

export async function listArtists(req: Response) {
  const artists = await getAllArtists();
  return artists;
}

export async function getArtist(req: Response, id: number) {
  const artist = await getArtistById(id);
  if (!artist) {
    throw new AppError(404, 'Artist not found');
  }
  return artist;
}
