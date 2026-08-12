<template>
  <div
    class="m-list-item"
    :class="{
      'm-list-item--link': link,
      'm-list-item--divider': dividers,
      'm-list-item--strong': strong,
    }"
  >
    <slot name="media" />
    <div class="m-list-item__inner">
      <slot name="header">{{ header }}</slot>
      <div class="m-list-item__title-wrap">
        <div class="m-list-item__title" :class="{ 'm-list-item__title--strong': strongTitle }">
          <slot>{{ title }}</slot>
        </div>
        <div class="m-list-item__after">
          <slot name="after" />
        </div>
      </div>
      <div v-if="subtitle" class="m-list-item__subtitle">{{ subtitle }}</div>
      <div v-if="text" class="m-list-item__text">{{ text }}</div>
      <slot name="footer">{{ footer }}</slot>
    </div>
    <span v-if="chevron" class="m-list-item__chevron" aria-hidden="true">
      <svg
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentcolor"
        stroke-width="2.5"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="m9 18 6-6-6-6" />
      </svg>
    </span>
  </div>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MListItem —— 列表行（替代 k-list-item，iOS）
 * title + 默认 slot；subtitle 13px 次级；after 右侧插槽；media 左侧缩略图插槽；
 * text 多行文本 13px；header/footer 12px 说明；chevron 右箭头（link 提示）；
 * dividers 默认从 MList 注入（iOS 列表行间 hairline）。
 */
import { computed, inject, type Ref } from 'vue'

interface MListState {
  strong: boolean
  dividers: boolean
}

const props = withDefaults(
  defineProps<{
    title?: string
    subtitle?: string
    text?: string
    header?: string
    footer?: string
    link?: boolean
    chevron?: boolean
    strong?: boolean
    /** 标题加粗（Konsta strongTitle 语义） */
    strongTitle?: boolean
    dividers?: boolean
  }>(),
  {
    title: undefined,
    subtitle: undefined,
    text: undefined,
    header: undefined,
    footer: undefined,
    link: false,
    chevron: false,
    strong: false,
    strongTitle: false,
    dividers: undefined,
  },
)

const listState = inject<Ref<MListState> | null>('m-list-state', null)

/** 分隔线：自身 prop 优先，否则跟随 MList（Konsta iOS 默认开） */
const dividers = computed(() => props.dividers ?? listState?.value?.dividers ?? true)
</script>

<style scoped lang="scss">
.m-list-item {
  display: flex;
  align-items: center;
  box-sizing: border-box;
  width: 100%;
  position: relative;
  color: var(--m-text);
  padding-left: calc(16px + var(--m-safe-area-left, 0px));

  &--link {
    cursor: pointer;
    user-select: none;

    &:active {
      background-color: rgba(0, 0, 0, 0.1);
    }

    &:active:has(.m-toggle:active) {
      background-color: transparent;
    }
  }

  &--strong {
    padding-left: calc(16px + var(--m-safe-area-left, 0px));
  }

  &--divider {
    &::after {
      content: '';
      position: absolute;
      left: calc(16px + var(--m-safe-area-left, 0px));
      right: 0;
      bottom: 0;
      height: 1px;
      background-color: var(--m-hairline);
      transform-origin: center bottom;
      transform: scaleY(calc(1 / var(--m-device-pixel-ratio, 1)));
      z-index: 10;
    }
  }

  &__inner {
    position: relative;
    width: 100%;
    padding: 12px calc(16px + var(--m-safe-area-right, 0px)) 12px 0;
  }

  &__title-wrap {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 28px;
  }

  &__title {
    font-size: 17px;
    line-height: 1.35;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &--strong {
      font-weight: 600;
    }
  }

  &__after {
    flex-shrink: 0;
    margin-left: auto;
    padding-left: 4px;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  &__subtitle {
    font-size: 13px;
    line-height: 1.35;
    margin-top: 2px;
    color: var(--m-text-2);
  }

  &__text {
    font-size: 13px;
    line-height: 1.4;
    margin-top: 2px;
    color: var(--m-text-2);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__header,
  &__footer {
    font-size: 12px;
    margin-top: 2px;
    color: var(--m-text-2);
  }

  &__chevron {
    flex-shrink: 0;
    opacity: 0.2;
    margin-left: 12px;
    margin-right: calc(4px + var(--m-safe-area-right, 0px));
    display: flex;
    align-items: center;
  }
}

:global(.dark) .m-list-item--link:active {
  background-color: rgba(255, 255, 255, 0.1);
}

:global(.dark) .m-list-item--link:active:has(.m-toggle:active) {
  background-color: transparent;
}
</style>