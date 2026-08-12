<template>
  <button
    type="button"
    class="m-fab"
    :class="{ 'm-fab--with-text': $slots.text }"
    :disabled="disabled"
  >
    <span class="m-fab__icon">
      <slot />
    </span>
    <slot name="text" />
  </button>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MFab —— 浮动按钮（替代 k-fab）
 * iOS：44×44 全圆、主色底（暗色主色 shade/50）、玻璃 fab 阴影、白字。
 * 定位（fixed/absolute + 安全区）由调用方 class 控制。
 */
withDefaults(
  defineProps<{
    disabled?: boolean
  }>(),
  {
    disabled: false,
  },
)
</script>

<style scoped lang="scss">
.m-fab {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  overflow: hidden;
  user-select: none;
  padding: 0;
  border: none;
  outline: none;
  font-family: inherit;
  width: 44px;
  height: 44px;
  border-radius: 9999px;
  color: #fff;
  background-color: var(--m-primary);
  box-shadow: var(--m-shadow-ios-light-glass-fab);
  -webkit-tap-highlight-color: rgba(0, 0, 0, 0);

  &--with-text {
    padding: 0 16px;
    width: auto;
  }

  &:disabled {
    opacity: 0.6;
    pointer-events: none;
  }

  &::selection {
    background: transparent;
  }
}

:global(.dark) .m-fab {
  background-color: rgba(0, 103, 214, 0.5);
  box-shadow: var(--m-shadow-ios-dark-glass-fab);
}

.m-fab__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  flex: 0 0 24px;
}
</style>