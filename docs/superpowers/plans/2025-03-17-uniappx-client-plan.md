# uniappx 移动客户端实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建 uniappx 移动客户端，实现登录注册、音乐库浏览、播放列表、收藏和后台播放功能

**Architecture:** 使用 Vue 3 (uvue) 语法，通过 uni.request 与后端 API 交互，使用 uni.getBackgroundAudioManager 实现后台音频播放

**Tech Stack:** uniappx, Vue 3, uni.request, uni.getBackgroundAudioManager, uni.setStorageSync

---

## 项目文件结构

```
client/
├── pages/
│   ├── index/index.uvue           # 首页（推荐/最近播放）
│   ├── library/library.uvue      # 音乐库
│   ├── library/artists.uvue      # 艺术家列表
│   ├── library/albums.uvue       # 专辑列表
│   ├── library/songs.uvue        # 歌曲列表
│   ├── profile/profile.uvue      # 我的（收藏/播放列表）
│   ├── profile/playlists.uvue   # 播放列表管理
│   ├── profile/settings.uvue    # 设置页面
│   ├── login/login.uvue         # 登录页
│   ├── login/register.uvue      # 注册页
│   ├── player/player.uvue       # 播放详情页
│   └── playlist/detail.uvue    # 播放列表详情
├── components/
│   ├── PlayerBar.uvue           # 底部播放栏
│   ├── SongItem.uvue            # 歌曲列表项
│   ├── ArtistCard.uvue          # 艺术家卡片
│   ├── AlbumCard.uvue           # 专辑卡片
│   └── PlaylistCard.uvue       # 播放列表卡片
├── utils/
│   ├── api.js                   # API 封装
│   ├── auth.js                  # 认证工具
│   └── player.js                # 播放器工具
├── store/
│   └── player.js                # 播放状态管理
├── pages.json
├── manifest.json
└── App.uvue
```

---

## Task 1: 项目基础配置

**Files:**
- Modify: `client/pages.json`
- Modify: `client/manifest.json`

- [ ] **Step 1: 配置 pages.json 页面路由和 tabBar**

```json
{
  "pages": [
    {
      "path": "pages/index/index",
      "style": {
        "navigationStyle": "custom",
        "navigationBarTitleText": "首页"
      }
    },
    {
      "path": "pages/library/library",
      "style": {
        "navigationBarTitleText": "音乐库"
      }
    },
    {
      "path": "pages/profile/profile",
      "style": {
        "navigationBarTitleText": "我的"
      }
    },
    {
      "path": "pages/login/login",
      "style": {
        "navigationBarTitleText": "登录"
      }
    },
    {
      "path": "pages/login/register",
      "style": {
        "navigationBarTitleText": "注册"
      }
    },
    {
      "path": "pages/player/player",
      "style": {
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/playlist/detail",
      "style": {
        "navigationBarTitleText": "播放列表"
      }
    }
  ],
  "tabBar": {
    "color": "#999999",
    "selectedColor": "#1DB954",
    "borderStyle": "white",
    "backgroundColor": "#ffffff",
    "list": [
      {
        "pagePath": "pages/index/index",
        "text": "首页",
        "iconPath": "static/tab-home.png",
        "selectedIconPath": "static/tab-home-active.png"
      },
      {
        "pagePath": "pages/library/library",
        "text": "音乐库",
        "iconPath": "static/tab-library.png",
        "selectedIconPath": "static/tab-library-active.png"
      },
      {
        "pagePath": "pages/profile/profile",
        "text": "我的",
        "iconPath": "static/tab-profile.png",
        "selectedIconPath": "static/tab-profile-active.png"
      }
    ]
  }
}
```

- [ ] **Step 2: 配置 manifest.json 添加后台音频权限**

```json
{
  "app": {
    "distribute": {
      "android": {
        "permissions": [
          "<uses-permission android:name=\"android.permission.WAKE_LOCK\"/>",
          "<uses-permission android:name=\"android.permission.FOREGROUND_SERVICE\"/>"
        ]
      }
    }
  }
}
```

