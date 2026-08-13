<template>
  <div class="m-actions-label">
    <slot />
  </div>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MActionsLabel —— 操作表标题行（替代 k-actions-label，iOS）
 * 56px 行、13px 次级色文字居中。
 */
</script>

<style scoped lang="scss">
.m-actions-label {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: var(--m-list-row-h);
  padding: 0 var(--m-spacing);
  box-sizing: border-box;
  overflow: hidden;
  position: relative;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.3;
  color: var(--m-text-2);
  text-align: center;
}
</style>