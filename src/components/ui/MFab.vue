<template>
  <button
    type="button"
    class="m-fab"
    :class="{ 'm-fab--with-text': $slots.text || text }"
    :disabled="disabled"
    role="button"
    tabindex="0"
  >
    <span v-if="$slots.icon" class="m-fab__icon">
      <slot name="icon" />
    </span>
    <span v-if="(text || $slots.text) && textPosition === 'before'" class="m-fab__text">
      {{ text }}<slot name="text" />
    </span>
    <span v-else-if="$slots.icon === undefined && !textPosition" class="m-fab__icon">
      <slot />
    </span>
    <span v-if="(text || $slots.text) && textPosition === 'after'" class="m-fab__text">
      {{ text }}<slot name="text" />
    </span>
  </button>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MFab —— 浮动按钮（替代 k-fab，iOS）
 * 44×44 全圆、主色底（暗色主色 shade/50）、玻璃 fab 阴影、白字。
 * 契约：默认 slot = 图标；#icon slot 同义（Konsta 写法兼容）；
 * #text / text prop 带文字形态（加宽）。定位由调用方 class/style 控制。
 */
withDefaults(
  defineProps<{
    text?: string
    textPosition?: 'before' | 'after'
    disabled?: boolean
  }>(),
  {
    text: undefined,
    textPosition: 'after',
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
  color: var(--m-on-primary);
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

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    flex: 0 0 24px;
  }

  &__text {
    font-size: 14px;
    font-weight: 600;
    text-transform: uppercase;
  }
}

:global(.dark) .m-fab {
  background-color: rgba(var(--m-primary-rgb), 0.5);
  box-shadow: var(--m-shadow-ios-dark-glass-fab);
}
</style>