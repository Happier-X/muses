<template>
  <label
    class="m-toggle"
    :class="{ 'm-toggle--checked': isOn, 'm-toggle--disabled': disabled }"
  >
    <input
      type="checkbox"
      class="m-toggle__input"
      :checked="isOn"
      :disabled="disabled"
      :aria-label="ariaLabel"
      @change="onChange"
    />
    <motion.span
      class="m-toggle__thumb"
      :animate="{ x: isOn ? 20 : 0 }"
      :transition="{ type: 'spring', stiffness: 600, damping: 32, mass: 0.6 }"
    />
  </label>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MToggle —— 椒盐风格开关（Salt Player 12.2.0 实测复刻，08-15-salt-toggle-replica）
 * 46×26dp 胶囊轨道 + 16dp 白色圆环拇指（4dp 白边 + 中心 8dp 露轨道色圆点，
 * 与 SaltUI Switcher 源码 border 方案一致）；开启拇指右移 20dp、轨道色 300ms 渐变。
 * 事件契约：原生 change（e.target.checked 可用）+ update:modelValue（与 Konsta 时代一致）。
 */
import { computed } from 'vue'
import { motion } from 'motion-v'

const props = withDefaults(
  defineProps<{
    modelValue?: boolean
    checked?: boolean
    disabled?: boolean
    ariaLabel?: string
  }>(),
  {
    modelValue: undefined,
    checked: undefined,
    disabled: false,
    ariaLabel: undefined,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  change: [event: Event]
}>()

/** 有效开关值：modelValue 优先，兼容 Konsta 时代 :checked 用法 */
const isOn = computed(() => props.modelValue ?? props.checked ?? false)

function onChange(event: Event) {
  if (props.disabled) return
  const next = (event.target as HTMLInputElement).checked
  emit('update:modelValue', next)
  emit('change', event)
}
</script>

<style scoped lang="scss">
.m-toggle {
  /* 关闭态轨道色 = subText @ 10% 叠表面（浅 #E9E9E9 / 深约 #333435），深色由 :global(.dark) 覆盖 */
  --m-toggle-track-off: #e9e9e9;

  display: inline-block;
  position: relative;
  width: 46px;
  height: 26px;
  border-radius: 9999px;
  background-color: var(--m-toggle-track-off);
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: rgba(0, 0, 0, 0);
  box-sizing: border-box;
  transition: background-color 0.3s ease; /* 轨道色 300ms 渐变（SaltUI tween 300） */

  &--checked {
    background-color: var(--m-primary);
  }

  &--disabled {
    opacity: 0.5;
    pointer-events: none;
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

  &:active .m-toggle__thumb {
    scale: 1.08;
  }

  /* 白色圆环拇指：16dp 圆 + 4dp 白边 + 透明中心 → 中心 8dp 露轨道色（开启蓝点/关闭灰点） */
  &__thumb {
    position: absolute;
    top: 5px;
    left: 5px;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    border: 4px solid #fff;
    box-sizing: border-box;
    background-color: transparent;
  }
}

:global(.dark .m-toggle) {
  --m-toggle-track-off: #333435;
}
</style>
