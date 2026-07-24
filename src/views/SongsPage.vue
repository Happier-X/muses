<template>
  <ion-page ref="pageRef">
    <h-nav-bar title="歌曲" :fixed="false">
      <template #right>
        <ion-button fill="clear" aria-label="搜索歌曲">
          <h-icon slot="icon-only" :icon="searchOutline" aria-hidden="true" />
        </ion-button>
      </template>
    </h-nav-bar>
    <div class="shuffle-bar">
      <div class="shuffle-actions tablet-content-limit">
        <h-button
          variant="ghost"
          size="sm"
          class="shuffle-all-button"
          aria-label="随机播放全部"
          :disabled="songs.length === 0"
          @click="onShuffleAll"
        >
          <template #leading><h-icon :icon="shuffle" /></template>
          随机播放全部
        </h-button>
      </div>
    </div>
    <ion-content :fullscreen="false" class="songs-content">
      <h-empty
        v-if="songs.length === 0"
        title="还没有歌曲"
        description="请先到音源页添加并扫描音源。"
      />

      <div v-else ref="listParentRef" class="song-list list-grid tablet-content-limit">
        <div class="song-list-spacer" :style="{ height: `${totalSize}px` }">
          <div
            v-for="virtualRow in virtualRows"
            :key="songs[virtualRow.index].id"
            :ref="measureVirtualRow"
            class="song-row"
            :data-index="virtualRow.index"
            :style="{ transform: `translateY(${virtualRow.start}px)` }"
          >
            <ion-item
              button
              :detail="false"
              lines="none"
              class="song-item"
              :class="{ 'is-playing': playerState.currentSong?.id === songs[virtualRow.index].id }"
              :data-song-id="songs[virtualRow.index].id"
              @click="playSong(songs[virtualRow.index])"
            >
              <m-cover slot="start" :src="getSongCoverSrc(songs[virtualRow.index])" alt="" />
              <ion-label>
                <h2>{{ songs[virtualRow.index].title }}</h2>
                <p>{{ getSongArtistName(songs[virtualRow.index]) }} - {{ getSongAlbumName(songs[virtualRow.index]) }}</p>
              </ion-label>
              <ion-button
                slot="end"
                fill="clear"
                class="more-button"
                aria-label="更多歌曲操作"
                @click.stop="openSongActions(songs[virtualRow.index])"
              >
                <h-icon :icon="ellipsisVertical" />
              </ion-button>
            </ion-item>
          </div>
        </div>
      </div>

      <ion-action-sheet
        :is-open="isSongActionsOpen"
        header="歌曲操作"
        :buttons="songActionButtons"
        @didDismiss="isSongActionsOpen = false"
      />

      <ion-action-sheet
        :is-open="isPlaylistPickOpen"
        header="加入歌单"
        :buttons="playlistPickButtons"
        @didDismiss="isPlaylistPickOpen = false"
      />

      <ion-alert
        :is-open="isCreatePlaylistOpen"
        header="新建歌单"
        :inputs="createPlaylistInputs"
        :buttons="createPlaylistButtons"
        @didDismiss="isCreatePlaylistOpen = false"
      />

      <ion-fab
        v-if="currentPlayingInList"
        vertical="bottom"
        horizontal="end"
        slot="fixed"
        class="jump-current-fab"
      >
        <ion-fab-button
          aria-label="跳转到当前播放"
          @click="scrollToCurrentSong"
        >
          <h-icon :icon="locateOutline" aria-hidden="true" />
        </ion-fab-button>
      </ion-fab>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, type ComponentPublicInstance } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { Capacitor } from '@capacitor/core'
import {
  IonActionSheet,
  IonAlert,
  IonButton,
  IonContent,
  IonFab,
  IonFabButton,
  IonItem,
  IonLabel,
  IonPage,
  onIonViewWillEnter,
  type ActionSheetButton,
  type AlertButton,
  type AlertInput,
} from '@ionic/vue'
import { ellipsisVertical, locateOutline, searchOutline, shuffle } from '@/icons'
import { HButton, HEmpty, HIcon, HNavBar, MCover } from '@/components/ui'
import { loadSongs, SONGS_UPDATED_EVENT } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { getSongAlbumName, getSongArtistName, sortSongsForDisplay } from '@/features/library/views'
import {
  addSongToPlaylist,
  createPlaylist,
  loadPlaylists,
} from '@/features/playlist'
import {
  clearQueue,
  enqueueSong,
  enqueueSongs,
  playerState,
  playSong,
  selectSongAtIndex,
  shuffleEnabled,
  toggleShuffle,
} from '@/features/player/controller'

