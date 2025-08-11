<template>
  <el-menu ref="menuRef" router default-active="home">
    <el-menu-item v-for="item in menuList" :key="item.value" :index="item.value">
      <el-icon>
        <component :is="item.icon" />
      </el-icon>
      <template #title>
        {{ item.label }}
      </template>
    </el-menu-item>
  </el-menu>
</template>
<script setup lang="ts">
import {
  Disc3 as AlbumsIcon,
  MicVocal as ArtistsIcon,
  House as HomeIcon,
  ScrollText as PlaylistsIcon,
  Settings as SettingsIcon,
  Music as SongsIcon
} from 'lucide-vue-next'
import { h, ref, useTemplateRef, watch } from 'vue'
import { useRoute } from 'vue-router'
const menuList = ref([
  {
    label: '首页',
    value: 'home',
    icon: () => h(HomeIcon, { size: 14 })
  },
  {
    label: '歌曲',
    value: 'songs',
    icon: () => h(SongsIcon, { size: 14 })
  },
  {
    label: '专辑',
    value: 'albums',
    icon: () => h(AlbumsIcon, { size: 14 })
  },
  {
    label: '艺术家',
    value: 'artists',
    icon: () => h(ArtistsIcon, { size: 14 })
  },
  {
    label: '歌单',
    value: 'playlists',
    icon: () => h(PlaylistsIcon, { size: 14 })
  },
  {
    label: '设置',
    value: 'settings',
    icon: () => h(SettingsIcon, { size: 14 })
  }
])
const route = useRoute()
const menuRef = useTemplateRef('menuRef')
const pathToMenu: Record<string, string> = {
  home: 'home',
  songs: 'songs',
  albums: 'albums',
  artists: 'artists',
  playlists: 'playlists',
  settings: 'settings'
}

watch(
  () => route.path,
  () => {
    const key = Object.keys(pathToMenu).find((k) => route.path.startsWith(`/app/${k}`))
    if (key) {
      menuRef.value.updateActiveIndex(pathToMenu[key])
    }
  }
)
</script>
