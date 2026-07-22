<template>
  <div
    class="h-list-row"
    :class="{
      'h-list-row--playing': playing,
      'h-list-row--button': button,
    }"
    :role="button ? 'button' : undefined"
    :tabindex="button ? 0 : undefined"
    :aria-current="playing ? 'true' : undefined"
    @click="onClick"
    @keydown.enter.prevent="onActivate"
    @keydown.space.prevent="onActivate"
  >
    <div v-if="showStart" class="h-list-row__start">
      <slot name="start" />
    </div>

    <div class="h-list-row__body">
      <h2 class="h-list-row__title">
        <span v-if="playing && showPlayingIndicator" class="h-list-row__playing-mark" aria-hidden="true">♪ </span>
        {{ title }}
      </h2>
      <p v-if="subtitle" class="h-list-row__subtitle">{{ subtitle }}</p>
    </div>

    <div v-if="$slots.end" class="h-list-row__end">
      <slot name="end" />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * happier-ui：纯 Vue 列表行。无默认封面（由宿主 start 槽提供）。
 */
import { computed, useSlots } from 'vue'

const props = withDefaults(defineProps<{
  title: string
  subtitle?: string
  playing?: boolean
  button?: boolean
  /** 无 start 槽时是否仍预留起始区（一般 false） */
  showStartWhenEmpty?: boolean
  showPlayingIndicator?: boolean
}>(), {
  subtitle: undefined,
  playing: false,
  button: true,
  showStartWhenEmpty: false,
  showPlayingIndicator: false,
})

const emit = defineEmits<{
  click: [event: MouseEvent | KeyboardEvent]
}>()

const slots = useSlots()

const showStart = computed(() => Boolean(slots.start) || props.showStartWhenEmpty)

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
.h-list-row {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  width: 100%;
  min-height: var(--h-song-row-height, 72px);
  margin: 0;
  padding: var(--h-space-sm, 8px) var(--h-space-sm, 8px) var(--h-space-sm, 8px)
    var(--h-space-md, 12px);
  border: none;
  background: transparent;
  color: inherit;
  text-align: start;
  font: inherit;
  -webkit-tap-highlight-color: transparent;
}

.h-list-row--button {
  cursor: pointer;
}

.h-list-row--button:focus-visible {
  outline: 2px solid var(--h-color-primary, #006fee);
  outline-offset: -2px;
}

.h-list-row--button:active {
  background: var(--h-color-playing-bg-soft, rgba(0, 111, 238, 0.08));
}

.h-list-row--playing {
  background: var(--h-color-playing-bg, rgba(0, 111, 238, 0.1));
}

.h-list-row__start {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  margin-inline-end: var(--h-space-md, 12px);
}

.h-list-row__body {
  flex: 1;
  min-width: 0;
}

.h-list-row__end {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--h-space-xs, 2px);
  margin-inline-start: var(--h-space-sm, 8px);
}

.h-list-row__title {
  margin: 0;
  overflow: hidden;
  font-size: var(--h-font-title, 15px);
  font-weight: 600;
  line-height: var(--h-line-height-title, 1.25);
  color: var(--h-color-ink, #000000);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.h-list-row__subtitle {
  margin: var(--h-space-xs, 2px) 0 0;
  overflow: hidden;
  font-size: var(--h-font-body-sm, 13px);
  color: var(--h-color-ink-muted, #92949c);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.h-list-row__playing-mark {
  color: var(--h-color-primary, #006fee);
}
</style>
