<template>
  <span
    class="m-cover"
    :class="`m-cover--${radius}`"
    :style="coverStyle"
    :aria-hidden="alt ? undefined : 'true'"
  >
    <img v-if="src" :src="src" :alt="alt" />
    <slot v-else name="placeholder">
      <ion-icon :icon="musicalNotesOutline" aria-hidden="true" />
    </slot>
  </span>
</template>

<script setup lang="ts">
/**
 * APP-ONLY（不进 happier-ui v0.1）：音乐封面占位，依赖 ion-lucide。
 */
import { computed, type CSSProperties } from 'vue'
import { IonIcon } from '@ionic/vue'
import { musicalNotesOutline } from '@/icons/ion-lucide'

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

<style scoped>
.m-cover {
  width: var(--m-cover-size);
  height: var(--m-cover-size);
  flex: 0 0 var(--m-cover-size);
  display: inline-grid;
  place-items: center;
  overflow: hidden;
  background: var(--muses-color-cover-placeholder);
  color: var(--muses-color-ink-muted);
  font-size: calc(var(--m-cover-size) / 2);
}

.m-cover--sm {
  border-radius: var(--muses-radius-cover-sm);
}

.m-cover--md {
  border-radius: var(--muses-radius-cover);
}

.m-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
