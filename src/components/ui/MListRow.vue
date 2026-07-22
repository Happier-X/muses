<template>
  <h-list-row
    class="m-list-row"
    :title="title"
    :subtitle="subtitle"
    :playing="playing"
    :button="button"
    :show-playing-indicator="showPlayingIndicator"
    @click="onClick"
  >
    <template v-if="showStart" #start>
      <slot name="start">
        <m-cover
          :src="coverSrc"
          :size="coverSize"
          :radius="coverRadius"
          alt=""
        />
      </slot>
    </template>
    <template v-if="$slots.end" #end>
      <slot name="end" />
    </template>
  </h-list-row>
</template>

<script setup lang="ts">
/**
 * App 包装：HListRow + 默认 MCover（coverSrc API 兼容现网）
 */
import { computed, useSlots } from 'vue'
import { HListRow } from 'happier-ui'
import MCover from './MCover.vue'

const props = withDefaults(defineProps<{
  title: string
  subtitle?: string
  coverSrc?: string | null
  playing?: boolean
  button?: boolean
  detail?: boolean
  cover?: boolean
  coverSize?: 'sm' | 'md' | number
  coverRadius?: 'sm' | 'md'
  showPlayingIndicator?: boolean
}>(), {
  subtitle: undefined,
  coverSrc: null,
  playing: false,
  button: true,
  detail: false,
  cover: true,
  coverSize: 'md',
  coverRadius: 'md',
  showPlayingIndicator: false,
})

const emit = defineEmits<{
  click: [event: MouseEvent | KeyboardEvent]
}>()

const slots = useSlots()

const showStart = computed(() => Boolean(slots.start) || props.cover)

const onClick = (event: MouseEvent | KeyboardEvent) => {
  emit('click', event)
}
</script>
