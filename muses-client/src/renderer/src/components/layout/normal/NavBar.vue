<template>
  <div class="size-full flex items-center justify-between px-2 drag">
    <div class="flex items-center gap-1 no-drag">
      <lew-button type="text" single-icon size="small" @click="handleBack">
        <BackIcon class="size-4" />
      </lew-button>
      <lew-button type="text" single-icon size="small" @click="handleForward">
        <ForwardIcon class="size-4" />
      </lew-button>
    </div>
    <div class="flex items-center gap-1 no-drag">
      <lew-button type="text" single-icon size="small" @click="handleMinimize">
        <MinimizeIcon class="size-4" />
      </lew-button>
      <lew-button
        type="text"
        single-icon
        size="small"
        class="scale-x-[-1]"
        @click="handleToggleWindowSize"
      >
        <component :is="isMaximized ? RestoreIcon : MaximizeIcon" class="size-4" />
      </lew-button>
      <lew-button type="text" single-icon size="small" @click="handleClose">
        <CloseIcon class="size-4" />
      </lew-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useWindowControl } from '@renderer/composables/useWindowControl'
import {
  ChevronLeft as BackIcon,
  X as CloseIcon,
  ChevronRight as ForwardIcon,
  Square as MaximizeIcon,
  Minus as MinimizeIcon,
  Copy as RestoreIcon
} from 'lucide-vue-next'
import { useRouter } from 'vue-router'

const { isMaximized, handleMinimize, handleToggleWindowSize, handleClose } = useWindowControl()
const router = useRouter()
const handleBack = () => {
  router.back()
}
const handleForward = () => {
  router.forward()
}
</script>
