<template>
  <motion.button
    type="button"
    class="m-icon-button"
    :class="{
      'm-icon-button--disabled': disabled,
      'm-icon-button--size-sm': size === 'sm',
    }"
    :disabled="disabled"
    :aria-label="ariaLabel"
    :while-tap="{ scale: 0.88 }"
    :transition="{ type: 'spring', stiffness: 600, damping: 25 }"
    @click="$emit('click', $event)"
  >
    <!-- 半透明圆形涟漪背景（点击时淡入扩散，优雅反馈） -->
    <span class="m-icon-button__ripple" aria-hidden="true" />

    <span class="m-icon-button__icon">
      <slot />
    </span>
  </motion.button>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MIconButton —— 统一图标按钮（替代各处手写图标 button）
 *
 * 视觉契约：
 * - 透明底，图标使用继承文字色（由调用方通过 color 控制）
 * - 点击反馈 = 半透明圆形背景淡入 + 轻微缩放（motion 弹簧动画），
 *   替代 MButton clear 的浅蓝底 / 各页面手写 active 背景色，全局统一
 * - 常用尺寸：默认 40px 触控区（图标 20px）；sm 36px 触控区（图标 18px）
 *
 * 用法：
 * ```vue
 * <m-icon-button aria-label="搜索" @click="onSearch">
 *   <search-outline class="mi-icon" />
 * </m-icon-button>
 * ```
 */
import { motion } from 'motion-v'

withDefaults(
  defineProps<{
    /** 无障碍标签（必传：纯图标按钮） */
    ariaLabel?: string
    disabled?: boolean
    size?: 'md' | 'sm'
  }>(),
  {
    ariaLabel: undefined,
    disabled: false,
    size: 'md',
  },
)

defineEmits<{
  (e: 'click', event: MouseEvent): void
}>()
</script>

<style scoped lang="scss">
.m-icon-button {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  flex: 0 0 40px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: inherit;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  user-select: none;
  overflow: visible;

  &:disabled {
    opacity: 0.4;
    cursor: default;
  }

  /* 半透明圆形背景：常态透明，active 淡入 10% 深色 */
  &__ripple {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: currentColor;
    opacity: 0;
    transform: scale(0.5);
    transition:
      opacity 0.18s ease,
      transform 0.24s cubic-bezier(0.2, 0.8, 0.4, 1);
    pointer-events: none;
  }

  &:active &__ripple {
    opacity: 0.1;
    transform: scale(1);
  }

  &__icon {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1;
  }

  &--disabled {
    pointer-events: none;
  }
}

/* 深色主题：涟漪用白 10% */
:global(.dark) .m-icon-button:active .m-icon-button__ripple {
  opacity: 0.12;
}
</style>
