<template>
  <k-popup :opened="queueOverlayVisible">
    <k-page class="queue-popup-panel flex flex-col overflow-hidden">
      <div class="flex-none flex items-center justify-between gap-[8px] px-[12px] pt-[12px] pb-[8px]">
        <h2 class="m-0 text-[17px] leading-[1.3] font-bold text-black dark:text-white">播放队列</h2>
        <div class="flex items-center gap-[2px]">
          <k-button
            v-if="queueState.hasItems"
            component="button"
            clear
            rounded
            class="size-8"
            :colors="{ textIos: 'text-[#ff3b30]', clearBgIos: 'bg-transparent active:bg-[#ff3b30]/15' }"
            aria-label="清空队列"
            @click="onClearQueue"
          >
            <component :is="trash" aria-hidden="true" class="size-4" />
          </k-button>
          <k-button
            component="button"
            clear
            rounded
            class="size-8"
            aria-label="关闭队列"
            @click="goBack"
          >
            <component :is="close" aria-hidden="true" class="size-4" />
          </k-button>
        </div>
      </div>

      <div class="flex-1 min-h-0 overflow-hidden">
        <m-empty
          v-if="!queueState.hasItems"
          title="队列为空"
          description="从歌曲列表中添加歌曲即可开始播放。"
        />

        <div v-else ref="listParentRef" class="h-full overflow-auto overscroll-contain box-border pb-safe-6 [overflow-anchor:none]" role="list" aria-label="播放队列歌曲" @touchstart.stop @touchmove.stop @touchend.stop @touchcancel.stop>
          <k-list strong-ios outline-ios class="!my-0">
          <div class="relative w-full" :style="{ height: `${totalSize}px` }">>
            <div
              v-for="row in visibleRows"
              :key="row.song.id"
              :ref="measureVirtualRow"
              class="absolute inset-x-0 top-0 box-border min-h-[72px]"
              role="listitem"
              :data-index="row.virtualRow.index"
              :style="{ transform: `translateY(${row.virtualRow.start}px)` }"
            >
              <k-list-item
                :chevron="false"
                link
                :title="row.song.title"
                :subtitle="row.song.artist || '未知歌手'"
                titleClass="min-w-0 truncate"
                subtitleClass="truncate"
                class="h-full"
                :class="row.virtualRow.index === queueState.currentIndex ? 'is-playing bg-black/5 dark:bg-white/10' : ''"
                :aria-current="row.virtualRow.index === queueState.currentIndex ? 'true' : undefined"
                role="button"
                tabindex="0"
                @click="onSelectSong(row.virtualRow.index)"
              >
                <template #after>
                  <span class="flex-none text-[length:12px] opacity-60 me-[2px]">{{ row.virtualRow.index + 1 }}</span>
                  <k-button
                    component="button"
                    clear
                    rounded
                    class="flex-none m-0 size-8"
                    :colors="{ textIos: 'text-[#ff3b30]', clearBgIos: 'bg-transparent active:bg-[#ff3b30]/15' }"
                    :aria-label="`从队列删除 ${row.song.title}`"
                    @click.stop="onRemoveSong(row.song.id)"
                  >
                    <component :is="close" aria-hidden="true" class="size-4" />
                  </k-button>
                </template>
              </k-list-item>
            </div>
          </div>
        </k-list>
        </div>
      </div>
    </k-page>
  </k-popup>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, type ComponentPublicInstance, watch } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import { close, trash } from '@/icons'
import { kButton, kList, kListItem, kPage, kPopup, MEmpty } from '@/components/ui'
import {
  clearQueue,
  playSong,
  queueState,
  removeSongFromQueue,
  selectSongAtIndex,
} from '@/features/player/controller'
import { closeQueueOverlay, queueOverlayVisible } from '@/features/player/overlay'

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
  if (
    !listParentRef.value
    || queueState.currentIndex < 0
    || queueState.currentIndex >= queueState.items.length
  ) {
    return
  }
  rowVirtualizer.value.scrollToIndex(queueState.currentIndex, { align: 'center' })
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

const onSelectSong = async (index: number): Promise<void> => {
  const song = selectSongAtIndex(index)
  if (song) {
    await playSong(song)
  }
}
</script>
