<template>
  <div
    class="m-list-row"
    :class="{
      'm-list-row--playing': playing,
      'm-list-row--button': button,
    }"
    :role="button ? 'button' : undefined"
    :tabindex="button ? 0 : undefined"
    :aria-current="playing ? 'true' : undefined"
    @click="onClick"
    @keydown.enter.prevent="onActivate"
    @keydown.space.prevent="onActivate"
  >
    <div v-if="showStart" class="m-list-row__start">
      <slot name="start">
        <m-cover
          :src="coverSrc"
          :size="coverSize"
          :radius="coverRadius"
          alt=""
        />
      </slot>
    </div>

    <div class="m-list-row__body">
      <h2 class="m-list-row__title">
        <span v-if="playing && showPlayingIndicator" class="m-list-row__playing-mark" aria-hidden="true">♪ </span>
        {{ title }}
      </h2>
      <p v-if="subtitle" class="m-list-row__subtitle">{{ subtitle }}</p>
    </div>

    <div v-if="$slots.end" class="m-list-row__end">
      <slot name="end" />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * happier-ui 候选：纯 Vue 列表行（无 ion-item）。
 * 根节点用 div，避免 end 槽内 MIconButton 形成嵌套 button。
 * 默认封面依赖 app 侧 MCover；可用 start 槽覆盖。
 */
import { computed, useSlots } from 'vue'
import MCover from './MCover.vue'

const props = withDefaults(defineProps<{
  title: string
  subtitle?: string
  coverSrc?: string | null
  playing?: boolean
  button?: boolean
  detail?: boolean
  /** 是否显示默认封面槽（无 start slot 时） */
  cover?: boolean
  coverSize?: 'sm' | 'md' | number
  coverRadius?: 'sm' | 'md'
  /** 当前曲左侧 ♪ 指示（Queue 等） */
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

const onClick = (event: MouseEvent) => {
  if (!props.button) return
  emit('click', event)
}

const onActivate = (event: KeyboardEvent) => {
  if (!props.button) return
  emit('click', event)
}
</script>

<style scoped>
.m-list-row {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  width: 100%;
  min-height: var(--h-song-row-height, var(--muses-song-row-height));
  margin: 0;
  padding: var(--h-space-sm, var(--muses-space-sm)) var(--h-space-sm, var(--muses-space-sm))
    var(--h-space-sm, var(--muses-space-sm)) var(--h-space-md, var(--muses-space-md));
  border: none;
  background: transparent;
  color: inherit;
  text-align: start;
  font: inherit;
  -webkit-tap-highlight-color: transparent;
}

.m-list-row--button {
  cursor: pointer;
}

.m-list-row--button:focus-visible {
  outline: 2px solid var(--h-color-primary, var(--muses-color-primary));
  outline-offset: -2px;
}

.m-list-row--button:active {
  background: var(--h-color-playing-bg-soft, var(--muses-color-playing-bg-soft));
}

.m-list-row--playing {
  background: var(--h-color-playing-bg, var(--muses-color-playing-bg));
}

.m-list-row__start {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  margin-inline-end: var(--h-space-md, var(--muses-space-md));
}

.m-list-row__body {
  flex: 1;
  min-width: 0;
}

.m-list-row__end {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--h-space-xs, var(--muses-space-xs));
  margin-inline-start: var(--h-space-sm, var(--muses-space-sm));
}

.m-list-row__title {
  margin: 0;
  overflow: hidden;
  font-size: var(--h-font-title, var(--muses-font-title));
  font-weight: 600;
  line-height: var(--h-line-height-title, var(--muses-line-height-title));
  color: var(--h-color-ink, var(--muses-color-ink));
  text-overflow: ellipsis;
  white-space: nowrap;
}

.m-list-row__subtitle {
  margin: var(--h-space-xs, var(--muses-space-xs)) 0 0;
  overflow: hidden;
  font-size: var(--h-font-body-sm, var(--muses-font-body-sm));
  color: var(--h-color-ink-muted, var(--muses-color-ink-muted));
  text-overflow: ellipsis;
  white-space: nowrap;
}

.m-list-row__playing-mark {
  color: var(--h-color-primary, var(--muses-color-primary));
}
</style>
