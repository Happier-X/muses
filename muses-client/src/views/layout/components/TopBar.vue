<template>
  <n-flex class="size-full drag" align="center" justify="space-between">
    <n-flex class="no-drag" align="center" v-if="!isWeb()">
      <n-button quaternary :focusable="false" @click="handleBack">
        <template #icon>
          <n-icon class="i-mingcute-left-line">
          </n-icon>
        </template>
      </n-button>
      <n-button quaternary :focusable="false" @click="handleForward">
        <template #icon>
          <n-icon class="i-mingcute-right-line">
          </n-icon>
        </template>
      </n-button>
    </n-flex>
    <n-flex align="center" class="no-drag" v-if="!isWeb()">
      <n-button quaternary :focusable="false" @click="handleMinimize">
        <template #icon>
          <n-icon class="i-mingcute-minimize-line">
          </n-icon>
        </template>
      </n-button>
      <n-button quaternary :focusable="false" @click="handleToggleWindowSize">
        <template #icon>
          <n-icon :class="isMaximized ? 'i-mingcute-restore-line' : 'i-mingcute-square-line'">
          </n-icon>
        </template>
      </n-button>
      <n-button quaternary :focusable="false" @click="handleClose">
         <template #icon>
          <n-icon class="i-mingcute-close-line">
          </n-icon>
        </template>
      </n-button>
    </n-flex>
  </n-flex>
</template>
<script setup lang="ts">
import { useWindowControl } from '@/composables/useWindowControl'
import { isWeb } from '@/utils/isWeb'
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
