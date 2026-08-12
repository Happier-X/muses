<template>
  <label class="m-checkbox" :class="{ 'm-checkbox--disabled': disabled }">
    <input
      type="checkbox"
      class="m-checkbox__input"
      :checked="checked"
      :disabled="disabled"
      @change="onChange"
    />
    <span class="m-checkbox__icon" :class="{ 'm-checkbox__icon--checked': checked }">
      <motion.span
        class="m-checkbox__check"
        :animate="{ opacity: checked ? 1 : 0, scale: checked ? 1 : 0.5 }"
        :transition="{ duration: 0.15, ease: 'easeOut' }"
      >
        <svg
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentcolor"
          stroke-width="3.5"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M20 6 9 17l-5-5" />
        </svg>
      </motion.span>
    </span>
    <span class="m-checkbox__label">
      <slot />
    </span>
  </label>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MCheckbox —— iOS 圆形勾选框（替代 k-checkbox）
 * 22×22 圆、1px 灰描边；勾选主色圆底 + 白色对勾（motion-v 0.15s 淡入）。
 * 事件契约：原生 change 事件（事件源为内部 input，e.target.checked 可用）+ update:checked。
 */
import { motion } from 'motion-v'

const props = withDefaults(
  defineProps<{
    checked?: boolean
    disabled?: boolean
  }>(),
  {
    checked: false,
    disabled: false,
  },
)

const emit = defineEmits<{
  change: [event: Event]
  'update:checked': [value: boolean]
}>()

function onChange(event: Event) {
  if (props.disabled) return
  emit('change', event)
  emit('update:checked', (event.target as HTMLInputElement).checked)
}
</script>

<style scoped lang="scss">
.m-checkbox {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
  position: relative;
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: rgba(0, 0, 0, 0);

  &--disabled {
    cursor: default;
    opacity: 0.5;
  }

  &__input {
    position: absolute;
    width: 1px;
    height: 1px;
    margin: -1px;
    padding: 0;
    border: 0;
    clip-path: inset(50%);
    overflow: hidden;
    white-space: nowrap;
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    width: 22px;
    height: 22px;
    flex: 0 0 22px;
    border-radius: 50%;
    border: 1px solid rgba(0, 0, 0, 0.3);
    color: #fff;
    /* 勾选底色的颜色过渡（纯颜色例外） */
    transition:
      background-color 0.1s ease,
      border-color 0.1s ease;

    &--checked {
      background-color: var(--m-primary);
      border-color: var(--m-primary);
    }
  }

  &__check {
    display: flex;
    width: 14px;
    height: 14px;
  }

  &__label {
    margin-left: 6px;
  }
}

:global(.dark) .m-checkbox__icon {
  border-color: rgba(255, 255, 255, 0.3);
}
</style>