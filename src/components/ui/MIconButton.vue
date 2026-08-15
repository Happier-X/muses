<template>
  <motion.button
    type="button"
    class="m-icon-button"
    :class="{
      'm-icon-button--disabled': disabled,
      'm-icon-button--size-sm': size === 'sm',
      'm-icon-button--size-lg': size === 'lg',
    }"
    :disabled="disabled"
    :aria-label="ariaLabel"
    @click="$emit('click', $event)"
  >
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
 * - 点击反馈 = **图标按下变暗约 15%**（椒盐实测：无背景圆底、无缩放，
 *   仅图标亮度 RGB 255→230 ≈ 0.9 alpha；08-15 椒盐长按录屏逐帧验证）
 * - 常用尺寸：默认 40px 触控区（图标 20px）；sm 36px 触控区（图标 18px）；
 *   lg 48px 触控区（图标 28px，播放页主控用）
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
    size?: 'md' | 'sm' | 'lg'
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

  &--size-sm {
    width: 36px;
    height: 36px;
    flex-basis: 36px;
  }

  /* lg：48px 触控区（播放页主控/浮动播放键） */
  &--size-lg {
    width: 48px;
    height: 48px;
    flex-basis: 48px;
  }

  &:disabled {
    opacity: 0.4;
    cursor: default;
  }

  &__icon {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1;
    pointer-events: none; /* 点击穿透到按钮 */
    transition: opacity 0.15s ease;
  }

  /* 按下反馈：图标变暗（椒盐实测 ~0.9 alpha；无背景圆底、无缩放） */
  &:active &__icon {
    opacity: 0.85;
  }

  &--disabled {
    pointer-events: none;
  }
}
</style>
