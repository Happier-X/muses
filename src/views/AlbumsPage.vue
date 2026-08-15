<template>
  <div class="m-page albums-page">
    <div class="albums-page__navbar-wrap">
      <m-navbar>
        <template #title>专辑</template>
      </m-navbar>
    </div>
    <div class="m-content albums-page__content">
      <m-empty
        v-if="albums.length === 0"
        title="还没有专辑"
        description="请先到音源页添加并扫描音源。"
        :icon="albumsIcon"
      />

      <div v-else class="albums-page__grid">
        <article
          v-for="album in albums"
          :key="album.name"
          class="albums-page__card"
          role="button"
          tabindex="0"
          @click="openAlbum(album.name)"
        >
          <m-cover class="albums-page__cover" :src="getAlbumCoverSrc(album.songs)" alt="" />
          <div class="albums-page__info">
            <h2 class="albums-page__title">{{ album.name }}</h2>
            <p class="albums-page__meta">{{ album.songCount }} 首歌曲</p>
            <p class="albums-page__meta">{{ album.artistSummary }}</p>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { albums as albumsIcon } from '@/icons'
import { useRouter } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import { MCover, MEmpty, MNavbar } from '@/components/ui'
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
</script>

<style scoped lang="scss">
.albums-page {
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
    padding-top: calc(var(--m-navbar-pt, 16px) + 44px);
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

  /* 封面撑满列宽且等比（覆盖 MCover 固定尺寸） */
  &__cover {
    width: 100% !important;
    height: auto !important;
    flex: none;
    aspect-ratio: 1;
  }

  &__info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
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