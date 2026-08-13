<template>
  <div class="m-empty">
    <div v-if="icon || $slots.icon" class="m-empty__icon">
      <component
        :is="icon"
        v-if="icon"
        aria-hidden="true"
        class="m-empty__icon-svg"
      />
      <slot v-else name="icon" />
    </div>
    <h3 class="m-empty__title">{{ title }}</h3>
    <p v-if="description" class="m-empty__description">{{ description }}</p>
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

<style scoped lang="scss">
.m-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--m-spacing-sub);
  padding: 56px var(--m-spacing);
  text-align: center;
  box-sizing: border-box;

  &__icon {
    display: grid;
    place-items: center;
    width: 72px;
    height: 72px;
    border-radius: 50%;
    background-color: var(--m-surface-2);
    color: var(--m-text-3);
  }

  &__icon-svg {
    width: var(--m-list-icon);
    height: var(--m-list-icon);
  }

  &__title {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
    line-height: 1.35;
    color: var(--m-text);
  }

  &__description {
    margin: 0;
    font-size: 15px;
    line-height: 1.4;
    color: var(--m-text-2);
  }
}

</style>