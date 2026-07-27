<template>
  <span
    class="w-[var(--m-cover-size)] h-[var(--m-cover-size)] flex-[0_0_var(--m-cover-size)] inline-grid place-items-center overflow-hidden bg-[var(--muses-color-cover-placeholder)] text-[var(--muses-color-ink-muted)] text-[calc(var(--m-cover-size)/2)] m-cover [&>img]:w-full [&>img]:h-full [&>img]:object-cover"
    :class="radius === 'sm' ? 'rounded-[var(--muses-radius-cover-sm)]' : 'rounded-[var(--muses-radius-cover)]'"
    :style="coverStyle"
    :aria-hidden="alt ? undefined : 'true'"
  >
    <img v-if="src" :src="src" :alt="alt" />
    <slot v-else name="placeholder">
      <h-icon :icon="musicalNotesOutline" />
    </slot>
  </span>
</template>

<script setup lang="ts">
/** APP-ONLY：音乐封面及稳定占位。 */
import { computed, type CSSProperties } from 'vue'
import { HIcon } from 'happier-ui'
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
    : props.size === 'sm' ? 'var(--muses-cover-size-sm)' : 'var(--muses-cover-size-md)'
  return { '--m-cover-size': size }
})
</script>