---

## Task 2: 工具类封装

**Files:**
- Create: `client/utils/api.js`
- Create: `client/utils/auth.js`
- Create: `client/utils/player.js`

- [ ] **Step 1: 创建 API 封装 (client/utils/api.js)**

```javascript
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
```

- [ ] **Step 2: 创建认证工具 (client/utils/auth.js)**

```javascript
export function getToken() {
  return uni.getStorageSync('token');
}

export function setToken(token) {
  uni.setStorageSync('token', token);
}

export function removeToken() {
  uni.removeStorageSync('token');
}

export function isLoggedIn() {
  return !!getToken();
}

export async function checkAuth() {
  if (!isLoggedIn()) {
    uni.reLaunch({ url: '/pages/login/login' });
    return false;
  }
  return true;
}
```

- [ ] **Step 3: 创建播放器工具 (client/utils/player.js)**

```javascript
import { api } from './api.js';

let backgroundAudioManager = null;
let currentSong = null;
let playlist = [];
let currentIndex = 0;

function getManager() {
  if (!backgroundAudioManager) {
    backgroundAudioManager = uni.getBackgroundAudioManager();

    backgroundAudioManager.onPlay(() => {
      uni.$emit('player-play');
    });

    backgroundAudioManager.onPause(() => {
      uni.$emit('player-pause');
    });

    backgroundAudioManager.onStop(() => {
      uni.$emit('player-stop');
    });

    backgroundAudioManager.onEnded(() => {
      playNext();
    });

    backgroundAudioManager.onTimeUpdate(() => {
      uni.$emit('player-time-update', {
        currentTime: backgroundAudioManager.currentTime,
        duration: backgroundAudioManager.duration
      });
    });
  }
  return backgroundAudioManager;
}

export function playSong(song, list = []) {
  currentSong = song;
  playlist = list;
  currentIndex = list.findIndex(s => s.id === song.id);

  const manager = getManager();
  manager.title = song.title;
  manager.singer = song.artist?.name || '';
  manager.coverImageUrl = song.album?.cover_art || '';
  manager.src = api.getSongStream(song.id);

  uni.$emit('player-change', song);
}

export function pause() {
  getManager().pause();
}

export function resume() {
  getManager().play();
}

export function playNext() {
  if (playlist.length === 0) return;
  currentIndex = (currentIndex + 1) % playlist.length;
  playSong(playlist[currentIndex], playlist);
}

export function playPrev() {
  if (playlist.length === 0) return;
  currentIndex = (currentIndex - 1 + playlist.length) % playlist.length;
  playSong(playlist[currentIndex], playlist);
}

export function seek(time) {
  getManager().seek(time);
}

export function getCurrentSong() {
  return currentSong;
}

export function getIsPlaying() {
  return getManager().pauseStatus !== true;
}
```

---

## Task 3: 登录注册页面

**Files:**
- Modify: `client/pages/login/login.uvue`
- Create: `client/pages/login/register.uvue`

- [ ] **Step 1: 创建登录页面 (client/pages/login/login.uvue)**

