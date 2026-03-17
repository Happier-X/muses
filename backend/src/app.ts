import express from 'express';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import { errorHandler } from './middleware/error.js';
import authRoutes from './routes/auth.js';
import artistsRoutes from './routes/artists.js';
import albumsRoutes from './routes/albums.js';
import songsRoutes from './routes/songs.js';
import playlistsRoutes from './routes/playlists.js';
import favoritesRoutes from './routes/favorites.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const isProduction = process.env.NODE_ENV === 'production';

export function createApp() {
  const app = express();

  app.use(cors({
    origin: process.env.CORS_ORIGIN?.split(',') || ['http://localhost:5173', 'http://localhost:3001'],
    credentials: true
  }));
  app.use(express.json());
  app.use(express.urlencoded({ extended: true }));

  // API Routes
  app.use('/api/auth', authRoutes);
  app.use('/api/artists', artistsRoutes);
  app.use('/api/albums', albumsRoutes);
  app.use('/api/songs', songsRoutes);
  app.use('/api/playlists', playlistsRoutes);
  app.use('/api/favorites', favoritesRoutes);

  // Serve static files in production
  if (isProduction) {
    const staticPath = path.join(__dirname, '../../web/dist');
    app.use(express.static(staticPath));

    // SPA fallback - serve index.html for all non-API routes
    app.get('*', (req, res) => {
      res.sendFile(path.join(staticPath, 'index.html'));
    });
  }

  app.use(errorHandler);

  return app;
}
