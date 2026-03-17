import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/client';
import { useAuthStore } from '../stores/auth';

export default function Login() {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const setAuth = useAuthStore(state => state.setAuth);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      const api = isRegister ? authApi.register : authApi.login;
      const { data } = await api(username, password);
      setAuth(data.token, data.user);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.error || '操作失败');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100">
      <form onSubmit={handleSubmit} className="bg-white p-8 rounded-lg shadow-md w-80">
        <h2 className="text-2xl font-bold mb-6 text-center">
          {isRegister ? '注册' : '登录'}
        </h2>

        {error && <div className="text-red-500 text-sm mb-4">{error}</div>}

        <input
          type="text"
          placeholder="用户名"
          value={username}
          onChange={e => setUsername(e.target.value)}
          className="w-full p-2 border rounded mb-4"
          required
        />

        <input
          type="password"
          placeholder="密码"
          value={password}
          onChange={e => setPassword(e.target.value)}
          className="w-full p-2 border rounded mb-6"
          required
        />

        <button type="submit" className="w-full bg-slate-900 text-white p-2 rounded">
          {isRegister ? '注册' : '登录'}
        </button>

        <button
          type="button"
          onClick={() => setIsRegister(!isRegister)}
          className="w-full text-sm text-slate-500 mt-4"
        >
          {isRegister ? '已有账号？登录' : '没有账号？注册'}
        </button>
      </form>
    </div>
  );
}
