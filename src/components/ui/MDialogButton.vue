<template>
  <m-button
    :variant="variant"
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
 */
import { computed } from 'vue'
import MButton from './MButton.vue'

const props = withDefaults(
  defineProps<{
    strong?: boolean
    disabled?: boolean
  }>(),
  {
    strong: false,
    disabled: false,
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