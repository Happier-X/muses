import express from 'express';
import cors from 'cors';
import { errorHandler } from './middleware/error.js';
import authRoutes from './routes/auth.js';
import artistsRoutes from './routes/artists.js';
import albumsRoutes from './routes/albums.js';
import songsRoutes from './routes/songs.js';

export function createApp() {
  const app = express();

  app.use(cors({
    origin: process.env.CORS_ORIGIN?.split(',') || ['http://localhost:5173', 'http://localhost:3001'],
    credentials: true
  }));
  app.use(express.json());
  app.use(express.urlencoded({ extended: true }));

  // Routes will be added here
  app.use('/api/auth', authRoutes);
  app.use('/api/artists', artistsRoutes);
  app.use('/api/albums', albumsRoutes);
  app.use('/api/songs', songsRoutes);

  app.use(errorHandler);

  return app;
}
