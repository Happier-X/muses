<template>
  <div class="m-actions-group">
    <slot />
  </div>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MActionsGroup —— 操作表分组（替代 k-actions-group，iOS）
 * 32px 大圆角圆角容器；底部间距 8px（首组无上距）。
 * 底色透明（继承表面色，与 Konsta iOS 现状一致）。
 */
withDefaults(
  defineProps<{
    dividers?: boolean
  }>(),
  {
    dividers: false,
  },
)
</script>

<style scoped lang="scss">
.m-actions-group {
  position: relative;
  border-radius: var(--m-radius-dialog);
  overflow: hidden;
  background-color: var(--m-surface-1);
  border: 1px solid var(--m-hairline);

  & + & {
    margin-top: 8px;
  }
}
</style>