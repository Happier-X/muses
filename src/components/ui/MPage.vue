<template>
  <div class="flex flex-col h-full overflow-hidden m-page">
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
