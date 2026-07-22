<template>
  <ion-item
    class="m-list-row"
    :class="{ 'm-list-row--playing': playing }"
    :button="button"
    :detail="detail"
    lines="none"
    :aria-current="playing ? 'true' : undefined"
    @click="onClick"
  >
    <div v-if="showStart" slot="start" class="m-list-row__start">
      <slot name="start">
        <m-cover
          :src="coverSrc"
          :size="coverSize"
          :radius="coverRadius"
          alt=""
        />
      </slot>
    </div>

    <ion-label>
      <h2 class="m-list-row__title">
        <span v-if="playing && showPlayingIndicator" class="m-list-row__playing-mark" aria-hidden="true">♪ </span>
        {{ title }}
      </h2>
      <p v-if="subtitle" class="m-list-row__subtitle">{{ subtitle }}</p>
    </ion-label>

    <div v-if="$slots.end" slot="end" class="m-list-row__end">
      <slot name="end" />
    </div>
  </ion-item>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
import { IonItem, IonLabel } from '@ionic/vue'
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
  click: [event: MouseEvent]
}>()

const slots = useSlots()

const showStart = computed(() => Boolean(slots.start) || props.cover)

const onClick = (event: MouseEvent) => {
  emit('click', event)
}
</script>

<style scoped>
.m-list-row {
  --min-height: var(--muses-song-row-height);
  --background: transparent;
  --background-activated: var(--muses-color-playing-bg-soft);
  --padding-start: var(--muses-space-md);
  --inner-padding-end: var(--muses-space-sm);
}

.m-list-row--playing {
  --background: var(--muses-color-playing-bg);
}

.m-list-row__start {
  display: flex;
  align-items: center;
  margin-inline-end: var(--muses-space-md);
}

.m-list-row__end {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--muses-space-xs);
}

.m-list-row__title {
  margin: 0;
  font-size: var(--muses-font-title);
  font-weight: 600;
  line-height: var(--muses-line-height-title);
  color: var(--muses-color-ink);
}

.m-list-row__subtitle {
  margin: var(--muses-space-xs) 0 0;
  font-size: var(--muses-font-body-sm);
  color: var(--muses-color-ink-muted);
}

.m-list-row__playing-mark {
  color: var(--muses-color-primary);
}
</style>
