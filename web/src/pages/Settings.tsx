import { useState } from 'react';
import { libraryApi } from '../api/client';
import { useAuthStore } from '../stores/auth';
import { LogOut, RefreshCw } from 'lucide-react';

export default function Settings() {
  const [scanning, setScanning] = useState(false);
  const [message, setMessage] = useState('');
  const { logout, user } = useAuthStore();

  const handleScan = async () => {
    setScanning(true);
    setMessage('');
    try {
      const res = await libraryApi.scan();
      setMessage(`扫描完成：新增 ${res.data.added} 首，更新 ${res.data.updated} 首`);
    } catch (err: any) {
      setMessage('扫描失败：' + (err.response?.data?.error || err.message));
    }
    setScanning(false);
  };

  return (
    <div>
      <h2 className="text-xl font-bold mb-6">设置</h2>

      <div className="mb-6">
        <h3 className="font-medium mb-2">当前用户</h3>
        <div className="text-slate-600">{user?.username}</div>
      </div>

      <div className="mb-6">
        <h3 className="font-medium mb-2">音乐库</h3>
        <button
          onClick={handleScan}
          disabled={scanning}
          className="flex items-center gap-2 bg-slate-900 text-white px-4 py-2 rounded disabled:opacity-50"
        >
          <RefreshCw className={`w-4 h-4 ${scanning ? 'animate-spin' : ''}`} />
          {scanning ? '扫描中...' : '扫描音乐库'}
        </button>
        {message && <div className="mt-2 text-sm text-slate-600">{message}</div>}
      </div>

      <div className="border-t pt-6">
        <button
          onClick={logout}
          className="flex items-center gap-2 text-red-500 hover:text-red-600"
        >
          <LogOut className="w-4 h-4" />
          退出登录
        </button>
      </div>
    </div>
  );
}
