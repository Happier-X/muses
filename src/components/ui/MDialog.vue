<template>
  <!-- Teleport to body：TabsPage 推屏轨道带 transform，会成为 fixed 后代的包含块，
       不脱离的话对话框相对轨道而非视口定位（08-21-fix-overlay-fixed-containing-block）。 -->
  <Teleport to="body">
    <AnimatePresence>
      <template v-if="opened">
        <motion.div
          key="backdrop"
          class="m-overlay-backdrop m-dialog-backdrop"
          :initial="{ opacity: 0 }"
          :animate="{ opacity: 1 }"
          :exit="{ opacity: 0 }"
          :transition="{ duration: 0.3 }"
          @click="onBackdropClick"
        />
        <motion.div
          key="panel"
          class="m-dialog"
          :initial="{ x: '-50%', y: '-50%', scale: 0.85, opacity: 0 }"
          :animate="{ x: '-50%', y: '-50%', scale: 1, opacity: 1 }"
          :exit="{ x: '-50%', y: '-50%', scale: 0.85, opacity: 0 }"
          :transition="{ duration: 0.3, ease: [0.32, 0.72, 0, 1] }"
        >
          <div class="m-dialog__content-wrap">
            <div v-if="title || $slots.title" class="m-dialog__title">
              <slot name="title">{{ title }}</slot>
            </div>
            <div class="m-dialog__content">
              <slot />
            </div>
            <div v-if="$slots.buttons" class="m-dialog__buttons">
              <slot name="buttons" />
            </div>
          </div>
        </motion.div>
      </template>
    </AnimatePresence>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MDialog —— 居中对话框（替代 k-dialog，iOS）
 * 300px 宽、32px 圆角、surface-1 底；进入/离开 = 向心缩放 + 淡入（motion-v）。
 * title prop + 默认 slot 内容 + #buttons 按钮区。z 阶梯 1200。
 */
import { AnimatePresence, motion } from 'motion-v'

withDefaults(
  defineProps<{
    opened?: boolean
    title?: string
  }>(),
  {
    opened: false,
    title: undefined,
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
.m-dialog {
  position: fixed;
  left: 50%;
  top: 50%;
  max-width: 100%;
  max-height: 100%;
  width: 300px;
  box-sizing: border-box;
  border-radius: var(--m-radius-dialog);
  overflow: hidden;
  background-color: var(--m-surface-1);
  color: var(--m-text);
  z-index: 1200;

  &__content-wrap {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: var(--m-spacing);
    gap: var(--m-spacing-sub);
    position: relative;
    box-sizing: border-box;
  }

  &__title {
    width: 100%;
    font-size: 16px;
    font-weight: 600;
    line-height: 1.3;
    text-align: center;
    color: var(--m-text);
  }

  &__content {
    width: 100%;
    font-size: 16px;
    line-height: 1.45;
  }

  &__buttons {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    padding: var(--m-spacing-sub) 0 0;
    gap: 8px;
    box-sizing: border-box;
  }
}
</style>