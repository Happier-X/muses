<template>
  <div
    class="m-setting-row"
    :class="`m-setting-row--lines-${lines}`"
  >
    <div class="m-setting-row__text">
      <h2 class="m-setting-row__label">{{ label }}</h2>
      <p v-if="description" class="m-setting-row__description">{{ description }}</p>
    </div>
    <div v-if="$slots.end" class="m-setting-row__end">
      <slot name="end" />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * happier-ui 候选：纯 Vue 设置行壳。
 * toggle/input 由宿主放入 end 槽，不封装表单引擎。
 */
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
  box-sizing: border-box;
  display: flex;
  align-items: center;
  min-height: var(--h-touch-target, var(--muses-touch-target));
  padding: var(--h-space-md, var(--muses-space-md)) var(--h-space-md, var(--muses-space-md))
    var(--h-space-md, var(--muses-space-md)) var(--h-space-lg, var(--muses-space-lg));
  background: transparent;
}

.m-setting-row--lines-full {
  border-bottom: 1px solid var(--h-color-separator, var(--muses-color-border-subtle));
}

.m-setting-row--lines-inset {
  border-bottom: 1px solid var(--h-color-separator, var(--muses-color-border-subtle));
  margin-inline-start: var(--h-space-lg, var(--muses-space-lg));
}

.m-setting-row--lines-none {
  border-bottom: none;
}

.m-setting-row__text {
  flex: 1;
  min-width: 0;
}

.m-setting-row__label {
  margin: 0;
  font-size: var(--h-font-title, var(--muses-font-title));
  font-weight: 600;
  line-height: var(--h-line-height-title, var(--muses-line-height-title));
  color: var(--h-color-ink, var(--muses-color-ink));
}

.m-setting-row__description {
  margin: var(--h-space-xs, var(--muses-space-xs)) 0 0;
  font-size: var(--h-font-body-sm, var(--muses-font-body-sm));
  color: var(--h-color-ink-muted, var(--muses-color-ink-muted));
  text-wrap: pretty;
}

.m-setting-row__end {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  margin-inline-start: var(--h-space-md, var(--muses-space-md));
}
</style>
