<template>
  <section class="m-empty-state">
    <h2>{{ title }}</h2>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.default" class="m-empty-state__actions">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.m-empty-state {
  box-sizing: border-box;
  min-height: var(--muses-empty-state-min-height);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--muses-space-xl);
  color: var(--muses-color-ink-muted);
  text-align: center;
}

.m-empty-state h2 {
  margin: 0 0 var(--muses-space-sm);
  color: var(--muses-color-ink);
  font-size: var(--muses-font-title);
  line-height: var(--muses-line-height-title);
}

.m-empty-state p {
  max-width: var(--muses-empty-state-max-width);
  margin: 0;
  font-size: var(--muses-font-body-sm);
  text-wrap: pretty;
}

.m-empty-state__actions {
  margin-top: var(--muses-space-lg);
}
</style>
