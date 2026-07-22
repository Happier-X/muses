<template>
  <!-- HOST-IONIC — app shell；不进入 happier-ui 包心；路由/手势依赖 ion-page -->
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-buttons v-if="$slots.start" slot="start">
          <slot name="start" />
        </ion-buttons>
        <ion-title><slot name="title" /></ion-title>
        <ion-buttons v-if="$slots.end" slot="end">
          <slot name="end" />
        </ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content :fullscreen="fullscreen">
      <ion-header v-if="condensedTitle" collapse="condense">
        <ion-toolbar>
          <ion-title size="large"><slot name="title" /></ion-title>
        </ion-toolbar>
      </ion-header>
      <slot />
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButtons, IonContent, IonHeader, IonPage, IonTitle, IonToolbar } from '@ionic/vue'

withDefaults(defineProps<{
  fullscreen?: boolean
  condensedTitle?: boolean
}>(), {
  fullscreen: true,
  condensedTitle: true,
})
</script>
