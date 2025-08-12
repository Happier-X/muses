<template>
  <div class="size-full flex items-center justify-between px-2 drag">
    <div class="flex items-center gap-1 no-drag" v-if="!isWeb()">
      <el-button text @click="handleBack" class="ml-0! rounded-lg!" circle>
        <BackIcon class="size-4" />
      </el-button>
      <el-button text @click="handleForward" class="ml-0! rounded-lg!" circle>
        <ForwardIcon class="size-4" />
      </el-button>
    </div>
    <div class="flex items-center gap-1 no-drag" v-if="!isWeb()">
      <el-button text @click="handleMinimize" class="ml-0! rounded-lg!" circle>
        <MinimizeIcon class="size-4" />
      </el-button>
      <el-button text @click="handleToggleWindowSize" class="scale-x-[-1] ml-0! rounded-lg!" circle>
        <component :is="isMaximized ? RestoreIcon : MaximizeIcon" class="size-4" />
      </el-button>
      <el-button text @click="handleClose" class="ml-0! rounded-lg!" circle>
        <CloseIcon class="size-4" />
      </el-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useWindowControl } from '@/composables/useWindowControl'
import { isWeb } from '@/utils/isWeb'
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
