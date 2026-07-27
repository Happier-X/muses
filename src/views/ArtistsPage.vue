<template>
  <m-page>
    <template #title>艺术家</template>

    <h-empty
      v-if="artists.length === 0"
      title="还没有艺术家"
      description="请先到音源页添加并扫描音源。"
    />

    <div
      v-else
      class="grid grid-cols-2 gap-[var(--muses-space-lg)] p-[var(--muses-space-lg)] md:grid-cols-[repeat(auto-fill,minmax(180px,1fr))] md:max-w-[var(--muses-content-max-width)] md:mx-auto"
    >
      <article
        v-for="artist in artists"
        :key="artist.name"
        class="flex flex-col gap-[var(--muses-space-sm)] min-w-0"
      >
        <m-cover
          class="!w-full !h-auto aspect-square !flex-none !rounded-full"
          :src="getArtistCoverSrc(artist.songs)"
          alt=""
        />
        <div class="flex flex-col gap-[var(--muses-space-xs)] min-w-0 text-center">
          <h2 class="m-0 text-[length:var(--muses-font-title)] leading-[var(--muses-line-height-title)] text-[color:var(--muses-color-ink)] line-clamp-2">{{ artist.name }}</h2>
          <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--muses-color-ink-muted)] truncate">{{ artist.songCount }} 首歌曲</p>
          <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--muses-color-ink-muted)] truncate">{{ artist.albumCount }} 张专辑</p>
        </div>
      </article>
    </div>
  </m-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Capacitor } from '@capacitor/core'
import { HEmpty, MCover, MPage } from '@/components/ui'
import { loadSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { groupSongsByArtist } from '@/features/library/views'

const songs = ref<SongItem[]>([])
const artists = computed(() => groupSongsByArtist(songs.value))

const refreshSongs = () => {
  songs.value = loadSongs()
}

const getArtistCoverSrc = (artistSongs: SongItem[]): string => {
  const coverUri = artistSongs
    .map((song) => song.coverUri?.trim())
    .find((uri): uri is string => {
      if (!uri) return false
      const normalized = uri.toLowerCase()
      return !normalized.startsWith('data:')
        && !normalized.startsWith('blob:')
        && !normalized.includes(';base64,')
    })

  if (!coverUri) return ''
  const normalizedUri = coverUri.toLowerCase()
  return normalizedUri.startsWith('http://') || normalizedUri.startsWith('https://')
    ? coverUri
    : Capacitor.convertFileSrc(coverUri)
}

onMounted(refreshSongs)
onMounted(refreshSongs)
</script>
