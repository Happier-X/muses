<template>
  <button
    type="button"
    class="m-actions-button"
    :class="{ 'm-actions-button--bold': bold, 'm-actions-button--danger': danger }"
    @click="onClick"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MActionsButton —— 操作项（替代 k-actions-button，iOS）
 * 56px 行、17px 主色文字居中；bold 加粗；danger 红字（替代 Konsta
 * colors 覆盖：`--colors="{ textIos: 'text-[#ff3b30]' }"` 的迁移位）。
 * 按住反馈黑/10（暗色白/5）。
 */
withDefaults(
  defineProps<{
    bold?: boolean
    danger?: boolean
  }>(),
  {
    bold: false,
    danger: false,
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
.m-actions-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 56px;
  padding: 0 16px;
  box-sizing: border-box;
  border: none;
  outline: none;
  appearance: none;
  background: transparent;
  cursor: pointer;
  user-select: none;
  overflow: hidden;
  position: relative;
  font-family: inherit;
  font-size: 17px;
  font-weight: 500;
  line-height: 1.3;
  color: var(--m-primary);
  -webkit-tap-highlight-color: rgba(0, 0, 0, 0);

  &:active {
    background-color: rgba(0, 0, 0, 0.1);
  }

  &--bold {
    font-weight: 600;
  }

  &--danger {
    color: var(--m-danger);
  }
}

:global(.dark) .m-actions-button:active {
  background-color: rgba(255, 255, 255, 0.05);
}

:global(.dark) .m-actions-button--danger {
  color: var(--m-danger);
}
</style>