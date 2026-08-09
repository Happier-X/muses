<template>
  <span
    class="w-[var(--m-cover-size)] h-[var(--m-cover-size)] flex-[0_0_var(--m-cover-size)] inline-grid place-items-center overflow-hidden bg-[rgba(146,148,156,0.16)] text-black/55 dark:text-white/55 text-[calc(var(--m-cover-size)/2)] [&>img]:w-full [&>img]:h-full [&>img]:object-cover"
    :class="radius === 'sm' ? 'rounded-[8px]' : 'rounded-[10px]'"
    :style="coverStyle"
    :aria-hidden="alt ? undefined : 'true'"
  >
    <img v-if="src" :src="src" :alt="alt" />
    <slot v-else name="placeholder">
      <component :is="musicalNotesOutline" aria-hidden="true" class="size-[40%]" />
    </slot>
  </span>
</template>

<script setup lang="ts">
/** APP-ONLY：音乐封面及稳定占位。 */
import { computed, type CSSProperties } from 'vue'
import { musicalNotesOutline } from '@/icons'

const props = withDefaults(defineProps<{
  src?: string | null
  size?: 'sm' | 'md' | number
  radius?: 'sm' | 'md'
  alt?: string
}>(), {
  src: null,
  size: 'md',
  radius: 'md',
  alt: '',
})

const coverStyle = computed<CSSProperties>(() => {
  const size = typeof props.size === 'number'
    ? `${props.size}px`
    : props.size === 'sm' ? '48px' : '52px'
  return { '--m-cover-size': size }
})
</script>