```vue
<template>
  <view class="container">
    <view class="logo-area">
      <text class="app-name">Muses</text>
      <text class="app-desc">音乐流媒体平台</text>
    </view>

    <view class="form">
      <input class="input" v-model="form.username" placeholder="用户名" />
      <input class="input" v-model="form.password" type="password" placeholder="密码" />

      <button class="btn-primary" @click="handleLogin" :loading="loading">登录</button>
      <button class="btn-link" @click="goRegister">没有账号？去注册</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { api } from '@/utils/api.js';
import { setToken } from '@/utils/auth.js';

const form = ref({ username: '', password: '' });
const loading = ref(false);

async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    uni.showToast({ title: '请填写完整', icon: 'none' });
    return;
  }

  loading.value = true;
  try {
    const res = await api.login(form.value);
    setToken(res.token);
    uni.switchTab({ url: '/pages/index/index' });
  } catch (err) {
    uni.showToast({ title: err.message || '登录失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/login/register' });
}
</script>

<style>
.container {
  flex: 1;
  padding: 40px 24px;
  background-color: #fff;
}
.logo-area {
  align-items: center;
  margin-bottom: 60px;
}
.app-name {
  font-size: 32px;
  font-weight: bold;
  color: #1DB954;
}
.app-desc {
  font-size: 14px;
  color: #999;
  margin-top: 8px;
}
.form {
  width: 100%;
}
.input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
}
.btn-primary {
  width: 100%;
  height: 48px;
  background-color: #1DB954;
  color: #fff;
  border-radius: 24px;
  font-size: 16px;
  margin-top: 16px;
}
.btn-link {
  margin-top: 16px;
  color: #1DB954;
  font-size: 14px;
}
</style>
```

- [ ] **Step 2: 创建注册页面 (client/pages/login/register.uvue)**

```vue
<template>
  <view class="container">
    <view class="header">
      <text class="title">注册账号</text>
    </view>

    <view class="form">
      <input class="input" v-model="form.username" placeholder="用户名" />
      <input class="input" v-model="form.password" type="password" placeholder="密码" />
      <input class="input" v-model="form.confirmPassword" type="password" placeholder="确认密码" />

      <button class="btn-primary" @click="handleRegister" :loading="loading">注册</button>
      <button class="btn-link" @click="goLogin">已有账号？去登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { api } from '@/utils/api.js';
import { setToken } from '@/utils/auth.js';

const form = ref({ username: '', password: '', confirmPassword: '' });
const loading = ref(false);

async function handleRegister() {
  if (!form.value.username || !form.value.password) {
    uni.showToast({ title: '请填写完整', icon: 'none' });
    return;
  }
  if (form.value.password !== form.value.confirmPassword) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' });
    return;
  }

  loading.value = true;
  try {
    const res = await api.register({
      username: form.value.username,
      password: form.value.password
    });
    setToken(res.token);
    uni.switchTab({ url: '/pages/index/index' });
  } catch (err) {
    uni.showToast({ title: err.message || '注册失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function goLogin() {
  uni.navigateBack();
}
</script>

<style>
.container {
  flex: 1;
  padding: 40px 24px;
  background-color: #fff;
}
.header {
  margin-bottom: 40px;
}
.title {
  font-size: 24px;
  font-weight: bold;
}
.form {
  width: 100%;
}
.input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
}
.btn-primary {
  width: 100%;
  height: 48px;
  background-color: #1DB954;
  color: #fff;
  border-radius: 24px;
  font-size: 16px;
  margin-top: 16px;
}
.btn-link {
  margin-top: 16px;
  color: #1DB954;
  font-size: 14px;
}
</style>
```

---

## Task 4: 首页和底部播放栏

**Files:**
- Modify: `client/pages/index/index.uvue`
- Create: `client/components/PlayerBar.uvue`

- [ ] **Step 1: 修改首页 (client/pages/index/index.uvue)**

```vue
<template>
  <view class="page">
    <view v-if="!isLoggedIn" class="not-login">
      <text class="tip">登录后享受更多功能</text>
      <button class="btn-login" @click="goLogin">立即登录</button>
    </view>

    <view v-else class="content">
      <view class="section">
        <text class="section-title">最近播放</text>
      </view>
    </view>

    <PlayerBar v-if="currentSong" />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { isLoggedIn } from '@/utils/auth.js';
import { getCurrentSong } from '@/utils/player.js';
import PlayerBar from '@/components/PlayerBar.uvue';

const currentSong = ref(null);

onMounted(() => {
  uni.$on('player-change', (song) => {
    currentSong.value = song;
  });
  currentSong.value = getCurrentSong();
});

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' });
}
</script>

<style>
.page {
  flex: 1;
  background-color: #f5f5f5;
  padding-bottom: 60px;
}
.not-login {
  flex: 1;
  justify-content: center;
  align-items: center;
}
.tip {
  color: #999;
  margin-bottom: 20px;
}
.btn-login {
  background-color: #1DB954;
  color: #fff;
  padding: 10px 40px;
  border-radius: 20px;
}
.content {
  padding: 16px;
}
.section-title {
  font-size: 18px;
  font-weight: bold;
}
</style>
```

