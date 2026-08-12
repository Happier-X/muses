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
  padding-bottom: calc(16px + var(--m-safe-area-bottom, 0px));

  &__bg {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: calc(var(--m-safe-area-bottom, 0px) + 16px + 64px + 16px);
    pointer-events: none;
    background: linear-gradient(to top, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0) 100%);
    -webkit-backdrop-filter: blur(2px);
    backdrop-filter: blur(2px);
    -webkit-mask-image: linear-gradient(to top, #000 50%, transparent 100%);
    mask-image: linear-gradient(to top, #000 50%, transparent 100%);
  }

  &__inner {
    position: relative;
    display: flex;
    justify-content: space-between;
    align-items: stretch;
    gap: 16px;
    height: 64px;
    padding-left: calc(16px + var(--m-safe-area-left, 0px));
    padding-right: calc(16px + var(--m-safe-area-right, 0px));
    box-sizing: border-box;
  }
}

:global(.dark) .m-tabbar__bg {
  background: linear-gradient(
    to top,
    rgba(0, 0, 0, 0.6) 0%,
    rgba(0, 0, 0, 0) 100%
  );
}
</style>