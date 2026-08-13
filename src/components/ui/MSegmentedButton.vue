<template>
  <button
    ref="elRef"
    type="button"
    class="m-segmented-button"
    :class="{ 'm-segmented-button--active': active }"
    :aria-pressed="active"
    @click="onClick"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MSegmentedButton —— 分段按钮（替代 k-segmented-button，iOS）
 * flex-1 均分、15px 文字；active 黑字/白字（strong 模式下滑块由 MSegmented 量测）。
 * 挂载时注册自身供父级测量（滑块定位）。
 */
import { inject, onBeforeUnmount, onMounted, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    active?: boolean
  }>(),
  {
    active: false,
  },
)

// props 值通过 class 绑定使用（模板内），此处仅保持声明完整性
void props

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const elRef = ref<HTMLElement | null>(null)
const register = inject<((el: HTMLElement | null) => void) | null>('m-segmented-register', null)

onMounted(() => {
  register?.(elRef.value)
})

onBeforeUnmount(() => {
  register?.(null)
})

function onClick(event: MouseEvent) {
  emit('click', event)
}
</script>

<style scoped lang="scss">
.m-segmented-button {
  flex: 1 1 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  min-width: 0;
  height: 40px;
  padding: 0 var(--m-spacing-sub);
  border: none;
  outline: none;
  appearance: none;
  background: transparent;
  cursor: pointer;
  user-select: none;
  font-family: inherit;
  font-size: 16px;
  font-weight: 500;
  line-height: 1.2;
  color: var(--m-text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  position: relative;
  -webkit-tap-highlight-color: rgba(0, 0, 0, 0);

  &--active {
    color: var(--m-primary);
    font-weight: 600;
  }
}
</style>