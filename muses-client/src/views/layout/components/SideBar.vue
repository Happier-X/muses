<template>
  <n-menu :options="menuOptions" v-model:value="activeMenuKey"> </n-menu>
</template>
<script setup lang="ts">
import { NIcon } from 'naive-ui'
import { h, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

const activeMenuKey = ref('home')
const menuOptions = ref([
  {
    label: () =>
      h(
        RouterLink,
        {
          to: {
            name: 'home'
          }
        },
        { default: () => '首页' }
      ),
    key: 'home',
    icon:()=> h(NIcon,{class:'i-mingcute-home-3-line'})
  },
  {
    label: () =>
      h(
        RouterLink,
        {
          to: {
            name: 'songs'
          }
        },
        { default: () => '歌曲' }
      ),
    key: 'songs',
    icon: () => h(NIcon,{class:'i-mingcute-music-2-line'})
  },
  {
    label: () =>
      h(
        RouterLink,
        {
          to: {
            name: 'albums'
          }
        },
        { default: () => '专辑' }
      ),
    key: 'albums',
    icon: () => h(NIcon,{class:'i-mingcute-album-line'})
  },
  {
    label: () =>
      h(
        RouterLink,
        {
          to: {
            name: 'artists'
          }
        },
        { default: () => '艺术家' }
      ),
    key: 'artists',
    icon: () => h(NIcon,{class:'i-mingcute-mic-2-line'})
  },
  {
    label: () =>
      h(
        RouterLink,
        {
          to: {
            name: 'playlists'
          }
        },
        { default: () => '歌单' }
      ),
    key: 'playlists',
    icon: () => h(NIcon,{class:'i-mingcute-playlist-2-line'})
  },
  {
    label: () =>
      h(
        RouterLink,
        {
          to: {
            name: 'settings'
          }
        },
        { default: () => '设置' }
      ),
    key: 'settings',
    icon: () => h(NIcon,{class:'i-mingcute-settings-3-line'})
  }
])
import { useRoute } from 'vue-router'
const route = useRoute()
watch(()=>route.path,()=>{
  const key = Object.keys(pathToMenu).find((k) => route.path.startsWith(`/app/${k}`))
  if (key) {
    activeMenuKey.value = pathToMenu[key]
  }
})
const pathToMenu = {
  home: 'home',
  songs: 'songs',
  albums: 'albums',
  artists: 'artists',
  playlists: 'playlists',
  settings: 'settings'
}
</script>