- [ ] **Step 2: 创建底部播放栏组件 (client/components/PlayerBar.uvue)**

```vue
<template>
  <view class="player-bar" @click="goPlayer">
    <image class="cover" :src="song?.album?.cover_art || '/static/logo.png'" />
    <view class="info">
      <text class="title">{{ song?.title || '未在播放' }}</text>
      <text class="artist">{{ song?.artist?.name || '' }}</text>
    </view>
    <view class="controls">
      <text class="icon" @click.stop="togglePlay">{{ isPlaying ? '⏸' : '▶' }}</text>
      <text class="icon" @click.stop="playNext">⏭</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getCurrentSong, playNext, pause, resume, getIsPlaying } from '@/utils/player.js';

const song = ref(null);
const isPlaying = ref(false);

onMounted(() => {
  song.value = getCurrentSong();
  isPlaying.value = getIsPlaying();

  uni.$on('player-change', (s) => {
    song.value = s;
    isPlaying.value = true;
  });

  uni.$on('player-play', () => isPlaying.value = true);
  uni.$on('player-pause', () => isPlaying.value = false);
});

function togglePlay() {
  if (isPlaying.value) {
    pause();
  } else {
    resume();
  }
}

function goPlayer() {
  uni.navigateTo({ url: '/pages/player/player' });
}
</script>

<style>
.player-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background-color: #fff;
  flex-direction: row;
  align-items: center;
  padding: 0 16px;
  border-top: 1px solid #eee;
}
.cover {
  width: 44px;
  height: 44px;
  border-radius: 4px;
}
.info {
  flex: 1;
  margin-left: 12px;
}
.title {
  font-size: 14px;
  color: #333;
}
.artist {
  font-size: 12px;
  color: #999;
}
.controls {
  flex-direction: row;
}
.icon {
  font-size: 24px;
  margin-left: 16px;
}
</style>
```

---

## Task 5: 音乐库页面

**Files:**
- Modify: `client/pages/library/library.uvue`
- Create: `client/pages/library/artists.uvue`
- Create: `client/pages/library/albums.uvue`
- Create: `client/pages/library/songs.uvue`

- [ ] **Step 1: 创建音乐库主页 (client/pages/library/library.uvue)**

