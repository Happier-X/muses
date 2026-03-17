import dotenv from 'dotenv';

dotenv.config();

export const config = {
  port: process.env.PORT || 3000,
  jwtSecret: process.env.JWT_SECRET || 'your-secret-key',
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || '30d',
  databaseUrl: process.env.DATABASE_URL || 'file:./data/database.db',
  musicPath: process.env.MUSIC_PATH || '/music',
  cachePath: process.env.TRANSCODE_CACHE_PATH || './cache',
  corsOrigin: process.env.CORS_ORIGIN?.split(',') || ['http://localhost:5173']
};
