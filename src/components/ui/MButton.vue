<template>
  <component
    :is="component"
    :type="component === 'button' ? type : undefined"
    :disabled="component === 'button' && disabled ? disabled : undefined"
    :href="component === 'a' ? href : undefined"
    class="m-button"
    :class="[
      `m-button--${variant}`,
      `m-button--${size}`,
      {
        'm-button--rounded': isRounded,
        'm-button--danger': danger && variant === 'fill',
        'm-button--disabled': disabled,
        'm-button--inline': inline,
      },
    ]"
  >
    <slot />
  </component>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MButton —— 对齐 Konsta k-button 使用面（iOS 视觉）
 * variant: fill（主色实底白字）/ clear（透明主色字）/ outline（主色描边）
 * rounded / rounded-full 均为全圆（历史用法兼容，Vue 属性名自动 camelize）
 */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    component?: 'button' | 'a'
    type?: 'button' | 'submit'
    variant?: 'fill' | 'clear' | 'outline' | 'tonal'
    size?: 'small' | 'md' | 'large'
    /** 全圆（iOS rounded-full） */
    rounded?: boolean
    /** 兼容历史写法 rounded-full */
    roundedFull?: boolean
    /** fill 变体红色底（Konsta danger 语义扩展） */
    danger?: boolean
    disabled?: boolean
    /** 不占满容器宽度 */
    inline?: boolean
    href?: string
  }>(),
  {
    component: 'button',
    type: 'button',
    variant: 'fill',
    size: 'md',
    rounded: false,
    roundedFull: false,
    danger: false,
    disabled: false,
    inline: false,
    href: undefined,
  },
)

const isRounded = computed(() => props.rounded || props.roundedFull)
</script>

<style scoped lang="scss">
.m-button {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  appearance: none;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  user-select: none;
  outline: none;
  -webkit-tap-highlight-color: rgba(0, 0, 0, 0);
  font-family: inherit;
  border: none;
  display: flex;
  width: 100%;
  padding: 0 8px;
  border-radius: var(--m-radius-md);
  /* 纯颜色过渡（Project Rule：动画唯一例外保留 CSS transition） */
  transition:
    color 0.1s ease,
    background-color 0.1s ease,
    border-color 0.1s ease;

  &--inline {
    width: auto;
  }

  &--rounded {
    border-radius: 9999px;
  }

  &--small {
    height: 28px;
    font-size: 14px;
    font-weight: 500;
  }

  &--md {
    height: 34px;
    font-size: 15px;
    font-weight: 500;
  }

  &--large {
    height: 48px;
    font-size: 17px;
    font-weight: 600;
  }

  &--fill {
    color: var(--m-on-primary);
    background-color: var(--m-primary);

    &:active {
      background-color: var(--m-primary-shade);
    }

    &.m-button--danger {
      background-color: var(--m-danger);

      &:active {
        background-color: var(--m-danger-shade);
      }
    }
  }

  &--clear {
    color: var(--m-primary);
    background-color: transparent;

    &:active {
      background-color: rgba(var(--m-primary-rgb), 0.15);
    }
  }

  &--outline {
    color: var(--m-primary);
    background-color: transparent;
    border: 2px solid var(--m-primary);
    padding: 0 6px; /* 2px 边框抵消，保持内容区 8px */

    &:active {
      background-color: rgba(var(--m-primary-rgb), 0.15);
    }
  }

  /* Dialog 按钮用（Konsta tonal）：主色 15% 底 + 主色字 */
  &--tonal {
    color: var(--m-primary);
    background-color: rgba(var(--m-primary-rgb), 0.15);

    &:active {
      background-color: rgba(var(--m-primary-rgb), 0.25);
    }
  }

  &--disabled {
    pointer-events: none;
    color: var(--m-disabled-text);
    background-color: var(--m-disabled-bg);

    &.m-button--clear {
      background-color: transparent;
    }

    &.m-button--outline {
      background-color: transparent;
      border-color: var(--m-disabled-border);
    }
  }
}
</style>