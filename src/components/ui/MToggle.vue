<template>
  <button
    type="button"
    role="switch"
    class="m-toggle"
    :class="{ 'm-toggle--checked': isOn, 'm-toggle--disabled': disabled }"
    :aria-checked="isOn"
    :disabled="disabled"
    @click="onToggle"
  >
    <motion.span
      class="m-toggle__thumb"
      :animate="{ x: isOn ? 22 : 0 }"
      :transition="{ type: 'spring', stiffness: 600, damping: 32, mass: 0.6 }"
    >
      <span class="m-toggle__thumb-core" />
    </motion.span>
  </button>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MToggle —— iOS 开关（替代 k-toggle）
 * 64×28 轨道 + p-0.5 + 24px 白拇指；选中主色底、拇指位移 22px（Konsta iOS 原值）。
 * 拇指位移走 motion-v spring；轨道底色为纯颜色过渡（CSS 例外保留）。
 */
import { computed } from 'vue'
import { motion } from 'motion-v'

const props = withDefaults(
  defineProps<{
    modelValue?: boolean
    checked?: boolean
    disabled?: boolean
  }>(),
  {
    modelValue: undefined,
    checked: undefined,
    disabled: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  change: [event: Event]
}>()

/** 有效开关值：modelValue 优先，兼容 Konsta 时代 :checked 用法 */
const isOn = computed(() => props.modelValue ?? props.checked ?? false)

function onToggle(event: MouseEvent) {
  if (props.disabled) return
  const next = !isOn.value
  emit('update:modelValue', next)
  // 兼容 k-toggle 的 change 用法（Settings 页 :checked + @change）
  emit('change', event)
}
</script>

<style scoped lang="scss">
.m-toggle {
  box-sizing: border-box;
  display: inline-block;
  position: relative;
  width: 64px;
  height: 28px;
  padding: 2px;
  border: none;
  border-radius: 9999px;
  background-color: var(--m-surface-1-shade);
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: rgba(0, 0, 0, 0);
  /* 轨道底色纯颜色过渡（例外保留） */
  transition: background-color 0.2s ease;
  outline: none;

  &--checked {
    background-color: var(--m-primary);
  }

  &--disabled {
    opacity: 0.5;
    pointer-events: none;
  }

  &:active .m-toggle__thumb-core {
    transform: scale(1.15);
  }

  &__thumb {
    position: absolute;
    top: 2px;
    left: 2px;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    box-shadow: var(--m-shadow-ios-thumb);
  }

  &__thumb-core {
    display: block;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background-color: #fff;
  }
}
</style>