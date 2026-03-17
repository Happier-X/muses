import { Outlet, Link, useLocation } from 'react-router-dom';
import { Music, ListMusic, Settings, PlayCircle } from 'lucide-react';
import Player from './Player';

export default function Layout() {
  const location = useLocation();

  const navs = [
    { path: '/', icon: Music, label: '音乐库' },
    { path: '/playlists', icon: ListMusic, label: '播放列表' },
    { path: '/settings', icon: Settings, label: '设置' }
  ];

  return (
    <div className="min-h-screen flex flex-col">
      <header className="h-14 bg-slate-900 flex items-center px-4">
        <h1 className="text-white font-bold text-lg flex items-center gap-2">
          <PlayCircle className="w-6 h-6" />
          Muses
        </h1>
      </header>

      <div className="flex-1 flex">
        <nav className="w-48 bg-slate-50 border-r p-2">
          {navs.map(nav => (
            <Link
              key={nav.path}
              to={nav.path}
              className={`flex items-center gap-2 px-3 py-2 rounded ${
                location.pathname === nav.path
                  ? 'bg-slate-200'
                  : 'hover:bg-slate-100'
              }`}
            >
              <nav.icon className="w-4 h-4" />
              {nav.label}
            </Link>
          ))}
        </nav>

        <main className="flex-1 p-4 pb-32">
          <Outlet />
        </main>
      </div>

      <Player />
    </div>
  );
}
