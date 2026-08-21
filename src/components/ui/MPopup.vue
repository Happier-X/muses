<template>
  <!-- Teleport to body：TabsPage 推屏轨道带 transform，会成为 fixed 后代的包含块，
       不脱离的话全屏弹层相对轨道而非视口定位（08-21-fix-overlay-fixed-containing-block）。 -->
  <Teleport to="body">
    <AnimatePresence>
      <template v-if="opened">
        <motion.div
          key="backdrop"
          class="m-overlay-backdrop m-popup-backdrop"
          :initial="{ opacity: 0 }"
          :animate="{ opacity: transparent ? 0 : 1 }"
          :exit="{ opacity: 0 }"
          :transition="{ duration: 0.3 }"
          @click="onBackdropClick"
        />
        <motion.div
          key="panel"
          class="m-popup"
          :class="{ 'm-popup--fullscreen': fullscreen, 'm-popup--transparent': transparent }"
          :initial="{ x: '-50%', y: '100vh' }"
          :animate="{ x: '-50%', y: '-50%' }"
          :exit="{ x: '-50%', y: '100vh' }"
          :transition="{ duration: 0.4, ease: [0.32, 0.72, 0, 1] }"
        >
          <slot />
        </motion.div>
      </template>
    </AnimatePresence>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MPopup —— 全屏弹层（替代 k-popup，iOS）
 * 全屏（w-screen h-screen）surface 底；平板 md+ 为 640×640 居中方块 + 32px 圆角
 * （Konsta 默认行为）；motion-v 从屏底滑入/滑出。z 阶梯 1100。
 */
import { AnimatePresence, motion } from 'motion-v'

withDefaults(
  defineProps<{
    opened?: boolean
    fullscreen?: boolean
    /** 面板背景透明（PlayerPage 沉浸式用 AMLL 背景，下滑关闭需露出底下内容） */
    transparent?: boolean
  }>(),
  {
    opened: false,
    fullscreen: true,
    transparent: false,
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
.m-popup {
  position: fixed;
  left: 50%;
  top: 50%;
  width: 100vw;
  height: 100vh;
  max-width: 100%;
  max-height: 100%;
  box-sizing: border-box;
  overflow: hidden;
  background-color: var(--m-surface);
  color: var(--m-text);
  z-index: 1100;

  @media (min-width: 768px) {
    width: 640px;
    height: 640px;
    border-radius: var(--m-radius-lg);
  }

  /* fullscreen：md+ 仍全屏无圆角（PlayerPage 用法） */
  &--fullscreen {
    @media (min-width: 768px) {
      width: 100vw;
      height: 100vh;
      border-radius: 0;
    }
  }

  /* transparent：面板背景透明（PlayerPage 沉浸式，下滑关闭露出底下） */
  &--transparent {
    background-color: transparent;
  }
}
</style>