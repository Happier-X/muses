<template>
  <m-page>
    <template #title>艺术家</template>

    <m-empty
      v-if="artists.length === 0"
      title="还没有艺术家"
      description="请先到音源页添加并扫描音源。"
      :icon="person"
    />

    <div
      v-else
      class="grid grid-cols-2 gap-[16px] px-[16px] pt-[16px] pb-[var(--content-pb)] md:pb-[var(--content-pb-md)] md:grid-cols-[repeat(auto-fill,minmax(180px,1fr))] "
    >
      <article
        v-for="artist in artists"
        :key="artist.name"
        class="flex flex-col gap-[8px] min-w-0 cursor-pointer active:opacity-80"
        role="button"
        tabindex="0"
        @click="openArtist(artist.name)"
      >
        <m-cover
          class="!w-full !h-auto aspect-square !flex-none !rounded-full"
          :src="getArtistCoverSrc(artist.songs)"
          alt=""
        />
        <div class="flex flex-col gap-[2px] min-w-0 text-center">
          <h2 class="m-0 text-[17px] font-semibold leading-[1.3] text-black dark:text-white line-clamp-2">{{ artist.name }}</h2>
          <p class="m-0 text-[13px] text-black/55 dark:text-white/55 truncate">{{ artist.songCount }} 首歌曲</p>
          <p class="m-0 text-[13px] text-black/55 dark:text-white/55 truncate">{{ artist.albumCount }} 张专辑</p>
        </div>
      </article>
    </div>
  </m-page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { person } from '@/icons'
import { useRouter } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import { MEmpty, MCover, MPage } from '@/components/ui'
import { loadSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { groupSongsByArtist } from '@/features/library/views'

const router = useRouter()

const openArtist = (name: string): void => {
  void router.push(`/tabs/library/artist/${encodeURIComponent(name)}`)
}

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