const songs = ref<SongItem[]>([])
const pageRef = ref<HTMLElement | { $el?: HTMLElement } | null>(null)
const listParentRef = ref<HTMLElement | null>(null)
const actionSong = ref<SongItem | null>(null)
const isSongActionsOpen = ref(false)
const isPlaylistPickOpen = ref(false)
const isCreatePlaylistOpen = ref(false)
let jumpHighlightTimer: ReturnType<typeof setTimeout> | null = null

/** 大曲库只渲染可视行，降低滚动/卡顿（#50） */
const rowVirtualizer = useVirtualizer(
  computed(() => ({
    count: songs.value.length,
    getScrollElement: () => listParentRef.value,
    estimateSize: () => 72,
    overscan: 8,
  })),
)

const measureVirtualRow = (element: Element | ComponentPublicInstance | null): void => {
  rowVirtualizer.value.measureElement(element instanceof HTMLElement ? element : null)
}

const virtualRows = computed(() => {
  const items = rowVirtualizer.value.getVirtualItems()
  if (items.length > 0) {
    return items
  }
  // 无滚动容器时（首帧 / 单测 stub）退化为全量行，避免空白与测试失败
  return songs.value.map((_, index) => ({
    index,
    start: index * 72,
    size: 72,
    end: (index + 1) * 72,
    key: index,
  }))
})
const totalSize = computed(() => {
  const measured = rowVirtualizer.value.getTotalSize()
  if (measured > 0) {
    return measured
  }
  return songs.value.length * 72
})

const currentPlayingInList = computed(() => {
  const currentId = playerState.currentSong?.id
  if (!currentId) {
    return false
  }
  return songs.value.some((song) => song.id === currentId)
})

const refreshSongs = () => {
  songs.value = sortSongsForDisplay(loadSongs())
}

const openSongActions = (song: SongItem) => {
  actionSong.value = song
  isSongActionsOpen.value = true
}

const songActionButtons = computed<ActionSheetButton[]>(() => [
  {
    text: '添加到队列',
    handler: () => {
      if (actionSong.value) {
        enqueueSong(actionSong.value)
      }
    },
  },
  {
    text: '加入歌单…',
    handler: () => {
      // 等主 sheet 关闭后再开，避免叠层冲突
      window.setTimeout(() => {
        isPlaylistPickOpen.value = true
      }, 180)
    },
  },
  { text: '取消', role: 'cancel' },
])

const playlistPickButtons = computed<ActionSheetButton[]>(() => {
  const list = loadPlaylists().slice().sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
  const buttons: ActionSheetButton[] = list.map((playlist) => ({
    text: playlist.name,
    handler: () => {
      if (actionSong.value) {
        addSongToPlaylist(playlist.id, actionSong.value.id)
      }
    },
  }))
  buttons.push({
    text: '新建歌单',
    handler: () => {
      window.setTimeout(() => {
        isCreatePlaylistOpen.value = true
      }, 180)
    },
  })
  buttons.push({ text: '取消', role: 'cancel' })
  return buttons
})

const createPlaylistInputs: AlertInput[] = [
  {
    name: 'name',
    type: 'text',
    placeholder: '歌单名称',
    attributes: { maxlength: 80 },
  },
]

const createPlaylistButtons = computed<AlertButton[]>(() => [
  { text: '取消', role: 'cancel' },
  {
    text: '创建并加入',
    handler: (data: { name?: string }) => {
      const name = typeof data?.name === 'string' ? data.name : ''
      const created = createPlaylist(name)
      if (!created || !actionSong.value) {
        return false
      }
      addSongToPlaylist(created.id, actionSong.value.id)
      return true
    },
  },
])

const onShuffleAll = () => {
  if (songs.value.length === 0) {
    return
  }

  clearQueue()
  enqueueSongs(songs.value)
  if (!shuffleEnabled()) {
    toggleShuffle()
  }
  const first = selectSongAtIndex(0)
  if (first) {
    void playSong(first)
  }
}

const resolvePageRoot = (): ParentNode | null => {
  const value = pageRef.value
  if (!value) {
    return typeof document !== 'undefined' ? document : null
  }
  if (value instanceof HTMLElement) {
    return value
  }
  if (value.$el instanceof HTMLElement) {
    return value.$el
  }
  return typeof document !== 'undefined' ? document : null
}

const findSongRow = (songId: string): HTMLElement | null => {
  const root = resolvePageRoot()
  if (!root) {
    return null
  }

  const rows = Array.from(root.querySelectorAll<HTMLElement>('[data-song-id]'))
  return rows.find((row) => row.getAttribute('data-song-id') === songId) ?? null
}

