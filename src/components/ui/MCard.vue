<template>
  <div class="m-card">
    <header v-if="$slots.header" class="m-card__header">
      <slot name="header" />
    </header>
    <div class="m-card__content">
      <slot />
    </div>
    <footer v-if="$slots.footer" class="m-card__footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MCard —— 内容卡片（替代 k-card）
 * iOS：大圆角 24px + surface-1 底。外边距由调用方 class 控制（Konsta 默认
 * my-4 mx-safe-4 与页面覆盖 class 冲突，零回归起见自研默认无外边距）。
 */
defineProps<{
  raised?: boolean
}>()
</script>

<style scoped lang="scss">
.m-card {
  box-sizing: border-box;
  border-radius: var(--m-radius-card);
  background-color: var(--m-surface-1);
  color: var(--m-text);
  overflow: hidden;

  &--raised {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }

  &__header,
  &__footer {
    padding: 16px;
    font-size: 14px;
  }

  &__content {
    padding: 16px;
    font-size: 14px;
    line-height: 1.45;
  }
}
</style>