```vue
<template>
  <view class="page">
    <view class="tabs">
      <text
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab', { active: currentTab === tab.key }]"
        @click="currentTab = tab.key"
      >{{ tab.label }}</text>
    </view>

    <view v-if="currentTab === 'artists'" class="list">
      <view v-for="artist in artists" :key="artist.id" class="item" @click="goArtist(artist)">
        <image class="cover" :src="artist.cover_art || '/static/logo.png'" />
        <text class="name">{{ artist.name }}</text>
      </view>
    </view>

    <view v-if="currentTab === 'albums'" class="list">
      <view v-for="album in albums" :key="album.id" class="item" @click="goAlbum(album)">
        <image class="cover" :src="album.cover_art || '/static/logo.png'" />
        <text class="name">{{ album.title }}</text>
        <text class="sub">{{ album.artist?.name }}</text>
      </view>
    </view>

    <view v-if="currentTab === 'songs'" class="list">
      <view v-for="song in songs" :key="song.id" class="item" @click="playSong(song)">
        <text class="title">{{ song.title }}</text>
        <text class="sub">{{ song.artist?.name }} - {{ song.album?.title }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { api } from '@/utils/api.js';
import { playSong } from '@/utils/player.js';

const currentTab = ref('songs');
const tabs = [
  { key: 'songs', label: '歌曲' },
  { key: 'albums', label: '专辑' },
  { key: 'artists', label: '艺术家' }
];

const artists = ref([]);
const albums = ref([]);
const songs = ref([]);

onMounted(async () => {
  try {
    const [artistsRes, albumsRes, songsRes] = await Promise.all([
      api.getArtists(),
      api.getAlbums(),
      api.getSongs()
    ]);
    artists.value = artistsRes;
    albums.value = albumsRes;
    songs.value = songsRes;
  } catch (err) {
    uni.showToast({ title: '加载失败', icon: 'none' });
  }
});

function goArtist(artist) {
  uni.navigateTo({ url: `/pages/library/artists?id=${artist.id}` });
}

function goAlbum(album) {
  uni.navigateTo({ url: `/pages/library/albums?id=${album.id}` });
}

function playSong(song) {
  playSong(song, songs.value);
}
</script>

<style>
.page {
  flex: 1;
  background-color: #f5f5f5;
}
.tabs {
  flex-direction: row;
  background-color: #fff;
  padding: 12px;
}
.tab {
  flex: 1;
  text-align: center;
  font-size: 14px;
  color: #666;
  padding-bottom: 8px;
}
.tab.active {
  color: #1DB954;
  border-bottom-width: 2px;
  border-bottom-color: #1DB954;
}
.list {
  padding: 12px;
}
.item {
  flex-direction: row;
  align-items: center;
  padding: 12px;
  background-color: #fff;
  margin-bottom: 8px;
  border-radius: 8px;
}
.cover {
  width: 50px;
  height: 50px;
  border-radius: 4px;
}
.name, .title {
  font-size: 14px;
  color: #333;
  margin-left: 12px;
}
.sub {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}
</style>
```

- [ ] **Step 2: 创建艺术家详情页 (client/pages/library/artists.uvue)**

```vue
<template>
  <view class="page">
    <view class="header">
      <image class="cover" :src="artist.cover_art || '/static/logo.png'" />
      <text class="name">{{ artist.name }}</text>
    </view>
    <view class="songs">
      <text class="section-title">歌曲</text>
      <view v-for="song in artist.songs" :key="song.id" class="song-item" @click="playSong(song)">
        <text class="title">{{ song.title }}</text>
        <text class="duration">{{ formatDuration(song.duration) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onLoad } from '@/utils';
import { api } from '@/utils/api.js';
import { playSong } from '@/utils/player.js';

const artist = ref({});

onLoad(async (options) => {
  try {
    artist.value = await api.getArtist(options.id);
  } catch (err) {
    uni.showToast({ title: '加载失败', icon: 'none' });
  }
});

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}
</script>

<style>
.page {
  flex: 1;
  background-color: #f5f5f5;
}
.header {
  align-items: center;
  padding: 30px;
  background-color: #fff;
}
.cover {
  width: 150px;
  height: 150px;
  border-radius: 8px;
}
.name {
  font-size: 20px;
  font-weight: bold;
  margin-top: 16px;
}
.songs {
  margin-top: 16px;
  padding: 16px;
  background-color: #fff;
}
.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 12px;
}
.song-item {
  flex-direction: row;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom-width: 1px;
  border-bottom-color: #f5f5f5;
}
.title {
  font-size: 14px;
  color: #333;
}
.duration {
  font-size: 12px;
  color: #999;
}
</style>
```

- [ ] **Step 3: 创建专辑详情页 (client/pages/library/albums.uvue)**

类似艺术家详情页，实现专辑歌曲列表和播放功能。

- [ ] **Step 4: 创建歌曲列表页 (client/pages/library/songs.uvue)**

类似实现，支持搜索和筛选功能。

---

## Task 6: 我的页面和播放列表

**Files:**
- Modify: `client/pages/profile/profile.uvue`
- Create: `client/pages/profile/playlists.uvue`
- Create: `client/pages/playlist/detail.uvue`

