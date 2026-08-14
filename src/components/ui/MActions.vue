<template>
  <Teleport to="body">
    <AnimatePresence>
      <template v-if="opened">
        <motion.div
          key="backdrop"
          class="m-overlay-backdrop m-actions-backdrop"
          :initial="{ opacity: 0 }"
          :animate="{ opacity: 1 }"
          :exit="{ opacity: 0 }"
          :transition="{ duration: 0.3 }"
          @click="onBackdropClick"
        />
        <motion.div
          key="panel"
          class="m-actions"
          :initial="{ y: '100%' }"
          :animate="{ y: 0 }"
          :exit="{ y: '100%' }"
          :transition="{ duration: 0.3, ease: [0.32, 0.72, 0, 1] }"
        >
          <slot />
        </motion.div>
      </template>
    </AnimatePresence>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MActions —— 底部操作表（替代 k-actions，iOS）
 * 滑入滑出动画走 motion-v（AnimatePresence）；z 阶梯 1200（index.scss 全局）。
 * backdrop 点击 → backdropclick（页面对接现状）。遮罩层静态样式见 index.scss。
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
/* 面板静态布局（transform 由 motion 接管，仅 y 滑入；水平用 left/right 拉伸居中） */
.m-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  max-width: 28rem;
  margin: 0 auto;
  box-sizing: border-box;
  padding: var(--m-spacing) var(--m-spacing)
    calc(var(--m-spacing) + var(--m-safe-area-bottom, 0px));
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  z-index: 1200;
}
</style>