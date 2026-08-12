<template>
  <a
    class="m-navbar-back-link"
    role="button"
    tabindex="0"
    :aria-label="ariaLabel || text"
    @click="onClick"
    @keyup.enter="onClick"
  >
    <span class="m-navbar-back-link__icon" aria-hidden="true">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="12"
        height="20"
        viewBox="0 0 12 20"
        fill="currentcolor"
      >
        <path
          d="M10.6737904,1.29289322 C11.0342516,1.65335447 11.0619794,2.22054978 10.7569738,2.61281627 L10.6737458,2.70706222 L3.76756619,9.61235263 C3.5939889,9.78590804 3.57468677,10.0553312 3.70967055,10.2502079 L3.76753111,10.3194689 L10.673816,17.2262348 C11.0643303,17.6167774 11.0643188,18.2499456 10.6737904,18.640474 C10.2832661,19.0309983 9.65010112,19.0309983 9.25957683,18.640474 L1.29289322,10.6737904 C0.902368927,10.2832661 0.902368927,9.65010112 1.29289322,9.25957683 L9.25957683,1.29289322 C9.62006079,0.932409257 10.1872918,0.904679722 10.5795831,1.20970461 L10.6737904,1.29289322 Z"
        />
      </svg>
    </span>
    <span v-if="showText || text !== 'Back'" class="m-navbar-back-link__text">
      {{ text }}
    </span>
    <slot />
  </a>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MNavbarBackLink —— 导航栏返回（替代 k-navbar-back-link，iOS）
 * 主色 17px 文字 + Konsta BackIcon 原路径。showText 控制文字显隐
 * （iOS 系统默认不显示文字，页面可传 show-text）。
 */
withDefaults(
  defineProps<{
    text?: string
    showText?: boolean
    ariaLabel?: string
  }>(),
  {
    text: 'Back',
    showText: false,
    ariaLabel: undefined,
  },
)

const emit = defineEmits<{
  click: [event: Event]
}>()

function onClick(event: Event) {
  emit('click', event)
}
</script>

<style scoped lang="scss">
.m-navbar-back-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  cursor: pointer;
  user-select: none;
  color: var(--m-primary);
  font-size: 17px;
  font-weight: 500;
  line-height: 1;
  -webkit-tap-highlight-color: rgba(0, 0, 0, 0);

  &__icon {
    display: flex;
    align-items: center;
    width: 12px;
    height: 20px;
  }

  &__text {
    white-space: nowrap;
  }
}
</style>