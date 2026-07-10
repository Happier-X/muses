<template>
  <div class="tabs-page-shell">
    <div class="layout-shell">
      <aside v-if="isTablet && isTabsRoute" class="tablet-sidebar" aria-label="主导航">
        <ion-list inset>
          <ion-item
            v-for="item in navItems"
            :key="item.to"
            :router-link="item.to"
            :detail="false"
            lines="none"
            :class="{ 'is-active': route.path === item.to }"
          >
            <ion-icon slot="start" aria-hidden="true" :icon="item.icon" />
            <ion-label>{{ item.label }}</ion-label>
          </ion-item>
        </ion-list>
      </aside>

      <main class="content-shell" :class="{ 'has-tabs-navigation': isTabsRoute }">
        <RouterView />
      </main>
    </div>

    <nav v-if="!isTablet && isTabsRoute" class="mobile-tab-bar" aria-label="底部导航">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        class="mobile-tab-link"
        :class="{ 'is-active': route.path === item.to }"
        :to="item.to"
      >
        <ion-icon aria-hidden="true" :icon="item.icon" />
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { IonIcon, IonItem, IonLabel, IonList } from '@ionic/vue'
import { useRoute, RouterLink, RouterView } from 'vue-router'
import { albums, list, musicalNotes, person, radio, settings } from 'ionicons/icons'

const navItems = [
  { to: '/tabs/songs', label: '歌曲', icon: musicalNotes },
  { to: '/tabs/albums', label: '专辑', icon: albums },
  { to: '/tabs/artists', label: '艺术家', icon: person },
  { to: '/tabs/playlists', label: '歌单', icon: list },
  { to: '/tabs/sources', label: '音源', icon: radio },
  { to: '/tabs/settings', label: '设置', icon: settings },
]

const route = useRoute()
const viewportWidth = ref(typeof window === 'undefined' ? 0 : window.innerWidth)
const isTablet = computed(() => viewportWidth.value >= 768)
const isTabsRoute = computed(() => route.path === '/tabs' || route.path.startsWith('/tabs/'))

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
  padding-bottom: calc(64px + var(--ion-safe-area-bottom, 0px));
}

.mobile-tab-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 30;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  padding: 6px 0 calc(6px + var(--ion-safe-area-bottom, 0px));
  border-top: 1px solid var(--ion-color-step-150, #e0e0e0);
  background: var(--ion-tab-bar-background, var(--ion-background-color, #fff));
}

.mobile-tab-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-width: 0;
  min-height: 52px;
  color: var(--ion-color-step-650, #595959);
  font-size: 12px;
  text-decoration: none;
}

.mobile-tab-link ion-icon {
  font-size: 22px;
}

.mobile-tab-link.is-active {
  color: var(--ion-color-primary);
  font-weight: 600;
}

.tablet-sidebar .is-active {
  --color: var(--ion-color-primary);
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
    width: 240px;
    overflow: auto;
    border-right: 1px solid var(--ion-color-step-150, #e0e0e0);
    background: var(--ion-background-color, #fff);
    padding-top: calc(12px + var(--ion-safe-area-top, 0px));
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
    left: 240px;
    overflow: auto;
  }

  .mobile-tab-bar {
    display: none;
  }
}
</style>
