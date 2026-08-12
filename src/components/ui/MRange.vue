<template>
  <div class="m-range">
    <span ref="trackBgRef" class="m-range__track-bg" />
    <span class="m-range__track-value" :style="{ width: `${valuePercent}%` }" />
    <input
      class="m-range__input"
      type="range"
      :min="min"
      :max="max"
      :step="step"
      :value="modelValue"
      :disabled="disabled"
      :aria-label="ariaLabel"
      @input="onInput"
      @change="onChange"
    />
    <span
      class="m-range__thumb-wrap"
      :style="{ insetInlineStart: `${thumbOffsetPercent}%` }"
    >
      <span class="m-range__thumb-shadow" />
      <span class="m-range__thumb" />
    </span>
  </div>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MRange —— iOS 滑块（替代 k-range）
 * DOM 结构对齐 Konsta（trackBg / trackValue / input / thumbWrap(thumbShadow, thumb)），
 * PlayerPage 的 .progress-range 沉浸覆盖规则（nth-child hack）对同构结构持续有效。
 * 位置更新即 input 事件直驱 style（状态渲染，非动画）；拖动实时跟手、无延迟。
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    /** v-model（自研首选） */
    modelValue?: number
    /** Konsta k-range 兼容：:value="n" 只读受控 */
    value?: number
    min?: number
    max?: number
    step?: number
    disabled?: boolean
    ariaLabel?: string
  }>(),
  {
    modelValue: undefined,
    value: undefined,
    min: 0,
    max: 100,
    step: 1,
    disabled: false,
    ariaLabel: undefined,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: number]
  input: [event: Event]
  change: [event: Event]
}>()

const trackBgRef = ref<HTMLElement | null>(null)
/** 拇指宽度占轨道宽度比例（Konsta 同款测量：位置 = value% × (1 - thumb/track)） */
const thumbRatio = ref(0)

/** 实际受控值：modelValue 优先，其次 k-range 兼容的 value */
const modelValue = computed(() => props.modelValue ?? props.value ?? props.min)

const valuePercent = computed(() => {
  const range = props.max - props.min
  if (range <= 0) return 0
  return ((modelValue.value - props.min) / range) * 100
})

const thumbOffsetPercent = computed(() => valuePercent.value * (1 - thumbRatio.value))

function measure() {
  const track = trackBgRef.value
  if (!track) return
  const trackWidth = track.offsetWidth
  if (trackWidth <= 0) return
  // iOS 拇指热区 38px（与 Konsta w-9.5 一致）
  thumbRatio.value = Math.min(38 / trackWidth, 0.9)
}

function onInput(event: Event) {
  emit('input', event)
  emit('update:modelValue', Number((event.target as HTMLInputElement).value))
}

function onChange(event: Event) {
  emit('change', event)
}

onMounted(() => {
  measure()
  window.addEventListener('resize', measure)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', measure)
})

// 轨道宽度变化（容器尺寸变化）时重新测量
watch(
  () => trackBgRef.value?.offsetWidth,
  () => measure(),
)
</script>

<style scoped lang="scss">
.m-range {
  display: block;
  position: relative;
  width: 100%;
  height: 28px;
  align-self: center;
  user-select: none;
  touch-action: pan-y;

  &__track-bg {
    position: absolute;
    top: 50%;
    left: 0;
    width: 100%;
    height: 6px;
    border-radius: 9999px;
    background-color: rgba(0, 0, 0, 0.2);
    transform: translateY(-50%);
  }

  &__track-value {
    position: absolute;
    top: 50%;
    left: 0;
    height: 6px;
    border-radius: 9999px;
    background-color: var(--m-primary);
    transform: translateY(-50%);
  }

  &__input {
    appearance: none;
    width: 100%;
    height: 28px;
    background: transparent;
    cursor: pointer;
    display: block;
    position: relative;
    outline: none;

    &::-webkit-slider-thumb {
      appearance: none;
      opacity: 0;
      width: 38px;
      height: 24px;
      margin-top: -12px;
      border: none;
    }

    &::-webkit-slider-runnable-track {
      appearance: none;
      height: 1px;
    }

    &:disabled {
      cursor: default;
    }
  }

  /* 拇指热区/视觉元素：Konsta 同构（active 时玻璃发光反馈） */
  &__thumb-wrap {
    position: absolute;
    top: 50%;
    left: 0;
    width: 38px;
    height: 24px;
    margin-top: -12px;
    border-radius: 9999px;
    background-color: #fff;
    box-shadow: var(--m-shadow-ios-thumb);
    pointer-events: none;
    user-select: none;
    /* 定位与 Konsta 一致：左边缘 = 值百分比（hack 与测量依赖此语义） */
  }

  &__thumb-shadow,
  &__thumb {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    border-radius: 9999px;
    opacity: 0;
    pointer-events: none;
  }

  &__thumb-shadow {
    box-shadow: 0 0 40px 10px rgba(0, 122, 255, 0.75);
  }

  &__thumb {
    box-shadow: var(--m-shadow-ios-light-glass-thumb);
  }

  &:has(input:active) &__thumb-wrap {
    transform: scale(1.4);
  }

  &:has(input:active) &__thumb-shadow,
  &:has(input:active) &__thumb {
    opacity: 1;
  }
}

:global(.dark) .m-range {
  &__track-bg {
    background-color: rgba(255, 255, 255, 0.2);
  }

  &__thumb {
    background-color: rgba(255, 255, 255, 0.1);
    box-shadow: var(--m-shadow-ios-dark-glass-thumb);
  }
}
</style>