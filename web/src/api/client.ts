import axios from 'axios';
import { useAuthStore } from '../stores/auth';

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
});

client.interceptors.request.use(config => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const authApi = {
  login: (username: string, password: string) => client.post('/auth/login', { username, password }),
  register: (username: string, password: string) => client.post('/auth/register', { username, password }),
  me: () => client.get('/auth/me')
};

export const libraryApi = {
  getArtists: () => client.get('/artists'),
  getAlbums: () => client.get('/albums'),
  getSongs: () => client.get('/songs'),
  scan: () => client.post('/songs/scan')
};

export const playlistsApi = {
  list: () => client.get('/playlists'),
  create: (name: string) => client.post('/playlists', { name }),
  get: (id: number) => client.get(`/playlists/${id}`),
  delete: (id: number) => client.delete(`/playlists/${id}`),
  addSong: (playlistId: number, songId: number) => client.post(`/playlists/${playlistId}/songs`, { songId }),
  removeSong: (playlistId: number, songId: number) => client.delete(`/playlists/${playlistId}/songs/${songId}`)
};

export const favoritesApi = {
  list: () => client.get('/favorites'),
  add: (songId: number) => client.post(`/favorites/${songId}`),
  remove: (songId: number) => client.delete(`/favorites/${songId}`)
};

export default client;
