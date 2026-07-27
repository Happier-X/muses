<template>
  <div
    class="m-content"
    :class="{ 'm-content--fullscreen': fullscreen }"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  fullscreen?: boolean
}>()
</script>

<style>
.m-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  position: relative;
  overscroll-behavior: contain;
}

.m-content--fullscreen {
  --offset-top: 0px;
  --offset-bottom: 0px;
}
</style>
