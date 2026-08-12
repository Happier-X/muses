<template>
  <AnimatePresence>
    <motion.div
      v-if="opened"
      key="toast"
      class="m-toast"
      :class="`m-toast--${position}`"
      :initial="{ y: '100%', opacity: 0 }"
      :animate="{ y: 0, opacity: 1 }"
      :exit="{ y: '100%', opacity: 0 }"
      :transition="{ duration: 0.3, ease: [0.32, 0.72, 0, 1] }"
      role="status"
      aria-live="polite"
    >
      <div class="m-toast__inner">
        <slot />
      </div>
    </motion.div>
  </AnimatePresence>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MToast —— 轻提示（替代 k-toast，iOS）
 * 底部安全区上浮胶囊（黑字/白字由主题变量决定）；position 控制水平对齐
 * （left/center/right，与 PlayerPage 现状一致）。
 * 关闭时机由调用方 opened 控制（现页面自管 timeout，不内建）。
 */
import { AnimatePresence, motion } from 'motion-v'
import { onBeforeUnmount, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    opened?: boolean
    position?: 'left' | 'center' | 'right'
    /** 自动关闭毫秒（0 = 不自动关闭） */
    duration?: number
  }>(),
  {
    opened: false,
    position: 'center',
    duration: 0,
  },
)

const emit = defineEmits<{
  close: []
}>()

let timer: ReturnType<typeof setTimeout> | null = null

watch(
  () => props.opened,
  (opened) => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    if (opened && props.duration > 0) {
      timer = setTimeout(() => emit('close'), props.duration)
    }
  },
)

onBeforeUnmount(() => {
  if (timer) clearTimeout(timer)
})
</script>

<style scoped lang="scss">
.m-toast {
  position: fixed;
  left: 0;
  right: 0;
  bottom: calc(16px + var(--m-safe-area-bottom, 0px));
  display: flex;
  width: auto;
  z-index: 1300;
  padding: 0 calc(16px + var(--m-safe-area-left, 0px)) 0
    calc(16px + var(--m-safe-area-right, 0px));
  box-sizing: border-box;
  pointer-events: none;

  &--left {
    justify-content: flex-start;
  }

  &--right {
    justify-content: flex-end;
  }

  &--center {
    justify-content: center;
  }

  &__inner {
    max-width: 32rem;
    border-radius: 16px;
    padding: 12px 16px;
    font-size: 14px;
    line-height: 1.4;
    color: #000;
    /* 白玻璃（iOS toast 同款） */
    background-color: rgba(255, 255, 255, 0.75);
    -webkit-backdrop-filter: blur(16px);
    backdrop-filter: blur(16px);
    box-shadow: var(--m-shadow-ios-light-glass);
  }
}

:global(.dark) .m-toast__inner {
  color: #fff;
  background-color: rgba(50, 50, 50, 0.5);
  box-shadow: var(--m-shadow-ios-dark-glass);
}
</style>