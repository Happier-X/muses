<template>
  <div class="size-full flex items-center justify-center gap-2.5">
    <lew-button
      type="text"
      single-icon
      size="small"
      v-tooltip="{
        content: '上一曲',
      }"
    >
      <PreviousIcon class="size-5" />
    </lew-button>
    <lew-button type="text" single-icon size="large">
      <PauseIcon class="size-6" />
    </lew-button>
    <lew-button
      type="text"
      single-icon
      size="small"
      v-tooltip="{
        content: '下一曲',
      }"
    >
      <NextIcon class="size-5" />
    </lew-button>
  </div>
</template>
<script setup lang="ts">
import {
  //   Play as PlayIcon,
  SkipBack as PreviousIcon,
  SkipForward as NextIcon,
  Pause as PauseIcon,
} from "lucide-vue-next";
// import { usePlayerStore } from '@/stores/player'
// const playerStore = usePlayerStore()
</script>
