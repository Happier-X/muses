<template>
  <div class="fixed inset-0 z-[1200] flex flex-col overflow-hidden overscroll-none bg-[var(--muses-color-surface)] dark:bg-[var(--muses-color-surface-dark)]">
    <h-nav-bar
      title="播放队列"
      show-back
      back-aria-label="返回"
      :fixed="false"
      @handle-left-click="goBack"
    >
      <template v-if="queueState.hasItems" #right>
        <h-button variant="danger-soft" is-icon-only shape="square" aria-label="清空队列" @click="onClearQueue">
          <h-icon :icon="trash" />
        </h-button>
      </template>
    </h-nav-bar>

    <div class="flex-1 min-h-0 overflow-hidden">
      <h-empty
        v-if="!queueState.hasItems"
        title="队列为空"
        description="从歌曲列表中添加歌曲即可开始播放。"
      />

      <div v-else ref="listParentRef" class="h-full overflow-auto overscroll-contain box-border pb-[calc(var(--muses-mini-player-height)+var(--muses-space-xl)+env(safe-area-inset-bottom,0px))]" role="list" aria-label="播放队列歌曲">
        <div class="relative w-full" :style="{ height: `${totalSize}px` }">
          <div
            v-for="row in visibleRows"
            :key="row.song.id"
            :ref="measureVirtualRow"
            class="absolute inset-x-0 top-0 box-border min-h-[var(--muses-song-row-height)]"
            role="listitem"
            :data-index="row.virtualRow.index"
            :style="{ transform: `translateY(${row.virtualRow.start}px)` }"
          >
            <div
              class="flex items-center gap-[var(--muses-space-md)] p-[var(--muses-space-md)]"
              :class="row.virtualRow.index === queueState.currentIndex ? 'is-playing bg-[var(--muses-color-playing-bg)]' : ''"
              :aria-current="row.virtualRow.index === queueState.currentIndex ? 'true' : undefined"
              role="button"
              tabindex="0"
              @click="onSelectSong(row.virtualRow.index, $event)"
            >
              <div class="flex-1 min-w-0 flex flex-col gap-[var(--muses-space-xs)]">
                <h2 class="m-0 text-[length:var(--muses-font-title)] leading-[var(--muses-line-height-title)] text-[color:var(--muses-color-ink)] truncate">{{ row.song.title }}</h2>
                <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--muses-color-ink-muted)] truncate">{{ row.song.artist || '未知歌手' }}</p>
              </div>
              <span class="flex-none text-[length:var(--muses-font-label)] opacity-60 me-[var(--muses-space-xs)]">{{ row.virtualRow.index + 1 }}</span>
              <h-button
                variant="danger-soft"
                is-icon-only
                shape="square"
                class="remove-button flex-none m-0"
                :aria-label="`从队列删除 ${row.song.title}`"
                @click.stop="onRemoveSong(row.song.id)"
              >
                <h-icon :icon="close" />
              </h-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, type ComponentPublicInstance, watch } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { close, trash } from '@/icons'
import { HEmpty, HIcon, HNavBar } from '@/components/ui'
import {
  clearQueue,
  playSong,
  queueState,
  removeSongFromQueue,
  selectSongAtIndex,
} from '@/features/player/controller'
import { closeQueueOverlay } from '@/features/player/overlay'

const listParentRef = ref<HTMLElement | null>(null)

const rowVirtualizer = useVirtualizer(
  computed(() => ({
    count: queueState.items.length,
    getScrollElement: () => listParentRef.value,
    estimateSize: () => 72,
    overscan: 8,
  })),
)

const visibleRows = computed(() => rowVirtualizer.value.getVirtualItems().flatMap((virtualRow) => {
  const song = queueState.items[virtualRow.index]
  return song ? [{ virtualRow, song }] : []
}))
const totalSize = computed(() => rowVirtualizer.value.getTotalSize())

const measureVirtualRow = (element: Element | ComponentPublicInstance | null): void => {
  rowVirtualizer.value.measureElement(element instanceof HTMLElement ? element : null)
}

const scrollToCurrent = async (): Promise<void> => {
  await nextTick()
  if (queueState.currentIndex >= 0 && queueState.currentIndex < queueState.items.length) {
    rowVirtualizer.value.scrollToIndex(queueState.currentIndex, { align: 'center' })
  }
}

watch(
  [listParentRef, () => queueState.items.length, () => queueState.currentIndex],
  () => void scrollToCurrent(),
  { flush: 'post', immediate: true },
)

const goBack = () => {
  closeQueueOverlay()
}

const onClearQueue = () => {
  clearQueue()
}

const onRemoveSong = (songId: string) => {
  removeSongFromQueue(songId)
}

const onSelectSong = async (index: number, event: MouseEvent | KeyboardEvent): Promise<void> => {
  if (event.composedPath().some((target) => target instanceof Element && target.classList.contains('remove-button'))) {
    return
  }
  const song = selectSongAtIndex(index)
  if (song) {
    await playSong(song)
  }
}
</script>
