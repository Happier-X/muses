<template>
  <div
    class="m-navbar"
    :class="{ 'm-navbar--transparent': transparent, 'm-navbar--has-subnavbar': $slots.subnavbar }"
  >
    <!-- 背景壳：透明占位（has-subnavbar 时撑满整根高度；实色背景在根元素上） -->
    <div class="m-navbar__bg" aria-hidden="true" />
    <div class="m-navbar__inner">
      <div
        v-if="$slots.left || navigationDrawer"
        class="m-navbar__left"
        :class="{ 'm-navbar__left--drawer': navigationDrawer && !$slots.left }"
      >
        <slot name="left">
          <m-icon-button
            v-if="navigationDrawer"
            class="m-navbar__menu-button"
            aria-label="打开导航菜单"
            title="打开导航菜单"
            :aria-expanded="navigationDrawer.expanded.value"
            @click="openNavigationDrawer"
          >
            <component :is="menu" aria-hidden="true" class="m-navbar__menu-icon" />
          </m-icon-button>
        </slot>
      </div>
      <div class="m-navbar__title">
        <slot name="title"><slot /></slot>
      </div>
      <div v-if="$slots.right" class="m-navbar__right" :class="rightClass">
        <slot name="right" />
      </div>
    </div>
    <div v-if="$slots.subnavbar" class="m-navbar__subnavbar">
      <slot name="subnavbar" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { navigationDrawerKey } from '@/features/navigation/drawer'
import { menu } from '@/icons'
import { MIconButton } from '@/components/ui'

/**
 * APP-ONLY：MNavbar —— 吸顶导航栏（替代 k-navbar，iOS）
 * 安全区顶部 + 44px 内容行；实心表面底（与列表同色 --m-surface-1，对齐椒盐顶栏，08-16 起）。
 * 默认 slot = 标题（17px 半粗）。
 */
withDefaults(
  defineProps<{
    transparent?: boolean
    /** 透传到右侧容器（Konsta rightClass 语义） */
    rightClass?: string
  }>(),
  {
    transparent: false,
    rightClass: undefined,
  },
)

const navigationDrawer = inject(navigationDrawerKey, null)

const openNavigationDrawer = (event: MouseEvent) => {
  navigationDrawer?.open(event.currentTarget as HTMLElement)
}
</script>

<style scoped lang="scss">
.m-navbar {
  position: sticky;
  top: 0;
  z-index: 20;
  width: 100%;
  box-sizing: border-box;
  color: var(--m-text);
  padding-top: var(--m-navbar-pt, 16px);
  /* 实心表面（08-16 对齐椒盐）：与下方列表/页面底同色 --m-surface，无模糊、无内高光（玻璃观感已撤） */
  background: var(--m-surface);
  border-bottom: 1px solid var(--m-hairline);

  &__bg {
    position: absolute;
    inset: 0;
    pointer-events: none;
    background-color: transparent;
  }

  &--has-subnavbar &__bg {
    height: 100%;
  }

  &--transparent {
    background-color: transparent;
    border-bottom-color: transparent;
    box-shadow: none;
    -webkit-backdrop-filter: none;
    backdrop-filter: none;
  }

  &--transparent &__bg {
    display: none;
  }

  &__inner {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 44px;
    padding-left: calc(var(--m-spacing) + var(--m-safe-area-left, 0px));
    padding-right: calc(var(--m-spacing) + var(--m-safe-area-right, 0px));
    box-sizing: border-box;
  }

  &__left,
  &__right {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    flex: 0 0 auto;
  }

  &__right { margin-left: auto; }
  &__left { margin-right: var(--m-spacing-sub); }

  &__menu-button {
    color: var(--m-text);

    @media (min-width: 768px) {
      display: none;
    }
  }

  &__menu-icon {
    width: var(--m-list-icon);
    height: var(--m-list-icon);
  }

  @media (min-width: 768px) {
    &__left--drawer {
      display: none;
    }
  }

  &__title {
    flex: 1 1 auto;
    min-width: 0;
    font-size: 17px;
    font-weight: 600;
    line-height: 1.3;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    text-align: start;
  }

  &__subnavbar {
    position: relative;
    display: flex;
    align-items: center;
    height: var(--m-list-row-h);
    padding-left: calc(var(--m-spacing) + var(--m-safe-area-left, 0px));
    padding-right: calc(var(--m-spacing) + var(--m-safe-area-right, 0px));
    box-sizing: border-box;
  }
}

/* 深色主题：背景随 --m-surface 深色值自动切换（#202020），无需高光。
 * :not(.m-navbar--transparent) 必须保留：与 --transparent 变体同特异性 (0,2,0)，
 * 本规则靠后定义会覆盖 transparent 的透明背景（PlayerPage 沉浸式无回归）。 */
:global(.dark .m-navbar:not(.m-navbar--transparent)) {
  background: var(--m-surface);
}
</style>