- [ ] **Step 1: 创建我的页面 (client/pages/profile/profile.uvue)**

```vue
<template>
  <view class="page">
    <view class="user-info">
      <image class="avatar" src="/static/logo.png" />
      <text class="username">{{ user?.username || '未登录' }}</text>
    </view>

    <view class="menu">
      <view class="menu-item" @click="goFavorites">
        <text class="label">我的收藏</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @click="goPlaylists">
        <text class="label">我的播放列表</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @click="goSettings">
        <text class="label">设置</text>
        <text class="arrow">></text>
      </view>
    </view>

    <button v-if="isLoggedIn" class="btn-logout" @click="handleLogout">退出登录</button>

    <PlayerBar v-if="currentSong" />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { isLoggedIn, removeToken } from '@/utils/auth.js';
import { getCurrentSong } from '@/utils/player.js';
import { api } from '@/utils/api.js';
import PlayerBar from '@/components/PlayerBar.uvue';

const user = ref(null);
const currentSong = ref(null);

onMounted(async () => {
  currentSong.value = getCurrentSong();
  if (isLoggedIn()) {
    try {
      user.value = await api.getMe();
    } catch (err) {
      console.error(err);
    }
  }
});

function goFavorites() {
  if (!isLoggedIn()) return goLogin();
  uni.navigateTo({ url: '/pages/library/songs?type=favorites' });
}

function goPlaylists() {
  if (!isLoggedIn()) return goLogin();
  uni.navigateTo({ url: '/pages/profile/playlists' });
}

function goSettings() {
  uni.navigateTo({ url: '/pages/profile/settings' });
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' });
}

function handleLogout() {
  removeToken();
  user.value = null;
  uni.switchTab({ url: '/pages/index/index' });
}
</script>

<style>
.page {
  flex: 1;
  background-color: #f5f5f5;
  padding-bottom: 60px;
}
.user-info {
  align-items: center;
  padding: 40px;
  background-color: #fff;
}
.avatar {
  width: 80px;
  height: 80px;
  border-radius: 40px;
}
.username {
  font-size: 18px;
  font-weight: bold;
  margin-top: 12px;
}
.menu {
  margin-top: 16px;
  background-color: #fff;
}
.menu-item {
  flex-direction: row;
  justify-content: space-between;
  padding: 16px;
  border-bottom-width: 1px;
  border-bottom-color: #f5f5f5;
}
.label {
  font-size: 14px;
  color: #333;
}
.arrow {
  color: #999;
}
.btn-logout {
  margin: 30px 16px;
  background-color: #fff;
  color: #f00;
}
</style>
```

- [ ] **Step 2: 创建播放列表管理页 (client/pages/profile/playlists.uvue)**

```vue
<template>
  <view class="page">
    <view class="header">
      <text class="title">我的播放列表</text>
      <text class="add" @click="showCreateDialog">+ 新建</text>
    </view>

    <view class="list">
      <view v-for="playlist in playlists" :key="playlist.id" class="item" @click="goDetail(playlist)">
        <text class="name">{{ playlist.name }}</text>
        <text class="count">{{ playlist.songs?.length || 0 }} 首</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { api } from '@/utils/api.js';

const playlists = ref([]);

onMounted(async () => {
  try {
    playlists.value = await api.getPlaylists();
  } catch (err) {
    uni.showToast({ title: '加载失败', icon: 'none' });
  }
});

function showCreateDialog() {
  uni.showModal({
    title: '新建播放列表',
    editable: true,
    success: async (res) => {
      if (res.confirm && res.value) {
        try {
          await api.createPlaylist({ name: res.value });
          playlists.value = await api.getPlaylists();
          uni.showToast({ title: '创建成功' });
        } catch (err) {
          uni.showToast({ title: '创建失败', icon: 'none' });
        }
      }
    }
  });
}

function goDetail(playlist) {
  uni.navigateTo({ url: `/pages/playlist/detail?id=${playlist.id}` });
}
</script>

<style>
.page {
  flex: 1;
  background-color: #f5f5f5;
}
.header {
  flex-direction: row;
  justify-content: space-between;
  padding: 16px;
  background-color: #fff;
}
.title {
  font-size: 18px;
  font-weight: bold;
}
.add {
  color: #1DB954;
}
.list {
  padding: 16px;
}
.item {
  flex-direction: row;
  justify-content: space-between;
  padding: 16px;
  background-color: #fff;
  margin-bottom: 8px;
  border-radius: 8px;
}
.name {
  font-size: 14px;
  color: #333;
}
.count {
  font-size: 12px;
  color: #999;
}
</style>
```

