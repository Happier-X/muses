<template>
  <div class="tabs-page-shell">
    <div class="layout-shell">
      <aside v-if="isTablet && isTabsRoute" class="tablet-sidebar" aria-label="主导航">
        <nav class="tablet-sidebar" aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="sidebar-link"
            :class="{ 'is-active': isNavActive(item.to) }"
          >
            <h-icon aria-hidden="true" :icon="item.icon" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
      </aside>

      <main class="content-shell" :class="{ 'has-tabs-navigation': isTabsRoute }">
        <RouterView />
      </main>
    </div>

    <h-tab-bar
      v-if="!isTablet && isTabsRoute"
      class="mobile-tab-bar"
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

<style scoped>
.tabs-page-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.layout-shell {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.tablet-sidebar {
  display: none;
}

.content-shell {
  flex: 1;
  min-height: 0;
  padding-bottom: 0;
}

.content-shell.has-tabs-navigation {
  padding-bottom: calc(var(--muses-tab-bar-height) + env(safe-area-inset-bottom, 0px));
}

.mobile-tab-bar {
  z-index: var(--muses-z-tab);
}

.sidebar-link { display: flex; align-items: center; gap: 12px; padding: 12px 16px; color: var(--h-color-ink); text-decoration: none; font-size: var(--muses-font-body); }
.sidebar-link.is-active {
  color: var(--h-color-primary);
  font-weight: 600;
}

@media (min-width: 768px) {
  .layout-shell {
    display: block;
    height: 100%;
  }

  .tablet-sidebar {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 20;
    width: var(--muses-sidebar-width);
    overflow: auto;
    border-right: 1px solid var(--muses-color-border-subtle);
    background: var(--h-color-surface);
    padding-top: calc(12px + env(safe-area-inset-top, 0px));
    box-sizing: border-box;
  }

  .content-shell {
    min-width: 0;
    padding-bottom: 0;
  }

  .content-shell.has-tabs-navigation {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    left: var(--muses-sidebar-width);
    overflow: auto;
  }

  .mobile-tab-bar {
    display: none;
  }
}
</style>
