<template>
  <div
    class="m-segmented"
    :class="{
      'm-segmented--rounded': rounded,
      'm-segmented--strong': strong,
    }"
  >
    <!-- 滑块：motion-v 驱动 left/width（JS 量 active 按钮 offset） -->
    <motion.span
      v-if="strong && highlightVisible"
      class="m-segmented__highlight"
      :class="{ 'm-segmented__highlight--rounded': rounded }"
      :animate="{ left: highlightLeft, width: highlightWidth }"
      :transition="{ duration: 0.2, ease: 'easeOut' }"
    />
    <slot />
  </div>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MSegmented —— 分段控件（替代 k-segmented，iOS）
 * strong：灰底凹槽 + 白滑块（滑块走 motion-v animate left/width，量 active 按钮）。
 * rounded：全圆形态（CategoriesPage 用法）。outline 形态（主色描边分栏）也支持。
 */
import { motion } from 'motion-v'
import { nextTick, onMounted, provide, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    strong?: boolean
    rounded?: boolean
    outline?: boolean
  }>(),
  {
    strong: false,
    rounded: false,
    outline: false,
  },
)

const rootRef = ref<HTMLElement | null>(null)
const highlightLeft = ref(0)
const highlightWidth = ref(0)
const highlightVisible = ref(false)

/** 供 MSegmentedButton 注册自身 DOM（父查询 active 按钮位置） */
provide('m-segmented-register', (el: HTMLElement | null) => {
  if (el) {
    register(el)
  }
})

const registered = new Set<HTMLElement>()

function register(el: HTMLElement) {
  registered.add(el)
  scheduleMeasure()
}

function scheduleMeasure() {
  void nextTick(() => measure())
}

function measure() {
  const root = rootRef.value
  if (!root) return
  const active = root.querySelector<HTMLElement>('.m-segmented-button--active')
  if (!active) {
    highlightVisible.value = false
    return
  }
  highlightLeft.value = active.offsetLeft
  highlightWidth.value = active.offsetWidth
  highlightVisible.value = true
}

onMounted(() => {
  scheduleMeasure()
})

// 容器尺寸/激活态变化后重测（slot 内容更新在 updated 之后）
watch(
  () => props.strong,
  () => scheduleMeasure(),
)
</script>

<style scoped lang="scss">
.m-segmented {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  width: 100%;
  box-sizing: border-box;

  &--rounded {
    border-radius: 9999px;
  }

  &:not(&--rounded) {
    border-radius: 4px;
  }

  &--outline {
    border: 2px solid var(--m-primary);
    border-radius: 4px;
  }

  &--strong {
    position: relative;
    gap: 4px;
    padding: 2px;
    background-color: rgba(0, 0, 0, 0.05);

    &:not(.m-segmented--rounded) {
      border-radius: 4px;
    }
  }

  &__highlight {
    position: absolute;
    top: 2px;
    bottom: 2px;
    background-color: var(--m-surface-1);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
    pointer-events: none;

    &--rounded {
      border-radius: 9999px;
    }
  }
}

:global(.dark) .m-segmented--strong {
  background-color: rgba(255, 255, 255, 0.1);
}
</style>