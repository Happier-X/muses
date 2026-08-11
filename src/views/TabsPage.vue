<template>
  <div class="flex flex-col h-full">
    <div class="flex flex-1 flex-col min-h-0 md:block md:h-full">
      <aside
        v-if="isTablet && isTabsRoute"
        class="hidden md:block md:fixed md:top-0 md:left-0 md:bottom-0 md:z-20 md:w-[260px] md:overflow-auto md:border-r md:border-r-black/10 md:bg-white dark:md:border-r-white/15 dark:md:bg-black md:pt-safe-3 md:box-border"
        aria-label="主导航"
      >
        <nav aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="flex items-center gap-[12px] px-[16px] py-[12px] no-underline text-[15px]"
            :class="isNavActive(item.to)
              ? 'text-primary font-semibold'
              : 'text-black dark:text-white'"
          >
            <component :is="item.icon" aria-hidden="true" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
      </aside>

      <main
        class="flex-1 min-h-0 relative"
        :class="isTabsRoute
          ? 'md:fixed md:top-0 md:right-0 md:bottom-0 md:left-[260px] md:overflow-hidden'
          : 'pb-0 md:min-w-0'"
      >
        <RouterView />
      </main>
    </div>

    <k-tabbar
      v-if="!isTablet && isTabsRoute"
      class="left-0 bottom-0 fixed z-[950] md:hidden"
      bg-class="[--tw-gradient-position:to_top]"
      :colors="{ bgIos: 'bg-gradient-to-t from-white to-transparent dark:from-black/60' }"
      labels
      icons
      aria-label="底部导航"
    >
      <k-toolbar-pane class="!bg-transparent">
        <k-tabbar-link
          v-for="item in navItems"
          :key="item.to"
          component="button"
          :label="item.label"
          :active="isNavActive(item.to)"
          @click="navigateTab(item.to)"
        >
          <template #icon>
            <component :is="item.icon" aria-hidden="true" />
          </template>
        </k-tabbar-link>
      </k-toolbar-pane>
    </k-tabbar>
  </div>
</template>

<script setup lang="ts">
import { kTabbar, kTabbarLink, kToolbarPane } from '@/components/ui'
import { computed, onMounted, onUnmounted, ref } from 'vue'

import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router'
import { home, list, musicalNotes, radio, settings } from '@/icons'

const navItems = [
  { to: '/tabs/songs', label: '歌曲', icon: musicalNotes },
  { to: '/tabs/categories', label: '音乐库', icon: list },
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
