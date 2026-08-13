<template>
  <AnimatePresence>
    <template v-if="opened">
      <motion.div
        key="backdrop"
        class="m-overlay-backdrop m-sheet-backdrop"
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :exit="{ opacity: 0 }"
        :transition="{ duration: 0.3 }"
        @click="onBackdropClick"
      />
      <motion.div
        key="panel"
        class="m-sheet"
        :initial="{ y: '100%' }"
        :animate="{ y: 0 }"
        :exit="{ y: '100%' }"
        :transition="{ duration: 0.4, ease: [0.32, 0.72, 0, 1] }"
      >
        <slot />
      </motion.div>
    </template>
  </AnimatePresence>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MSheet —— 底部滑出面板（替代 k-sheet，iOS）
 * 底部贴齐 + 32px 大上圆角 + surface-1 底；motion-v 滑入滑出（y 0↔100%）。
 * z 阶梯 1200。内容 padding 由调用方控制（与现状一致）。
 */
import { AnimatePresence, motion } from 'motion-v'

withDefaults(
  defineProps<{
    opened?: boolean
  }>(),
  {
    opened: false,
  },
)

const emit = defineEmits<{
  backdropclick: [event: MouseEvent]
}>()

function onBackdropClick(event: MouseEvent) {
  emit('backdropclick', event)
}
</script>

<style scoped lang="scss">
.m-sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  box-sizing: border-box;
  border-radius: var(--m-radius-lg) var(--m-radius-lg) 0 0;
  overflow: hidden;
  background-color: var(--m-surface-1);
  color: var(--m-text);
  z-index: 1200;
}
</style>