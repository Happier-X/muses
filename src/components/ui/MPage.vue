<template>
  <div class="m-page">
    <h-nav-bar :fixed="false">
      <template v-if="$slots.start" #left>
        <slot name="start" />
      </template>
      <template #title>
        <slot name="title" />
      </template>
      <template v-if="$slots.end" #right>
        <slot name="end" />
      </template>
    </h-nav-bar>
    <template v-if="$slots.subnavbar">
      <slot name="subnavbar" />
    </template>
    <m-content :fullscreen="fullscreen">
      <slot />
    </m-content>
  </div>
</template>

<script setup lang="ts">
import { HNavBar } from 'happier-ui'
import MContent from './MContent.vue'

withDefaults(defineProps<{
  fullscreen?: boolean
}>(), {
  fullscreen: false,
})
</script>

<style>
.m-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  /* ⚠️ 故意不加 contain，避免重建 fixed 包含块导致浮层偏移 */
}
</style>
