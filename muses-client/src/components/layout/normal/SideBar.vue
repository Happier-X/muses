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
  Music as SongIcon,
  Disc3 as AlbumIcon,
  MicVocal as ArtistIcon,
  ScrollText as PlaylistIcon,
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
        value: "song",
        icon: () => h(SongIcon, { size: 14 }),
      },
      {
        label: "专辑",
        value: "album",
        icon: () => h(AlbumIcon, { size: 14 }),
      },
      {
        label: "艺术家",
        value: "artist",
        icon: () => h(ArtistIcon, { size: 14 }),
      },
      {
        label: "歌单",
        value: "playlist",
        icon: () => h(PlaylistIcon, { size: 14 }),
      },
      {
        label: "设置",
        value: "setting",
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
  song: "song",
  album: "album",
  artist: "artist",
  playlist: "playlist",
  settings: "settings",
};

watch(
  () => route.path,
  () => {
    const key = Object.keys(pathToMenu).find((k) =>
      route.path.startsWith(`/${k}`)
    );
    activeMenu.value = key ? pathToMenu[key] : "";
  }
);
</script>
