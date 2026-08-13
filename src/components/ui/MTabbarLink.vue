<template>
  <button
    type="button"
    class="m-tabbar-link"
    :class="{ 'm-tabbar-link--active': active }"
    :aria-current="active ? 'page' : undefined"
    @click="onClick"
  >
    <span class="m-tabbar-link__icon">
      <slot name="icon" />
    </span>
    <span v-if="label" class="m-tabbar-link__label">{{ label }}</span>
  </button>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MTabbarLink —— 标签项（替代 k-tabbar-link，iOS）
 * 图标 28×28 + 12px 标签，垂直居中；active 主色（其余继承文字色）。
 */
withDefaults(
  defineProps<{
    label?: string
    active?: boolean
  }>(),
  {
    label: undefined,
    active: false,
  },
)

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

function onClick(event: MouseEvent) {
  emit('click', event)
}
</script>

<style scoped lang="scss">
.m-tabbar-link {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 4px 0;
  border: none;
  outline: none;
  appearance: none;
  background: transparent;
  cursor: pointer;
  user-select: none;
  font-family: inherit;
  gap: 2px;
  color: var(--m-text);
  -webkit-tap-highlight-color: transparent;

  &--active {
    color: var(--m-primary);
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: var(--m-list-icon);
    height: var(--m-list-icon);

    :deep(svg) {
      width: 100%;
      height: 100%;
    }
  }

  &__label {
    font-size: 12px;
    font-weight: 500;
    line-height: 1.2;
  }
}
</style>