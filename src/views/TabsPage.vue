<template>
  <div class="flex flex-col h-full">
    <div class="flex flex-1 flex-col min-h-0 md:block md:h-full">
      <aside
        v-if="isTablet && isTabsRoute"
        class="hidden md:block md:fixed md:top-0 md:left-0 md:bottom-0 md:z-20 md:w-[var(--muses-sidebar-width)] md:overflow-auto md:border-r md:border-r-[var(--muses-color-border-subtle)] md:bg-[var(--h-color-surface)] md:pt-[calc(12px+env(safe-area-inset-top,0px))] md:box-border"
        aria-label="主导航"
      >
        <nav aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="flex items-center gap-[12px] px-[16px] py-[12px] no-underline text-[length:var(--muses-font-body)]"
            :class="isNavActive(item.to)
              ? 'text-[color:var(--h-color-primary)] font-semibold'
              : 'text-[color:var(--h-color-ink)]'"
          >
            <h-icon aria-hidden="true" :icon="item.icon" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
      </aside>

      <main
        class="flex-1 min-h-0"
        :class="isTabsRoute
          ? 'pb-[calc(var(--muses-tab-bar-height)+env(safe-area-inset-bottom,0px))] md:pb-0 md:fixed md:top-0 md:right-0 md:bottom-0 md:left-[var(--muses-sidebar-width)] md:overflow-hidden'
          : 'pb-0 md:min-w-0'"
      >
        <RouterView />
      </main>
    </div>

    <h-tab-bar
      v-if="!isTablet && isTabsRoute"
      class="z-[var(--muses-z-tab)] md:hidden"
      :model-value="activeTab"
      :items="tabItems"
      aria-label="底部导航"
      fixed
      safe-area
      @update:model-value="navigateTab"
    />
  </div>
</template>

<script setup lang="ts">
import { HIcon, HTabBar, type HTabBarItem } from '@/components/ui'
import { computed, onMounted, onUnmounted, ref } from 'vue'

import { useRoute, useRouter, RouterView } from 'vue-router'
import { albums, list, musicalNotes, person, radio, settings } from '@/icons'

const navItems = [
  { to: '/tabs/songs', label: '歌曲', icon: musicalNotes },
  { to: '/tabs/albums', label: '专辑', icon: albums },
  { to: '/tabs/artists', label: '艺术家', icon: person },
  { to: '/tabs/playlists', label: '歌单', icon: list },
  { to: '/tabs/sources', label: '音源', icon: radio },
  { to: '/tabs/settings', label: '设置', icon: settings },
]

const route = useRoute()
const router = useRouter()
const tabItems: HTabBarItem[] = navItems.map((item) => ({ key: item.to, label: item.label, icon: item.icon }))
const activeTab = computed(() => navItems.find((item) => isNavActive(item.to))?.to ?? '/tabs/songs')
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
