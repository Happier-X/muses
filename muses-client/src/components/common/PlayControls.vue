<template>
  <div class="size-full flex items-center justify-center gap-2.5">
    <el-button text size="large" class="ml-0! rounded-lg!" circle>
      <PreviousIcon class="size-5" />
    </el-button>
    <el-button text size="large" class="ml-0! rounded-lg!" circle >
      <PauseIcon class="size-5" />
    </el-button>
    <el-button text size="large" class="ml-0! rounded-lg!" circle>
      <NextIcon class="size-5" />
    </el-button>
  </div>
</template>
<script setup lang="ts">
import {
  //   Play as PlayIcon,
  SkipBack as PreviousIcon,
  SkipForward as NextIcon,
  Pause as PauseIcon
} from 'lucide-vue-next'
// import { usePlayerStore } from '@/stores/player'
// const playerStore = usePlayerStore()
</script>
