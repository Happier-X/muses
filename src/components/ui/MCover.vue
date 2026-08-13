<template>
  <span
    class="m-cover"
    :class="radius === 'sm' ? 'm-cover--sm-radius' : 'm-cover--md-radius'"
    :style="coverStyle"
    :aria-hidden="alt ? undefined : 'true'"
  >
    <img v-if="src" :src="src" :alt="alt" />
    <slot v-else name="placeholder">
      <component :is="musicalNotesOutline" aria-hidden="true" class="m-cover__placeholder-icon" />
    </slot>
  </span>
</template>

<script setup lang="ts">
/** APP-ONLY：音乐封面及稳定占位（自研 scss 版）。 */
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

<style scoped lang="scss">
.m-cover {
  display: inline-grid;
  place-items: center;
  width: var(--m-cover-size);
  height: var(--m-cover-size);
  flex: 0 0 var(--m-cover-size);
  box-sizing: border-box;
  overflow: hidden;
  background-color: var(--m-surface-2);
  color: var(--m-text-2);
  font-size: calc(var(--m-cover-size) / 2);

  &--sm-radius {
    border-radius: var(--m-radius-sm);
  }

  &--md-radius {
    border-radius: var(--m-radius-card);
  }

  :deep(img) {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  &__placeholder-icon {
    width: var(--m-list-icon);
    height: var(--m-list-icon);
  }
}
</style>