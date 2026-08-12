<template>
  <h2 class="m-block-title" :class="`m-block-title--${size}`">
    <slot />
  </h2>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MBlockTitle —— 区块标题（替代 k-block-title）
 * iOS：17px 半粗黑/60；medium 20px 全黑；large 22px 全黑。
 * 外边距由调用方 class 控制（px-safe 语义 → 页面 scoped 处理）。
 */
withDefaults(
  defineProps<{
    size?: 'default' | 'medium' | 'large'
  }>(),
  {
    size: 'default',
  },
)
</script>

<style scoped lang="scss">
.m-block-title {
  box-sizing: border-box;
  margin: 0;
  padding: 8px 16px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-family: inherit;

  &--default {
    font-size: 17px;
    font-weight: 600;
    line-height: 1.4;
    color: var(--m-text-2);
  }

  &--medium {
    font-size: 20px;
    font-weight: 600;
    line-height: 1.3;
    color: var(--m-text);
  }

  &--large {
    font-size: 22px;
    font-weight: 600;
    line-height: 1.3;
    color: var(--m-text);
  }
}
</style>