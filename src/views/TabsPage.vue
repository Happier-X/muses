<template>
  <ion-page>
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
  </ion-page>
</template>

<script setup lang="ts">
import { IonTabBar, IonTabButton, IonTabs, IonLabel, IonIcon, IonPage, IonRouterOutlet } from '@ionic/vue';
import { albums, list, musicalNotes, person, radio, settings } from 'ionicons/icons';
</script>
