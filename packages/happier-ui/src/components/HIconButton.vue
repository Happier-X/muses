<template>
  <button
    type="button"
    class="h-icon-button"
    :class="[
      `h-icon-button--${size}`,
      `h-icon-button--${variant}`,
      color ? `h-icon-button--${color}` : null,
    ]"
    :disabled="disabled"
    :aria-label="ariaLabel"
    @click="onClick"
    @keyup.enter="onKeyGuard"
    @keyup.space="onKeyGuard"
  >
    <!-- 优先 slot（纯 Vue 宿主）；icon path 时用 Web Component ion-icon（宿主需加载 Ionic core） -->
    <slot>
      <ion-icon v-if="icon" :icon="icon" aria-hidden="true" />
    </slot>
  </button>
</template>

<script setup lang="ts">
/**
 * happier-ui：纯 Vue 图标触控。
 * 不 import @ionic/vue，避免无 Ionic 宿主无法解析依赖。
 * - slot：任意图标（SVG 等）
 * - icon：ionicons path data，依赖宿主页面已注册/加载 ion-icon
 */
const props = withDefaults(defineProps<{
  icon?: string
  ariaLabel: string
  disabled?: boolean
  size?: 'md' | 'lg'
  variant?: 'default' | 'on-media'
  color?: string
  stopPropagation?: boolean
}>(), {
  icon: undefined,
  disabled: false,
  size: 'md',
  variant: 'default',
  color: undefined,
  stopPropagation: false,
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const onClick = (event: MouseEvent) => {
  if (props.stopPropagation) {
    event.stopPropagation()
  }
  emit('click', event)
}

const onKeyGuard = (event: KeyboardEvent) => {
  if (props.stopPropagation) {
    event.stopPropagation()
  }
}
</script>

<style scoped>
.h-icon-button {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: var(--h-radius-control, 12px);
  background: transparent;
  color: var(--h-color-ink-muted, #92949c);
  cursor: pointer;
  appearance: none;
  -webkit-tap-highlight-color: transparent;
  transition: background-color var(--h-duration-press, 120ms) var(--h-ease-standard, ease),
    color var(--h-duration-press, 120ms) var(--h-ease-standard, ease);
}

.h-icon-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.h-icon-button:focus-visible {
  outline: 2px solid var(--h-color-primary, #006fee);
  outline-offset: 2px;
}

.h-icon-button:not(:disabled):active {
  background: var(--h-color-playing-bg-soft, rgba(0, 111, 238, 0.08));
}

.h-icon-button--md {
  width: var(--h-touch-target, 48px);
  height: var(--h-touch-target, 48px);
  min-width: var(--h-touch-target, 48px);
  min-height: var(--h-touch-target, 48px);
}

.h-icon-button--lg {
  width: calc(var(--h-touch-target, 48px) + 8px);
  height: calc(var(--h-touch-target, 48px) + 8px);
  min-width: calc(var(--h-touch-target, 48px) + 8px);
  min-height: calc(var(--h-touch-target, 48px) + 8px);
}

.h-icon-button :deep(ion-icon),
.h-icon-button :deep(svg) {
  width: var(--h-icon-size-md, 22px);
  height: var(--h-icon-size-md, 22px);
  font-size: var(--h-icon-size-md, 22px);
  pointer-events: none;
}

.h-icon-button--lg :deep(ion-icon),
.h-icon-button--lg :deep(svg) {
  width: var(--h-icon-size-lg, 24px);
  height: var(--h-icon-size-lg, 24px);
  font-size: var(--h-icon-size-lg, 24px);
}

.h-icon-button--danger {
  color: var(--h-color-danger, #eb445a);
}

.h-icon-button--on-media {
  color: var(--h-immersive-ink-soft, rgba(255, 255, 255, 0.68));
}

.h-icon-button--on-media:not(:disabled):active {
  background: rgba(255, 255, 255, 0.08);
  color: var(--h-immersive-ink, #ffffff);
}
</style>
