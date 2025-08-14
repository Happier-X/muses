<template>
  <div class="size-full flex items-center justify-end gap-2.5">
    <n-button text size="large" class="ml-0! rounded-lg!" circle>
      <ListLoopIcon class="size-5" />
    </n-button>
    <n-button text size="large" class="ml-0! rounded-lg!" circle>
      <OrderPlayIcon class="size-5" />
    </n-button>
    <n-button text size="large" class="ml-0! rounded-lg!" circle>
      <PlayQueueIcon class="size-5" />
    </n-button>
  </div>
</template>
<script setup lang="ts">
import {
  Repeat as ListLoopIcon,
  Repeat1 as SinglnoopIcon,
  ListOrdered as OrderPlayIcon,
  Shuffle as ShufflePlayIcon,
  ListMusic as PlayQueueIcon
} from 'lucide-vue-next'
// import { usePlayerStore } from '@/stores/player'
// const playerStore = usePlayerStore()
</script>
