<template>
  <lew-menu
    v-model="activeMenu"
    :options="menuOptions"
    @change="handleMenuChange"
  />
</template>
<script setup lang="ts">
import {
  House as HomeIcon,
  Music as SongsIcon,
  Disc3 as AlbumsIcon,
  MicVocal as ArtistsIcon,
  ScrollText as PlaylistsIcon,
  Settings as SettingsIcon,
} from "lucide-vue-next";
import { h, ref, watch } from "vue";
import { useRouter, useRoute } from "vue-router";

const activeMenu = ref("home");
const menuOptions = ref([
  {
    label: "Muses",
    value: "",
    children: [
      {
        label: "首页",
        value: "home",
        icon: () => h(HomeIcon, { size: 14 }),
      },
      {
        label: "歌曲",
        value: "songs",
        icon: () => h(SongsIcon, { size: 14 }),
      },
      {
        label: "专辑",
        value: "albums",

        icon: () => h(AlbumsIcon, { size: 14 }),
      },
      {
        label: "艺术家",
        value: "artists",
        icon: () => h(ArtistsIcon, { size: 14 }),
      },
      {
        label: "歌单",
        value: "playlists",
        icon: () => h(PlaylistsIcon, { size: 14 }),
      },
      {
        label: "设置",
        value: "settings",
        icon: () => h(SettingsIcon, { size: 14 }),
      },
    ],
  },
]);
const router = useRouter();
const handleMenuChange = (item: any) => {
  router.push({
    name: item.value,
  });
};
const route = useRoute();
const pathToMenu: Record<string, string> = {
  home: "home",
  songs: "songs",
  albums: "albums",
  artists: "artists",
  playlists: "playlists",
  settings: "settings",
};

watch(
  () => route.path,
  () => {
    const key = Object.keys(pathToMenu).find((k) =>
      route.path.startsWith(`/app/${k}`)
    );
    activeMenu.value = key ? pathToMenu[key] : "";
  }
);
</script>
