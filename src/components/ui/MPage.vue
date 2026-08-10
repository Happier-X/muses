<template>
  <k-page class="k-page m-page flex flex-col overflow-hidden !h-auto !bottom-safe-24 md:!bottom-0">
    <k-navbar rightClass="!h-8">
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
  </k-page>
</template>

<script setup lang="ts">
import { kNavbar, kPage } from 'konsta/vue'
import MContent from './MContent.vue'

withDefaults(defineProps<{
  fullscreen?: boolean
}>(), {
  fullscreen: false,
})
</script>
