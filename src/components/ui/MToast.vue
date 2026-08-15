<template>
  <Teleport to="body">
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
  </Teleport>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MToast —— 轻提示（替代 k-toast，iOS）
 * 底部安全区上浮胶囊（黑字/白字由主题变量决定）；position 控制水平对齐
 * （left/center/right，与 PlayerPage 现状一致）。
 * 关闭时机由调用方 opened 控制（现页面自管 timeout，不内建）。
 *
 * Teleport to body：页面在 TabsPage 推屏轨道（transform 层叠上下文 z≈0）内，
 * 不脱离的话 toast 会落入该上下文、被 MiniPlayer（z-1000）盖住（08-15-toast-not-showing）。
 * 注意：teleport 后脱离 .m-app，--m-safe-area-* 桥接变量失效，
 * bottom/padding 必须两级兜底到 --safe-area-inset-*（Capacitor 注入 html）与 env()。
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
  bottom: calc(
    16px + var(
        --m-safe-area-bottom,
        var(--safe-area-inset-bottom, env(safe-area-inset-bottom, 0px))
      )
  );
  display: flex;
  width: auto;
  z-index: 1300;
  padding: 0
    calc(
      16px +
        var(
          --m-safe-area-left,
          var(--safe-area-inset-left, env(safe-area-inset-left, 0px))
        )
    )
    0
    calc(
      16px +
        var(
          --m-safe-area-right,
          var(--safe-area-inset-right, env(safe-area-inset-right, 0px))
        )
    );
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
    border-radius: var(--m-radius-md);
    padding: var(--m-spacing-sub) var(--m-spacing);
    font-size: 14px;
    line-height: 1.4;
    color: var(--m-text);
    background-color: var(--m-surface-1);
    border: 1px solid var(--m-hairline);
  }
}

:global(.dark) .m-toast__inner {
  color: var(--m-text);
  background-color: var(--m-surface-1);
}
</style>