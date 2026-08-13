<template>
  <div class="tabs-layout">
    <div class="tabs-layout__body">
      <aside
        v-if="isTablet && isTabsRoute"
        class="tabs-layout__aside"
        aria-label="主导航"
      >
        <nav aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="tabs-layout__nav-link"
            :class="{ 'tabs-layout__nav-link--active': isNavActive(item.to) }"
          >
            <component :is="item.icon" aria-hidden="true" class="tabs-layout__nav-icon" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
      </aside>

      <main
        class="tabs-layout__main"
        :class="{ 'tabs-layout__main--tabbed': isTabsRoute }"
      >
        <RouterView />
      </main>
    </div>

    <m-tabbar
      v-if="!isTablet && isTabsRoute"
      class="tabs-layout__tabbar"
      labels
      icons
      aria-label="底部导航"
    >
      <m-tabbar-link
        v-for="item in navItems"
        :key="item.to"
        :label="item.label"
        :active="isNavActive(item.to)"
        @click="navigateTab(item.to)"
      >
        <template #icon>
          <component :is="item.icon" aria-hidden="true" />
        </template>
      </m-tabbar-link>
    </m-tabbar>
  </div>
</template>

<script setup lang="ts">
import { MTabbar, MTabbarLink } from '@/components/ui'
import { computed, onMounted, onUnmounted, ref } from 'vue'

import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router'
import { library, musicalNotes, radio, settings } from '@/icons'

const navItems = [
  { to: '/tabs/songs', label: '歌曲', icon: musicalNotes },
  { to: '/tabs/categories', label: '音乐库', icon: library },
  { to: '/tabs/sources', label: '音源', icon: radio },
  { to: '/tabs/settings', label: '设置', icon: settings },
]

const route = useRoute()
const router = useRouter()
const navigateTab = (to: string) => {
  if (to !== route.path) void router.push(to)
}
const viewportWidth = ref(typeof window === 'undefined' ? 0 : window.innerWidth)
const isTablet = computed(() => viewportWidth.value >= 768)
const isTabsRoute = computed(() => route.path === '/tabs' || route.path.startsWith('/tabs/'))

/** 详情子路由（如 /tabs/playlists/:id）仍高亮父 tab */
const isNavActive = (to: string) => {
  if (route.path === to) {
    return true
  }
  return to !== '/tabs' && route.path.startsWith(`${to}/`)
}

const updateViewportWidth = () => {
  viewportWidth.value = window.innerWidth
}

onMounted(() => {
  updateViewportWidth()
  window.addEventListener('resize', updateViewportWidth)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewportWidth)
})
</script>

<style scoped lang="scss">
.tabs-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;

  &__body {
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;

    @media (min-width: 768px) {
      display: block;
      height: 100%;
    }
  }

  &__aside {
    display: none;

    @media (min-width: 768px) {
      display: block;
      position: fixed;
      top: 0;
      left: 0;
      bottom: 0;
      z-index: 20;
      width: 260px;
      overflow-y: auto;
      border-right: 1px solid var(--m-hairline);
      background-color: var(--m-surface-1);
      padding-top: calc(var(--m-spacing-sub) + var(--m-safe-area-top, 0px));
      box-sizing: border-box;
    }
  }

  &__nav-link {
    display: flex;
    align-items: center;
    gap: var(--m-spacing-sub);
    min-height: var(--m-list-row-h);
    padding: 0 var(--m-spacing);
    text-decoration: none;
    font-size: 15px;
    color: var(--m-text);

    &--active {
      color: var(--m-primary);
      font-weight: 600;
    }
  }

  &__nav-icon {
    width: var(--m-list-icon);
    height: var(--m-list-icon);
    flex: 0 0 var(--m-list-icon);
  }

  &__main {
    position: relative;
    flex: 1;
    min-height: 0;

    &--tabbed {
      @media (min-width: 768px) {
        position: fixed;
        top: 0;
        right: 0;
        bottom: 0;
        left: 260px;
        overflow: hidden;
      }
    }
  }

  &__tabbar {
    display: none;

    @media (max-width: 767px) {
      display: block;
    }

    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 950;
  }
}

:global(.dark) .tabs-layout__aside {
  border-right-color: var(--m-hairline);
  background-color: var(--m-surface-1);
}
</style>