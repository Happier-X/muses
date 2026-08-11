<template>
  <k-page class="k-page m-page flex flex-col overflow-hidden !h-auto !bottom-0">
    <div class="flex min-h-0 flex-1 flex-col">
      <!-- 资料库分段控制器 -->
      <div class="z-10 shrink-0 px-4 pt-3 pb-2">
        <k-segmented class="!my-0">
          <k-segmented-button
            v-for="item in segments"
            :key="item.value"
            :active="segment === item.value"
            @click="segment = item.value"
          >
            {{ item.label }}
          </k-segmented-button>
        </k-segmented>
      </div>
      <!-- 子页面区：四个列表页 v-show 切换（每页自带 k-page/navbar） -->
      <div class="relative min-h-0 flex-1">
        <SongsPage v-show="segment === 'songs'" />
        <AlbumsPage v-show="segment === 'albums'" />
        <ArtistsPage v-show="segment === 'artists'" />
        <PlaylistsPage v-show="segment === 'playlists'" />
      </div>
    </div>
  </k-page>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { kPage, kSegmented, kSegmentedButton } from '@/components/ui'
import { getMusicSegment, setMusicSegment, type MusicSegment } from '@/features/player/musicSegment'
import SongsPage from './SongsPage.vue'
import AlbumsPage from './AlbumsPage.vue'
import ArtistsPage from './ArtistsPage.vue'
import PlaylistsPage from './PlaylistsPage.vue'

const segments: Array<{ value: MusicSegment; label: string }> = [
  { value: 'songs', label: '全部' },
  { value: 'albums', label: '专辑' },
  { value: 'artists', label: '艺术家' },
  { value: 'playlists', label: '歌单' },
]

const segment = ref<MusicSegment>(getMusicSegment())
watch(segment, (value) => setMusicSegment(value))
</script>
