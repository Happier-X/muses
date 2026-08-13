<template>
  <div class="m-tabbar">
    <div class="m-tabbar__bg" aria-hidden="true" />
    <div class="m-tabbar__inner">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MTabbar —— 底部标签栏（替代 k-tabbar + k-toolbar-pane，iOS）
 * 64px 内容 + 底部安全区；玻璃底 = surface→透明渐变（to top）+ blur 2px + mask 渐显
 * （TabsPage 现用法等价：from-white/70 白玻璃、dark from-black/60）。
 * 定位（fixed / z-index）由页面 class 控制，与现状一致。
 * 图标+文字固定组合形态（labels/icons 为 API 兼容占位）。
 */
defineProps<{
  labels?: boolean
  icons?: boolean
}>()
</script>

<style scoped lang="scss">
.m-tabbar {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  padding-bottom: var(--m-safe-area-bottom, 0px);
  background-color: var(--m-surface-1);
  border-top: 1px solid var(--m-hairline);

  &__bg {
    position: absolute;
    inset: 0;
    pointer-events: none;
    background-color: var(--m-surface-1);
  }

  &__inner {
    position: relative;
    display: flex;
    justify-content: space-between;
    align-items: stretch;
    gap: var(--m-spacing);
    height: 64px;
    padding-left: calc(var(--m-spacing) + var(--m-safe-area-left, 0px));
    padding-right: calc(var(--m-spacing) + var(--m-safe-area-right, 0px));
    box-sizing: border-box;
  }
}
</style>