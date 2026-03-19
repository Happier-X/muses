import { useEffect, useState } from 'react';
import { libraryApi } from '../api/client';
import { usePlayerStore } from '../stores/player';
import { Play, Disc } from 'lucide-react';

interface Artist {
  id: number;
  name: string;
  _count: { songs: number; albums: number };
}

interface Album {
  id: number;
  title: string;
  artist: { id: number; name: string };
  _count: { songs: number };
}

interface Song {
  id: number;
  title: string;
  duration: number;
  artist: { id: number; name: string };
  album: { id: number; title: string };
}

export default function Library() {
  const [activeTab, setActiveTab] = useState<'songs' | 'albums' | 'artists'>('songs');
  const [songs, setSongs] = useState<Song[]>([]);
  const [albums, setAlbums] = useState<Album[]>([]);
  const [artists, setArtists] = useState<Artist[]>([]);
  const { play } = usePlayerStore();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    const [songsRes, albumsRes, artistsRes] = await Promise.all([
      libraryApi.getSongs(),
      libraryApi.getAlbums(),
      libraryApi.getArtists()
    ]);
    setSongs(songsRes.data);
    setAlbums(albumsRes.data);
    setArtists(artistsRes.data);
  };

  const formatDuration = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div>
      <div className="flex gap-2 mb-6">
        {(['songs', 'albums', 'artists'] as const).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 rounded ${
              activeTab === tab ? 'bg-slate-900 text-white' : 'bg-slate-100'
            }`}
          >
            {tab === 'songs' ? '歌曲' : tab === 'albums' ? '专辑' : '艺术家'}
          </button>
        ))}
      </div>

      {activeTab === 'songs' && (
        <div className="space-y-1">
          {songs.map(song => (
            <div
              key={song.id}
              className="flex items-center gap-3 p-2 hover:bg-slate-50 rounded cursor-pointer"
              onClick={() => play(song, songs)}
            >
              <button className="text-slate-400 hover:text-slate-900">
                <Play className="w-4 h-4" />
              </button>
              <div className="flex-1">
                <div className="font-medium">{song.title}</div>
                <div className="text-sm text-slate-500">{song.artist.name}</div>
              </div>
              <div className="text-sm text-slate-400">
                {formatDuration(song.duration)}
              </div>
            </div>
          ))}
        </div>
      )}

      {activeTab === 'albums' && (
        <div className="grid grid-cols-4 gap-4">
          {albums.map(album => (
            <div key={album.id} className="p-3 border rounded hover:bg-slate-50">
              <div className="aspect-square bg-slate-200 rounded mb-2 flex items-center justify-center">
                <Disc className="w-12 h-12 text-slate-400" />
              </div>
              <div className="font-medium truncate">{album.title}</div>
              <div className="text-sm text-slate-500">{album.artist.name}</div>
            </div>
          ))}
        </div>
      )}

      {activeTab === 'artists' && (
        <div className="grid grid-cols-4 gap-4">
          {artists.map(artist => (
            <div key={artist.id} className="p-3 border rounded hover:bg-slate-50">
              <div className="aspect-square bg-slate-200 rounded-full mb-2 flex items-center justify-center">
                <span className="text-2xl">{artist.name[0]}</span>
              </div>
              <div className="font-medium text-center">{artist.name}</div>
              <div className="text-sm text-slate-500 text-center">
                {artist._count.albums} 张专辑 · {artist._count.songs} 首歌曲
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
