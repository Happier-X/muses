<template>
  <button
    type="button"
    class="m-icon-button"
    :class="[
      `m-icon-button--${size}`,
      `m-icon-button--${variant}`,
      color ? `m-icon-button--${color}` : null,
    ]"
    :disabled="disabled"
    :aria-label="ariaLabel"
    @click="onClick"
    @keyup.enter="onKeyGuard"
    @keyup.space="onKeyGuard"
  >
    <ion-icon :icon="icon" aria-hidden="true" />
  </button>
</template>

<script setup lang="ts">
/**
 * happier-ui 候选：纯 Vue 触控按钮。
 * ion-icon 仅用于渲染宿主传入的 SVG path data，不依赖 ion-lucide 映射表。
 */
import { IonIcon } from '@ionic/vue'

const props = withDefaults(defineProps<{
  /** 图标 path data（由宿主如 ion-lucide 传入） */
  icon: string
  /** 无障碍必填标签 */
  ariaLabel: string
  disabled?: boolean
  /** md=48 热区；lg 略大主控预留 */
  size?: 'md' | 'lg'
  /** default 列表/chrome；on-media 沉浸白/半透明预留 */
  variant?: 'default' | 'on-media'
  /** 语义色：danger 等 */
  color?: string
  stopPropagation?: boolean
}>(), {
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

/** 嵌套在可点父级（如 MiniPlayer）时，避免 Enter/Space 冒泡打开父级 */
const onKeyGuard = (event: KeyboardEvent) => {
  if (props.stopPropagation) {
    event.stopPropagation()
  }
}
</script>

<style scoped>
.m-icon-button {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: var(--h-radius-control, var(--muses-radius-cover, 12px));
  background: transparent;
  color: var(--h-color-ink-muted, var(--muses-color-ink-muted));
  cursor: pointer;
  appearance: none;
  -webkit-tap-highlight-color: transparent;
  transition: background-color var(--h-duration-press, 120ms) var(--h-ease-standard, ease),
    color var(--h-duration-press, 120ms) var(--h-ease-standard, ease);
}

.m-icon-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.m-icon-button:focus-visible {
  outline: 2px solid var(--h-color-primary, var(--muses-color-primary));
  outline-offset: 2px;
}

.m-icon-button:not(:disabled):active {
  background: var(--h-color-playing-bg-soft, var(--muses-color-playing-bg-soft));
}

.m-icon-button--md {
  width: var(--h-touch-target, var(--muses-touch-target));
  height: var(--h-touch-target, var(--muses-touch-target));
  min-width: var(--h-touch-target, var(--muses-touch-target));
  min-height: var(--h-touch-target, var(--muses-touch-target));
}

.m-icon-button--lg {
  width: calc(var(--h-touch-target, var(--muses-touch-target)) + 8px);
  height: calc(var(--h-touch-target, var(--muses-touch-target)) + 8px);
  min-width: calc(var(--h-touch-target, var(--muses-touch-target)) + 8px);
  min-height: calc(var(--h-touch-target, var(--muses-touch-target)) + 8px);
}

.m-icon-button ion-icon {
  font-size: var(--h-icon-size-md, var(--muses-icon-size-md));
  pointer-events: none;
}

.m-icon-button--lg ion-icon {
  font-size: var(--h-icon-size-lg, var(--muses-icon-size-lg));
}

.m-icon-button--danger {
  color: var(--h-color-danger, var(--ion-color-danger, #eb445a));
}

.m-icon-button--on-media {
  color: var(--h-immersive-ink-soft, var(--muses-immersive-ink-soft));
}

.m-icon-button--on-media:not(:disabled):active {
  background: rgba(255, 255, 255, 0.08);
  color: var(--h-immersive-ink, var(--muses-immersive-ink));
}
</style>
