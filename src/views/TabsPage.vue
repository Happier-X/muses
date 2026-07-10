<template>
  <div class="tabs-page-shell">
    <div class="layout-shell">
      <aside v-if="isTablet" class="tablet-sidebar" aria-label="主导航">
        <ion-list inset>
          <ion-item :router-link="'/tabs/songs'" :detail="false" lines="none" :class="{ 'is-active': route.path === '/tabs/songs' }">
            <ion-icon slot="start" aria-hidden="true" :icon="musicalNotes" />
            <ion-label>歌曲</ion-label>
          </ion-item>
          <ion-item :router-link="'/tabs/albums'" :detail="false" lines="none" :class="{ 'is-active': route.path === '/tabs/albums' }">
            <ion-icon slot="start" aria-hidden="true" :icon="albums" />
            <ion-label>专辑</ion-label>
          </ion-item>
          <ion-item :router-link="'/tabs/artists'" :detail="false" lines="none" :class="{ 'is-active': route.path === '/tabs/artists' }">
            <ion-icon slot="start" aria-hidden="true" :icon="person" />
            <ion-label>艺术家</ion-label>
          </ion-item>
          <ion-item :router-link="'/tabs/playlists'" :detail="false" lines="none" :class="{ 'is-active': route.path === '/tabs/playlists' }">
            <ion-icon slot="start" aria-hidden="true" :icon="list" />
            <ion-label>歌单</ion-label>
          </ion-item>
          <ion-item :router-link="'/tabs/sources'" :detail="false" lines="none" :class="{ 'is-active': route.path === '/tabs/sources' }">
            <ion-icon slot="start" aria-hidden="true" :icon="radio" />
            <ion-label>音源</ion-label>
          </ion-item>
          <ion-item :router-link="'/tabs/settings'" :detail="false" lines="none" :class="{ 'is-active': route.path === '/tabs/settings' }">
            <ion-icon slot="start" aria-hidden="true" :icon="settings" />
            <ion-label>设置</ion-label>
          </ion-item>
        </ion-list>
      </aside>

      <main class="content-shell">
        <RouterView />
      </main>
    </div>

    <ion-tab-bar v-if="!isTablet" class="mobile-tab-bar">
      <ion-tab-button tab="songs" href="/tabs/songs">
        <ion-icon aria-hidden="true" :icon="musicalNotes" />
        <ion-label>歌曲</ion-label>
      </ion-tab-button>
      <ion-tab-button tab="albums" href="/tabs/albums">
        <ion-icon aria-hidden="true" :icon="albums" />
        <ion-label>专辑</ion-label>
      </ion-tab-button>
      <ion-tab-button tab="artists" href="/tabs/artists">
        <ion-icon aria-hidden="true" :icon="person" />
        <ion-label>艺术家</ion-label>
      </ion-tab-button>
      <ion-tab-button tab="playlists" href="/tabs/playlists">
        <ion-icon aria-hidden="true" :icon="list" />
        <ion-label>歌单</ion-label>
      </ion-tab-button>
      <ion-tab-button tab="sources" href="/tabs/sources">
        <ion-icon aria-hidden="true" :icon="radio" />
        <ion-label>音源</ion-label>
      </ion-tab-button>
      <ion-tab-button tab="settings" href="/tabs/settings">
        <ion-icon aria-hidden="true" :icon="settings" />
        <ion-label>设置</ion-label>
      </ion-tab-button>
    </ion-tab-bar>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { IonIcon, IonItem, IonLabel, IonList, IonTabBar, IonTabButton } from '@ionic/vue'
import { useRoute, RouterView } from 'vue-router'
import { albums, list, musicalNotes, person, radio, settings } from 'ionicons/icons'

const route = useRoute()
const viewportWidth = ref(typeof window === 'undefined' ? 0 : window.innerWidth)
const isTablet = computed(() => viewportWidth.value >= 768)

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
  height: 100%;
}

.layout-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tablet-sidebar {
  display: none;
}

.content-shell {
  flex: 1;
  min-height: 0;
}

.mobile-tab-bar {
  display: flex;
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
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    left: 240px;
    min-width: 0;
    overflow: auto;
  }

  .mobile-tab-bar {
    display: none;
  }
}
</style>
