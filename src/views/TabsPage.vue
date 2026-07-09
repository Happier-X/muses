<template>
  <ion-page>
    <ion-split-pane :when="TABLET_BREAKPOINT" content-id="muses-main">
      <ion-menu side="start" content-id="muses-main">
        <ion-content>
          <ion-list>
            <ion-item :router-link="'/tabs/songs'" :detail="false">
              <ion-icon slot="start" aria-hidden="true" :icon="musicalNotes" />
              <ion-label>歌曲</ion-label>
            </ion-item>
            <ion-item :router-link="'/tabs/albums'" :detail="false">
              <ion-icon slot="start" aria-hidden="true" :icon="albums" />
              <ion-label>专辑</ion-label>
            </ion-item>
            <ion-item :router-link="'/tabs/artists'" :detail="false">
              <ion-icon slot="start" aria-hidden="true" :icon="person" />
              <ion-label>艺术家</ion-label>
            </ion-item>
            <ion-item :router-link="'/tabs/playlists'" :detail="false">
              <ion-icon slot="start" aria-hidden="true" :icon="list" />
              <ion-label>歌单</ion-label>
            </ion-item>
            <ion-item :router-link="'/tabs/sources'" :detail="false">
              <ion-icon slot="start" aria-hidden="true" :icon="radio" />
              <ion-label>音源</ion-label>
            </ion-item>
            <ion-item :router-link="'/tabs/settings'" :detail="false">
              <ion-icon slot="start" aria-hidden="true" :icon="settings" />
              <ion-label>设置</ion-label>
            </ion-item>
          </ion-list>
        </ion-content>
      </ion-menu>
      <div id="muses-main">
        <ion-tabs>
          <ion-router-outlet></ion-router-outlet>
          <ion-tab-bar slot="bottom">
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
        </ion-tabs>
      </div>
    </ion-split-pane>
  </ion-page>
</template>

<script setup lang="ts">
import { IonContent, IonIcon, IonItem, IonLabel, IonList, IonMenu, IonPage, IonRouterOutlet, IonSplitPane, IonTabBar, IonTabButton, IonTabs } from '@ionic/vue';
import { albums, list, musicalNotes, person, radio, settings } from 'ionicons/icons';

const TABLET_BREAKPOINT = '768px';
</script>

<style scoped>
@media (min-width: 768px) {
  ion-tab-bar {
    display: none;
  }
}

#muses-main {
  height: 100%;
  display: flex;
}
</style>