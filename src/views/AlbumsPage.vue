<template>
  <m-page>
    <template #title>专辑</template>

    <h-empty
      v-if="albums.length === 0"
      title="还没有专辑"
      description="请先到音源页添加并扫描音源。"
    />

    <div
      v-else
      class="grid grid-cols-2 gap-[var(--muses-space-lg)] p-[var(--muses-space-lg)] md:grid-cols-[repeat(auto-fill,minmax(180px,1fr))] md:max-w-[var(--muses-content-max-width)] md:mx-auto"
    >
      <article
        v-for="album in albums"
        :key="album.name"
        class="flex flex-col gap-[var(--muses-space-sm)] min-w-0 cursor-pointer active:opacity-80"
        role="button"
        tabindex="0"
        @click="openAlbum(album.name)"
      >
        <m-cover class="!w-full !h-auto aspect-square !flex-none" :src="getAlbumCoverSrc(album.songs)" alt="" />
        <div class="flex flex-col gap-[var(--muses-space-xs)] min-w-0">
          <h2 class="m-0 text-[length:var(--muses-font-title)] leading-[var(--muses-line-height-title)] text-[color:var(--muses-color-ink)] line-clamp-2">{{ album.name }}</h2>
          <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--muses-color-ink-muted)] truncate">{{ album.songCount }} 首歌曲</p>
          <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--muses-color-ink-muted)] truncate">{{ album.artistSummary }}</p>
        </div>
      </article>
    </div>
  </m-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import { MCover, HEmpty, MPage } from '@/components/ui'
import { loadSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { groupSongsByAlbum } from '@/features/library/views'

const router = useRouter()

const openAlbum = (name: string): void => {
  void router.push(`/tabs/library/album/${encodeURIComponent(name)}`)
}

const songs = ref<SongItem[]>([])
const albums = computed(() => groupSongsByAlbum(songs.value))

const refreshSongs = () => {
  songs.value = loadSongs()
}

const getAlbumCoverSrc = (albumSongs: SongItem[]): string => {
  const coverUri = albumSongs.find((song) => song.coverUri)?.coverUri
  if (!coverUri) return ''
  const normalizedUri = coverUri.trim().toLowerCase()
  if (normalizedUri.startsWith('data:') || normalizedUri.startsWith('blob:') || normalizedUri.includes(';base64,')) return ''
  return normalizedUri.startsWith('http://') || normalizedUri.startsWith('https://')
    ? coverUri
    : Capacitor.convertFileSrc(coverUri)
}

onMounted(refreshSongs)
onMounted(refreshSongs)
</script>
