<template>
  <m-button
    :variant="variant"
    :danger="danger"
    size="large"
    rounded
    :disabled="disabled"
    @click="onClick"
  >
    <slot />
  </m-button>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MDialogButton —— 对话框按钮（替代 k-dialog-button，iOS）
 * 大号全圆按钮：非 strong = tonal（主色 15% 底 + 主色字）；strong = 主色实底白字。
 * danger：strong 时红底（删除确认，替代 Konsta colors 覆盖）。
 */
import { computed } from 'vue'
import MButton from './MButton.vue'

const props = withDefaults(
  defineProps<{
    strong?: boolean
    disabled?: boolean
    danger?: boolean
  }>(),
  {
    strong: false,
    disabled: false,
    danger: false,
  },
)

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const variant = computed(() => (props.strong ? 'fill' : 'tonal'))

function onClick(event: MouseEvent) {
  emit('click', event)
}
</script>

<style scoped lang="scss">
:deep(.m-button) {
  border-radius: var(--m-radius-md);
}
</style>