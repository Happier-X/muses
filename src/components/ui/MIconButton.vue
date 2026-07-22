<template>
  <ion-button
    class="m-icon-button"
    :class="[
      `m-icon-button--${size}`,
      `m-icon-button--${variant}`,
    ]"
    fill="clear"
    :color="color"
    :disabled="disabled"
    :aria-label="ariaLabel"
    @click="onClick"
    @keyup.enter="onKeyGuard"
    @keyup.space="onKeyGuard"
  >
    <ion-icon slot="icon-only" :icon="icon" aria-hidden="true" />
  </ion-button>
</template>

<script setup lang="ts">
import { IonButton, IonIcon } from '@ionic/vue'

const props = withDefaults(defineProps<{
  /** ion-lucide 导出的图标 data */
  icon: string
  /** 无障碍必填标签 */
  ariaLabel: string
  disabled?: boolean
  /** md=48 热区；lg 略大主控预留 */
  size?: 'md' | 'lg'
  /** default 列表/chrome；on-media 沉浸白/半透明预留 */
  variant?: 'default' | 'on-media'
  /** 透传 Ionic color（如 danger 删除） */
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
  margin: 0;
  --padding-start: 0;
  --padding-end: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  min-width: var(--muses-touch-target);
  min-height: var(--muses-touch-target);
  color: var(--muses-color-ink-muted);
}

.m-icon-button--md {
  width: var(--muses-touch-target);
  height: var(--muses-touch-target);
}

.m-icon-button--lg {
  width: calc(var(--muses-touch-target) + 8px);
  height: calc(var(--muses-touch-target) + 8px);
}

.m-icon-button ion-icon {
  font-size: var(--muses-icon-size-md);
}

.m-icon-button--lg ion-icon {
  font-size: var(--muses-icon-size-lg);
}

.m-icon-button--on-media {
  color: var(--muses-immersive-ink-soft);
}

.m-icon-button--on-media:hover,
.m-icon-button--on-media.ion-activated {
  color: var(--muses-immersive-ink);
}
</style>
