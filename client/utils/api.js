const BASE_URL = 'http://localhost:3000/api';

function request(url, method = 'GET', data = {}) {
  const token = uni.getStorageSync('token');

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: token ? {
        'Authorization': 'Bearer ' + token
      } : {},
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data);
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('token');
          uni.reLaunch({ url: '/pages/login/login' });
          reject(new Error('未授权'));
        } else {
          reject(new Error(res.data.message || '请求失败'));
        }
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
}

export const api = {
  // 认证
  login: (data) => request('/auth/login', 'POST', data),
  register: (data) => request('/auth/register', 'POST', data),
  getMe: () => request('/auth/me', 'GET'),

  // 艺术家
  getArtists: () => request('/artists', 'GET'),
  getArtist: (id) => request('/artists/' + id, 'GET'),

  // 专辑
  getAlbums: () => request('/albums', 'GET'),
  getAlbum: (id) => request('/albums/' + id, 'GET'),

  // 歌曲
  getSongs: () => request('/songs', 'GET'),
  getSong: (id) => request('/songs/' + id, 'GET'),
  getSongStream: (id) => BASE_URL + '/songs/' + id + '/stream',

  // 播放列表
  getPlaylists: () => request('/playlists', 'GET'),
  getPlaylist: (id) => request('/playlists/' + id, 'GET'),
  createPlaylist: (data) => request('/playlists', 'POST', data),
  updatePlaylist: (id, data) => request('/playlists/' + id, 'PUT', data),
  deletePlaylist: (id) => request('/playlists/' + id, 'DELETE'),
  addSongToPlaylist: (playlistId, songId) => request('/playlists/' + playlistId + '/songs', 'POST', { songId }),
  removeSongFromPlaylist: (playlistId, songId) => request('/playlists/' + playlistId + '/songs/' + songId, 'DELETE'),

  // 收藏
  getFavorites: () => request('/favorites', 'GET'),
  addFavorite: (songId) => request('/favorites/' + songId, 'POST'),
  removeFavorite: (songId) => request('/favorites/' + songId, 'DELETE'),
};
