import { Outlet } from 'react-router-dom';

export default function Layout() {
  return (
    <div className="min-h-screen">
      <header className="h-14 bg-slate-900 flex items-center px-4">
        <h1 className="text-white font-bold text-lg">Muses</h1>
      </header>
      <main className="p-4">
        <Outlet />
      </main>
    </div>
  );
}