- [ ] **Step 3: 创建播放列表详情页 (client/pages/playlist/detail.uvue)**

实现播放列表歌曲列表、删除歌曲、删除播放列表功能。

---

## Task 7: 播放详情页

**Files:**
- Create: `client/pages/player/player.uvue`

- [ ] **Step 1: 创建播放详情页 (client/pages/player/player.uvue)**

```vue
<template>
  <view class="page">
    <view class="header" @click="goBack">
      <text class="back"><</text>
    </view>

    <view class="cover-area">
      <image class="cover" :src="song?.album?.cover_art || '/static/logo.png'" />
    </view>

    <view class="info">
      <text class="title">{{ song?.title }}</text>
      <text class="artist">{{ song?.artist?.name }}</text>
    </view>

    <view class="progress">
      <text class="time">{{ formatTime(currentTime) }}</text>
      <view class="progress-bar">
        <view class="progress-inner" :style="{ width: progress + '%' }"></view>
      </view>
      <text class="time">{{ formatTime(duration) }}</text>
    </view>

    <view class="controls">
      <text class="control prev" @click="playPrev">⏮</text>
      <text class="control play" @click="togglePlay">{{ isPlaying ? '⏸' : '▶' }}</text>
      <text class="control next" @click="playNext">⏭</text>
    </view>

    <view class="extra-controls">
      <text class="icon" @click="toggleFavorite">{{ isFavorite ? '❤️' : '🤍' }}</text>
      <text class="icon" @click="addToPlaylist">➕</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getCurrentSong, playNext, playPrev, pause, resume } from '@/utils/player.js';
import { api } from '@/utils/api.js';

const song = ref(null);
const isPlaying = ref(false);
const currentTime = ref(0);
const duration = ref(0);
const progress = ref(0);
const isFavorite = ref(false);

onMounted(() => {
  song.value = getCurrentSong();

  uni.$on('player-change', (s) => {
    song.value = s;
    checkFavorite();
  });

  uni.$on('player-play', () => isPlaying.value = true);
  uni.$on('player-pause', () => isPlaying.value = false);
  uni.$on('player-time-update', (data) => {
    currentTime.value = data.currentTime;
    duration.value = data.duration;
    progress.value = (data.currentTime / data.duration) * 100;
  });
});

onUnmounted(() => {
  uni.$off('player-change');
  uni.$off('player-play');
  uni.$off('player-pause');
  uni.$off('player-time-update');
});

function togglePlay() {
  if (isPlaying.value) {
    pause();
  } else {
    resume();
  }
}

function formatTime(seconds) {
  if (!seconds) return '0:00';
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

function goBack() {
  uni.navigateBack();
}

async function checkFavorite() {
  if (!song.value) return;
  try {
    const favorites = await api.getFavorites();
    isFavorite.value = favorites.some(f => f.id === song.value.id);
  } catch (err) {}
}

async function toggleFavorite() {
  try {
    if (isFavorite.value) {
      await api.removeFavorite(song.value.id);
    } else {
      await api.addFavorite(song.value.id);
    }
    isFavorite.value = !isFavorite.value;
  } catch (err) {
    uni.showToast({ title: '操作失败', icon: 'none' });
  }
}

function addToPlaylist() {
  // TODO: 显示播放列表选择
}
</script>

<style>
.page {
  flex: 1;
  background-color: #1a1a1a;
  padding: 40px 20px;
}
.header {
  margin-bottom: 20px;
}
.back {
  color: #fff;
  font-size: 24px;
}
.cover-area {
  align-items: center;
  margin: 40px 0;
}
.cover {
  width: 250px;
  height: 250px;
  border-radius: 12px;
}
.info {
  align-items: center;
  margin-bottom: 30px;
}
.title {
  font-size: 20px;
  color: #fff;
  font-weight: bold;
}
.artist {
  font-size: 14px;
  color: #999;
  margin-top: 8px;
}
.progress {
  flex-direction: row;
  align-items: center;
  margin-bottom: 30px;
}
.time {
  font-size: 12px;
  color: #999;
  width: 40px;
}
.progress-bar {
  flex: 1;
  height: 4px;
  background-color: #333;
  border-radius: 2px;
  margin: 0 10px;
}
.progress-inner {
  height: 100%;
  background-color: #1DB954;
  border-radius: 2px;
}
.controls {
  flex-direction: row;
  justify-content: center;
  align-items: center;
  margin-bottom: 30px;
}
.control {
  color: #fff;
  font-size: 32px;
  margin: 0 20px;
}
.play {
  font-size: 48px;
}
.extra-controls {
  flex-direction: row;
  justify-content: center;
}
.icon {
  font-size: 24px;
  margin: 0 20px;
}
</style>
```

