<template>
  <k-page class="k-page m-page flex flex-col overflow-hidden !h-auto !bottom-0">
    <k-navbar rightClass="!h-8">
      <template #title>首页</template>
    </k-navbar>
    <div class="m-content pb-[var(--content-pb)]">
      <m-empty
        v-if="recentPlays.length === 0"
        title="还没有最近播放"
        description="播放过的歌曲会出现在这里。"
        :icon="clockOutline"
      />

      <k-list v-else strong-ios outline-ios class="!my-0">
        <k-list-item
          v-for="entry in recentPlays"
          :key="entry.songId"
          :title="entry.title"
          :subtitle="entry.subtitle"
          link
          class="cursor-pointer"
          :class="{ 'is-playing': entry.songId === playerState.currentSong?.id }"
          @click="onPlay(entry.songId)"
        >
          <template #media>
            <m-cover :src="entry.coverUri || ''" :size="48" radius="sm" alt="" />
          </template>
        </k-list-item>
      </k-list>
    </div>
  </k-page>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { kList, kListItem, kNavbar, kPage, MEmpty, MCover } from '@/components/ui'
import { clockOutline } from '@/icons'
import { loadRecentPlays, type RecentPlayEntry } from '@/features/player/recent'
import { playSong, playerState } from '@/features/player/controller'
import { loadSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'

const recentPlays = ref<RecentPlayEntry[]>([])

const onPlay = (songId: string): void => {
  const song = loadSongs().find((item) => item.id === songId)
  if (song) {
    void playSong(song)
  }
}

onMounted(() => {
  recentPlays.value = loadRecentPlays()
})
</script>
