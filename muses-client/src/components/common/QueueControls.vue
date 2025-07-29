<template>
  <div class="size-full flex items-center justify-end gap-2.5">
    <lew-button type="text" single-icon size="small">
      <ListLoopIcon class="size-5" />
    </lew-button>
    <lew-button type="text" single-icon size="small">
      <OrderPlayIcon class="size-5" />
    </lew-button>
    <lew-button
      type="text"
      single-icon
      size="small"
      v-tooltip="{
        content: '播放队列',
      }"
    >
      <PlayQueueIcon class="size-5" />
    </lew-button>
  </div>
</template>
<script setup lang="ts">
import {
  Repeat as ListLoopIcon,
  Repeat1 as SingleLoopIcon,
  ListOrdered as OrderPlayIcon,
  Shuffle as ShufflePlayIcon,
  ListMusic as PlayQueueIcon,
} from "lucide-vue-next";
// import { usePlayerStore } from '@/stores/player'
// const playerStore = usePlayerStore()
</script>
