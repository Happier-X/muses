<template>
  <div class="m-page artists-page">
    <div class="artists-page__navbar-wrap">
      <m-navbar>
        <template #title>艺术家</template>
      </m-navbar>
    </div>
    <div class="m-content artists-page__content">
      <m-empty
        v-if="artists.length === 0"
        title="还没有艺术家"
        description="请先到音源页添加并扫描音源。"
        :icon="person"
      />

      <div v-else class="artists-page__grid">
        <article
          v-for="artist in artists"
          :key="artist.name"
          class="artists-page__card"
          role="button"
          tabindex="0"
          @click="openArtist(artist.name)"
        >
          <m-cover class="artists-page__cover" :src="getArtistCoverSrc(artist.songs)" alt="" />
          <div class="artists-page__info">
            <h2 class="artists-page__title">{{ artist.name }}</h2>
            <p class="artists-page__meta">{{ artist.songCount }} 首歌曲</p>
            <p class="artists-page__meta">{{ artist.albumCount }} 张专辑</p>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { person } from '@/icons'
import { useRouter } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import { MEmpty, MCover, MNavbar } from '@/components/ui'
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
</script>

<style scoped lang="scss">
.artists-page {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;

  &__navbar-wrap {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    z-index: 20;
  }

  &__content {
    padding-top: calc(max(16px, var(--m-safe-area-top, 0px)) + 44px);
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    padding: 16px 16px var(--m-content-pb);

    @media (min-width: 768px) {
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      padding-bottom: var(--m-content-pb-md);
    }
  }

  &__card {
    display: flex;
    flex-direction: column;
    gap: var(--m-spacing-sub);
    padding: var(--m-spacing-sub);
    border-radius: var(--m-radius-card);
    background-color: var(--m-surface-1);
    min-width: 0;
    cursor: pointer;

    &:active {
      background-color: var(--m-surface-2);
    }
  }

  &__cover {
    width: 100% !important;
    height: auto !important;
    flex: none;
    aspect-ratio: 1;
    border-radius: 50% !important;
  }

  &__info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    text-align: center;
  }

  &__title {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
    line-height: 1.3;
    color: var(--m-text);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__meta {
    margin: 0;
    font-size: 13px;
    line-height: 1.35;
    color: var(--m-text-2);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>