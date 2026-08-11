<template>
  <k-page class="k-page m-page flex flex-col overflow-hidden !h-auto !bottom-0">
    <k-navbar rightClass="!h-8">
      <template #title>音乐</template>
      <template #right>
        <k-button
          v-if="segment === 'playlists'"
          component="button"
          clear
          rounded
          class="size-8"
          aria-label="新建歌单"
          @click="playlistsRef?.openCreateAlert()"
        >
          <component :is="add" aria-hidden="true" class="size-4" />
        </k-button>
      </template>
      <template #subnavbar>
        <div class="flex-1 min-w-0">
          <k-segmented strong rounded class="!my-0">
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
      </template>
    </k-navbar>
    <!-- 子页面区：四个列表页 v-show 切换（改造后各自无 navbar，仅内容） -->
    <div class="relative min-h-0 flex-1">
      <SongsPage v-show="segment === 'songs'" />
      <AlbumsPage v-show="segment === 'albums'" />
      <ArtistsPage v-show="segment === 'artists'" />
      <PlaylistsPage ref="playlistsRef" v-show="segment === 'playlists'" />
    </div>
  </k-page>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { kButton, kNavbar, kPage, kSegmented, kSegmentedButton } from '@/components/ui'
import { add } from '@/icons'
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

const playlistsRef = ref<InstanceType<typeof PlaylistsPage> | null>(null)
</script>