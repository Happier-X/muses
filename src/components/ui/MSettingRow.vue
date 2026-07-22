<template>
  <ion-item class="m-setting-row" :lines="lines">
    <ion-label>
      <h2 class="m-setting-row__label">{{ label }}</h2>
      <p v-if="description" class="m-setting-row__description">{{ description }}</p>
    </ion-label>
    <div v-if="$slots.end" slot="end" class="m-setting-row__end">
      <slot name="end" />
    </div>
  </ion-item>
</template>

<script setup lang="ts">
import { IonItem, IonLabel } from '@ionic/vue'

withDefaults(defineProps<{
  label: string
  description?: string
  lines?: 'none' | 'inset' | 'full'
}>(), {
  description: undefined,
  lines: 'full',
})
</script>

<style scoped>
.m-setting-row {
  --min-height: var(--muses-touch-target);
  --padding-start: var(--muses-space-lg);
  --inner-padding-end: var(--muses-space-md);
  --background: transparent;
}

.m-setting-row__label {
  margin: 0;
  font-size: var(--muses-font-title);
  font-weight: 600;
  line-height: var(--muses-line-height-title);
  color: var(--muses-color-ink);
}

.m-setting-row__description {
  margin: var(--muses-space-xs) 0 0;
  font-size: var(--muses-font-body-sm);
  color: var(--muses-color-ink-muted);
  text-wrap: pretty;
}

.m-setting-row__end {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  margin-inline-start: var(--muses-space-md);
}
</style>
