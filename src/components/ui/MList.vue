<template>
  <div
    class="m-list"
    :class="{
      'm-list--strong': strong,
      'm-list--inset': inset,
      'm-list--outline': outline,
      'm-list--outline-inset': inset && outline,
    }"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MList —— 列表容器（替代 k-list，iOS）
 * strong：surface-1 卡片底（行间带分隔线）
 * inset：左右安全区缩进 + 大圆角 32px（配合 strong）
 * outline：上下 hairline（outline-ios 兼容名强/outline 布尔）
 * dividers 由 MList 统一 provide（Konsta iOS 默认开）
 */
import { provide, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    inset?: boolean
    strong?: boolean
    outline?: boolean
    /** 兼容历史 strong-ios / outline-ios（Vue 属性名 camelize） */
    strongIos?: boolean
    outlineIos?: boolean
    dividers?: boolean
  }>(),
  {
    inset: false,
    strong: false,
    outline: false,
    strongIos: false,
    outlineIos: false,
    dividers: true,
  },
)

const state = ref({
  strong: props.strong || props.strongIos,
  outline: props.outline || props.outlineIos,
  dividers: props.dividers,
})

provide('m-list-state', state)
</script>

<style scoped lang="scss">
.m-list {
  position: relative;
  z-index: 10;

  &--strong {
    background-color: var(--m-surface-1);
  }

  &--inset {
    margin-left: calc(16px + var(--m-safe-area-left, 0px));
    margin-right: calc(16px + var(--m-safe-area-right, 0px));
    border-radius: var(--m-radius-card);
    overflow: hidden;
  }

  &--outline {
    border-top: 1px solid var(--m-hairline);
    border-bottom: 1px solid var(--m-hairline);
  }

  &--outline-inset {
    border-top: none;
    border-bottom: none;
    border: 1px solid var(--m-hairline);
    margin-top: 16px;
    margin-bottom: 16px;
  }
}
</style>