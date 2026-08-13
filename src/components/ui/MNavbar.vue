<template>
  <div
    class="m-navbar"
    :class="{ 'm-navbar--transparent': transparent, 'm-navbar--has-subnavbar': $slots.subnavbar }"
  >
    <!-- 玻璃底（blur 2px + surface 渐变 + 下半 mask 渐隐，WebView 110 兼容写法） -->
    <div class="m-navbar__bg" aria-hidden="true" />
    <div class="m-navbar__inner">
      <div v-if="$slots.left" class="m-navbar__left">
        <slot name="left" />
      </div>
      <div class="m-navbar__title">
        <slot />
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
/**
 * APP-ONLY：MNavbar —— 吸顶导航栏（替代 k-navbar，iOS）
 * 安全区顶部 + 44px 内容行；玻璃底 = blur(2px) + surface→透明渐变 + mask 渐显
 * （渐变不用 oklab 插值、直接给 backdrop-filter 值，WebView<111/110 兼容）。
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
</script>

<style scoped lang="scss">
.m-navbar {
  position: sticky;
  top: 0;
  z-index: 20;
  width: 100%;
  box-sizing: border-box;
  color: var(--m-text);
  padding-top: max(16px, var(--m-safe-area-top, 0px));
  background-color: var(--m-surface-1);
  border-bottom: 1px solid var(--m-hairline);

  &__bg {
    position: absolute;
    inset: 0;
    pointer-events: none;
    background-color: var(--m-surface-1);
  }

  &--has-subnavbar &__bg {
    height: 100%;
  }

  &--transparent {
    background-color: transparent;
    border-bottom-color: transparent;
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
</style>