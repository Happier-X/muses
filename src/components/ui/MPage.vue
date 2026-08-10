<template>
  <div class="flex flex-col h-full overflow-hidden m-page">
    <k-navbar>
      <template v-if="$slots.start" #left>
        <slot name="start" />
      </template>
      <template #title>
        <slot name="title" />
      </template>
      <template v-if="$slots.end" #right>
        <slot name="end" />
      </template>
      <template v-if="$slots.subnavbar" #subnavbar>
        <slot name="subnavbar" />
      </template>
    </k-navbar>
    <m-content :fullscreen="fullscreen">
      <slot />
    </m-content>
  </div>
</template>

<script setup lang="ts">
import { kNavbar } from 'konsta/vue'
import MContent from './MContent.vue'

withDefaults(defineProps<{
  fullscreen?: boolean
}>(), {
  fullscreen: false,
})
</script>
