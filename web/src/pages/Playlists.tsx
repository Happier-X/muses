import { useEffect, useState } from 'react';
import { playlistsApi } from '../api/client';
import { Plus, Trash2 } from 'lucide-react';

interface Playlist {
  id: number;
  name: string;
  songs: { song: any }[];
  _count: { songs: number };
}

export default function Playlists() {
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [newName, setNewName] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  useEffect(() => {
    loadPlaylists();
  }, []);

  const loadPlaylists = async () => {
    const res = await playlistsApi.list();
    setPlaylists(res.data);
  };

  const createPlaylist = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;

    await playlistsApi.create(newName);
    setNewName('');
    setShowCreate(false);
    loadPlaylists();
  };

  const deletePlaylist = async (id: number) => {
    if (!confirm('确定删除这个播放列表？')) return;
    await playlistsApi.delete(id);
    loadPlaylists();
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold">播放列表</h2>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-1 bg-slate-900 text-white px-3 py-1 rounded"
        >
          <Plus className="w-4 h-4" />
          新建
        </button>
      </div>

      {showCreate && (
        <form onSubmit={createPlaylist} className="mb-4 flex gap-2">
          <input
            type="text"
            placeholder="播放列表名称"
            value={newName}
            onChange={e => setNewName(e.target.value)}
            className="flex-1 p-2 border rounded"
            autoFocus
          />
          <button type="submit" className="bg-slate-900 text-white px-4 py-2 rounded">
            创建
          </button>
          <button
            type="button"
            onClick={() => setShowCreate(false)}
            className="px-4 py-2 border rounded"
          >
            取消
          </button>
        </form>
      )}

      <div className="space-y-2">
        {playlists.map(playlist => (
          <div
            key={playlist.id}
            className="flex items-center gap-3 p-3 border rounded hover:bg-slate-50"
          >
            <div className="flex-1">
              <div className="font-medium">{playlist.name}</div>
              <div className="text-sm text-slate-500">
                {playlist._count.songs} 首歌曲
              </div>
            </div>
            <button
              onClick={() => deletePlaylist(playlist.id)}
              className="text-slate-400 hover:text-red-500"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        ))}

        {playlists.length === 0 && (
          <div className="text-center text-slate-400 py-8">
            暂无播放列表
          </div>
        )}
      </div>
    </div>
  );
}
