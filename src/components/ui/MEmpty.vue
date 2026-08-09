<template>
  <div class="flex flex-col items-center justify-center gap-[12px] px-[24px] py-[56px] text-center">
    <div
      v-if="icon || $slots.icon"
      class="grid place-items-center w-[72px] h-[72px] rounded-full bg-black/5 dark:bg-white/15 text-black/40 dark:text-white/40"
    >
      <component
        :is="icon"
        v-if="icon"
        aria-hidden="true"
        class="w-[32px] h-[32px]"
      />
      <slot v-else name="icon" />
    </div>
    <h3 class="m-0 text-[17px] font-semibold leading-[1.35] text-black dark:text-white">
      {{ title }}
    </h3>
    <p
      v-if="description"
      class="m-0 text-[15px] leading-[1.4] text-black/55 dark:text-white/55"
    >
      {{ description }}
    </p>
    <slot />
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(defineProps<{
  title: string
  description?: string
  icon?: Component
}>(), {
  description: undefined,
  icon: undefined,
})
</script>