const scrollToCurrentSong = async () => {
  const currentId = playerState.currentSong?.id
  if (!currentId) {
    return
  }

  const index = songs.value.findIndex((song) => song.id === currentId)
  if (index < 0) {
    return
  }

  // 虚拟列表：先滚到索引，再高亮 DOM 行
  rowVirtualizer.value.scrollToIndex(index, { align: 'start', behavior: 'smooth' })
  await nextTick()
  // 等 layout 一帧，确保目标行已挂载
  await new Promise<void>((resolve) => {
    requestAnimationFrame(() => resolve())
  })

  const row = findSongRow(currentId)
  if (!row) {
    return
  }

  // 保留既有 scrollIntoView 合约（stub/首帧 virtualizer 尚未拿到容器时也可跳转）
  const scrollableRow = row as HTMLElement & { scrollIntoView?: (options?: ScrollIntoViewOptions) => void }
  scrollableRow.scrollIntoView?.({ behavior: 'smooth', block: 'start', inline: 'nearest' })
  row.classList.add('jump-highlight')
  if (jumpHighlightTimer) {
    clearTimeout(jumpHighlightTimer)
  }
  jumpHighlightTimer = setTimeout(() => {
    row.classList.remove('jump-highlight')
    jumpHighlightTimer = null
  }, 1200)
}

const toDisplayableUri = (uri: string): string => {
  const normalizedUri = uri.trim().toLowerCase()
  if (!uri || normalizedUri.startsWith('data:') || normalizedUri.startsWith('blob:') || normalizedUri.includes(';base64,')) {
    return ''
  }

  return normalizedUri.startsWith('http://') || normalizedUri.startsWith('https://')
    ? uri
    : Capacitor.convertFileSrc(uri)
}

const getSongCoverSrc = (song: SongItem): string => {
  return song.coverUri ? toDisplayableUri(song.coverUri) : ''
}

onMounted(() => {
  refreshSongs()
  if (typeof window !== 'undefined') {
    window.addEventListener(SONGS_UPDATED_EVENT, refreshSongs)
  }
})

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener(SONGS_UPDATED_EVENT, refreshSongs)
  }
  if (jumpHighlightTimer) {
    clearTimeout(jumpHighlightTimer)
    jumpHighlightTimer = null
  }
})

onIonViewWillEnter(refreshSongs)
</script>

<style scoped>
/* 列表自管 padding-bottom；content 不再重复加底内边距 */
.songs-content {
  --padding-bottom: 0;
}

.shuffle-bar {
  flex: 0 0 48px;
  min-height: 48px;
  background: var(--muses-color-surface);
}

.shuffle-actions {
  box-sizing: border-box;
  width: 100%;
  padding: 4px 8px;
}

.shuffle-all-button {
  margin: 0;
}

/* 自建滚动容器：虚拟列表需要固定高度 + overflow（#50） */
.song-list {
  height: 100%;
  overflow: auto;
  box-sizing: border-box;
  /* 仅为 MiniPlayer 与 Tab Bar 预留滚动空间 */
  padding-bottom: calc(var(--muses-tab-bar-height) + var(--muses-mini-player-height) + var(--ion-safe-area-bottom, 0px));
}

.song-list-spacer {
  position: relative;
  width: 100%;
}

.song-row {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  box-sizing: border-box;
  min-height: var(--muses-song-row-height);
  /* 避开 navbar 与随机播放操作区，scrollIntoView block=start 时标题完整可见 */
  scroll-margin-top: 108px;
}

/* 当前播放行：替代已删除 HListRow 的 playing 背景。 */
:deep(.song-item.is-playing) {
  --background: var(--muses-color-playing-bg);
  background: var(--muses-color-playing-bg);
}

/* 跳转高亮：class 挂在 Ionic 列表行根节点。 */
:deep(.song-item.jump-highlight) {
  background: var(--muses-color-jump-highlight);
}

/* 避开底部 Tab Bar（~64）+ MiniPlayer（~64）+ 间距，保留安全区 */
.jump-current-fab {
  bottom: calc(144px + var(--ion-safe-area-bottom, 0px));
  right: 12px;
}

@media (min-width: 768px) {
  .jump-current-fab {
    /* 宽屏无底部 Tab Bar，仍避开 MiniPlayer */
    bottom: calc(80px + var(--ion-safe-area-bottom, 0px));
  }

  /* 歌曲页宽屏始终单列，仅保留内容最大宽度居中 */
  .list-grid {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }

  .song-list {
    padding-bottom: calc(var(--muses-mini-player-height) + var(--ion-safe-area-bottom, 0px));
  }

  .songs-content {
    --padding-bottom: 0;
  }

  .shuffle-actions {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
</style>
