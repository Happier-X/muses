<template>
  <div
    class="flex-1 overflow-y-auto [-webkit-overflow-scrolling:touch] relative overscroll-contain m-content"
    :class="{ '[--offset-top:0px] [--offset-bottom:0px] m-content--fullscreen': fullscreen }"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  fullscreen?: boolean
}>()
</script>