---

## Task 8: 设置页面

**Files:**
- Create: `client/pages/profile/settings.uvue`

- [ ] **Step 1: 构建设置页面 (client/pages/profile/settings.uvue)**

```vue
<template>
  <view class="page">
    <view class="section">
      <text class="section-title">服务器配置</text>
      <view class="item">
        <text class="label">API 地址</text>
        <input class="input" v-model="apiUrl" placeholder="http://192.168.1.x:3000" />
      </view>
      <button class="btn-save" @click="saveApiUrl">保存</button>
    </view>

    <view class="section">
      <text class="section-title">关于</text>
      <text class="version">Muses v1.0.0</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';

const apiUrl = ref(uni.getStorageSync('apiUrl') || '');

function saveApiUrl() {
  uni.setStorageSync('apiUrl', apiUrl.value);
  uni.showToast({ title: '保存成功' });
}
</script>

<style>
.page {
  flex: 1;
  background-color: #f5f5f5;
}
.section {
  margin-top: 16px;
  padding: 16px;
  background-color: #fff;
}
.section-title {
  font-size: 14px;
  color: #999;
  margin-bottom: 12px;
}
.item {
  flex-direction: row;
  align-items: center;
  margin-bottom: 12px;
}
.label {
  font-size: 14px;
  color: #333;
  width: 80px;
}
.input {
  flex: 1;
  font-size: 14px;
}
.btn-save {
  background-color: #1DB954;
  color: #fff;
  margin-top: 12px;
}
.version {
  font-size: 12px;
  color: #999;
}
</style>
```

---

## Task 9: 删除 Android 项目并更新文档

**Files:**
- Delete: `android/`
- Modify: `docs/superpowers/plans/2025-03-17-music-streaming-platform-plan.md`

- [ ] **Step 1: 删除 Android 文件夹**

```bash
rm -rf android/
```

- [ ] **Step 2: 更新实施计划文档**

将计划中的 Android 客户端部分替换为 uniappx 实施计划。

---

## Task 10: 测试和构建

- [ ] **Step 1: 在 HBuilderX 中运行调试**
- [ ] **Step 2: 测试登录注册功能**
- [ ] **Step 3: 测试音乐库浏览**
- [ ] **Step 4: 测试播放功能**
- [ ] **Step 5: 测试播放列表和收藏**
- [ ] **Step 6: 打包 Android APK**
